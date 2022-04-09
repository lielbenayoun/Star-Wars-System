package bgu.spl.mics.application.passiveObjects;


import java.util.*;

/**
 * Passive object representing the resource manager.
 * <p>
 * This class must be implemented as a thread-safe singleton.
 * You must not alter any of the given public methods of this class.
 * <p>
 * You can add ONLY private methods and fields to this class.
 */
public class Ewoks {
    private  Map<Integer,Ewok> ewoks = new HashMap<>();

    //----------------------------------------------------------------------------------------------------------------------
    private Ewoks() {
    } // default constructor


    public static Ewoks getInstance() { //todo : done
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static final Ewoks instance = new Ewoks();
    }

    public void load(Ewok[] ewoks) { // initializes the ewoks
        synchronized (this.ewoks) {
            for (Ewok ewok : ewoks) {
                this.ewoks.put(ewok.getSerialNumber(), ewok);
            }
        }
    }

    public void releaseEwoks(List<Integer> serials) { // releases ewoks from a given list
        if (!hasAllSers(serials))
            return;
        for (int serial : serials) {
            Ewok ewok ;
            synchronized (ewoks) {
                ewok = ewoks.get(serial);
            }
            synchronized (ewok) {
                ewok.release();
                ewok.notifyAll();
            }
        }
    }

    public boolean getEwoks(List<Integer> serials) { //acquires all needed ewoks for the attack
        try {
            if (!hasAllSers(serials))
                return false;
            serials.sort(Comparator.naturalOrder());

            for (int ser : serials) {
                Ewok _ewok ;
                synchronized (ewoks) {
                    _ewok = ewoks.get(ser);
                }
                synchronized (_ewok) {
                    while (!_ewok.isAvailable()) {
                        _ewok.wait();
                    }
                    _ewok.acquire();
                }
            }
        } catch (InterruptedException ie) {
            return false;
        }
        return true;
    }

    private boolean hasAllSers(List<Integer> serials) { //checks if all the needed ewoks are available
        synchronized (ewoks) {
            return ewoks.keySet().containsAll(serials);
        }
    }
}
