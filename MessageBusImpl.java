package bgu.spl.mics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
public class MessageBusImpl implements MessageBus {

    private ConcurrentHashMap<MicroService, LinkedBlockingQueue<Message>> microServiceQueueMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Class<? extends Message>, ConcurrentLinkedQueue<MicroService>> MessageQueueMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Event, Future> futureMap = new ConcurrentHashMap<>();

    private MessageBusImpl() {

    }

    public static MessageBus getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static final MessageBus instance = new MessageBusImpl();
    }

    @Override
    public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
        subscribeMessage(type, m);
    }

    @Override
    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
        subscribeMessage(type, m);
    }

    private void subscribeMessage(Class<? extends Message> type, MicroService m) {
        MessageQueueMap.putIfAbsent(type, new ConcurrentLinkedQueue<>());
        MessageQueueMap.get(type).add(m);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void complete(Event<T> e, T result) {
        futureMap.get(e).resolve(result);
    }

    @Override
    public void sendBroadcast(Broadcast b) {
        ConcurrentLinkedQueue<MicroService> microServices;
        synchronized (b.getClass()) {
            microServices = MessageQueueMap.get(b.getClass());
            System.out.println(MessageQueueMap.keySet());
            if (microServices == null)
                return;
        }
        //send the broadcast to his subs
        for (MicroService i : MessageQueueMap.get(b.getClass())) {
            try {
                microServiceQueueMap.get(i).put(b);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }

        }
    }
    @Override
    public <T> Future<T> sendEvent(Event<T> e) {
        Future<T> future = new Future<>();
        ConcurrentLinkedQueue<MicroService> microServices;
        MicroService micro;
        futureMap.put(e, future);
        microServices = MessageQueueMap.get(e.getClass());
        if (microServices == null)
            return null;
        synchronized (e.getClass()) {
            if (microServices.isEmpty())
                return null;
            micro = microServices.poll(); //get the micro to handle the event
            microServices.add(micro);
        }
        synchronized (micro) {
            if (microServiceQueueMap.get(micro) == null)
                return null;
            microServiceQueueMap.get(micro).add(e); //add the events to the micro queue
        }
        return future;
    }

    @Override
    public void register(MicroService m) {
        microServiceQueueMap.put(m, new LinkedBlockingQueue<>());
    }

    @Override
    public void unregister(MicroService m) {
        MessageQueueMap.forEach((c, q) -> { //remove the current micro service from the messageQueue
            synchronized (c) {
                q.remove(m);
            }
        });
        LinkedBlockingQueue<Message> messages;
        synchronized (m) {
            messages = microServiceQueueMap.get(m);
        }
        for (Message message : messages) { //resolves all the remaining messages in the micro's queue
            Future<?> future = futureMap.get(message);
            if (future != null) {
                future.resolve(null);
            }
        }
        microServiceQueueMap.remove(m);
    }

    @Override
    public Message awaitMessage(MicroService m) throws InterruptedException {
        return microServiceQueueMap.get(m).take();
    }
}
