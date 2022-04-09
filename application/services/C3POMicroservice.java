package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.AttackEvent;
import bgu.spl.mics.application.messages.TerminationBroadcast;
import bgu.spl.mics.application.passiveObjects.Diary;
import bgu.spl.mics.application.passiveObjects.Ewoks;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * C3POMicroservices is in charge of the handling {@link AttackEvent}.
 * This class may not hold references for objects which it is not responsible for:
 * {@link AttackEvent}.
 * <p>
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class C3POMicroservice extends MicroService {

    private AtomicInteger totalAttacks;

    public C3POMicroservice() {
        super("C3PO");
        this.totalAttacks = new AtomicInteger(0);
    }

    @Override
    protected void initialize() {
        subscribeEvent(AttackEvent.class, (c) -> {
            List<Integer> EwoksSerialNumbers = c.getMission().getSerialNumbers();
            boolean availability = Ewoks.getInstance().getEwoks(EwoksSerialNumbers); //check if the required ewoks are available
            if (availability) {
                Thread.sleep(c.getMission().getDuration()); //attack the target
                totalAttacks.incrementAndGet();
                Ewoks.getInstance().releaseEwoks(EwoksSerialNumbers); //release the ewoks he used
                complete(c, true);
            }
            Diary theDiary = Diary.getInstance(); //updates the diary after the current attack
            theDiary.setC3POFinish(System.currentTimeMillis());
            theDiary.setTotalAttacks(theDiary.getTotalAttacks() + 1);
        });

    }

    @Override
    protected void close() {
        Diary s = Diary.getInstance();
        s.setC3POTerminate(System.currentTimeMillis());
    }
}
