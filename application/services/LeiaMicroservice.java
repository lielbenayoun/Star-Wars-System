package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.AttackEvent;
import bgu.spl.mics.application.messages.DeactivationEvent;
import bgu.spl.mics.application.passiveObjects.Attack;
import bgu.spl.mics.application.passiveObjects.Diary;

/**
 * LeiaMicroservices Initialized with Attack objects, and sends them as  {@link AttackEvent}.
 * This class may not hold references for objects which it is not responsible for:
 * {@link AttackEvent}.
 * <p>
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class LeiaMicroservice extends MicroService {
    private Attack[] attacks;
    private List<AttackEvent> attackEventList;

    public LeiaMicroservice(Attack[] attacks) {
        super("Leia");
        this.attacks = attacks;
        attackEventList = new ArrayList<>();
        for (Attack attack : attacks) {
            AttackEvent e = new AttackEvent(attack);
            attackEventList.add(e);
        }
    }

    @Override
    protected void initialize() throws InterruptedException {
        Thread.sleep(1000); //sleep until the other micros subscribed to their messages
        List<Future<Boolean>> list = new ArrayList<>();
        for (AttackEvent e : this.attackEventList) { //sends all the attack events via the message bus
            Future<Boolean> future = super.sendEvent(e);
            list.add(future);
        }
        while (!list.isEmpty()) { //after all attack events are resolved she deletes them from the list and sends deactivation event
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) != null)
                    if (list.get(i).get() != null)
                        list.remove(list.get(i));
            }
        }
        super.sendEvent(new DeactivationEvent());
    }

    @Override
    protected void close() {
        Diary s = Diary.getInstance();
        s.setLeiaTerminate(System.currentTimeMillis());
    }
}
