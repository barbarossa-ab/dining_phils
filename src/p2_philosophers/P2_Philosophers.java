package p2_philosophers;

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author barbarossa
 */
public class P2_Philosophers {

    public static final int NR_PHIL_AND_STICKS = 5;

    public static class TableMonitor {
        // Lock + conditions
        private final Lock lock = new ReentrantLock();
        private final Condition placeAvailable = lock.newCondition();
        private final ArrayList<Condition> sticksAvailable = 
                new ArrayList<>(NR_PHIL_AND_STICKS);

        /* Resources */
        private ArrayList<Boolean> sticks = new ArrayList<>(NR_PHIL_AND_STICKS);
        private int eatingNow = 0;

        public TableMonitor() {
            for (int i = 0; i < NR_PHIL_AND_STICKS; i++) {
                sticksAvailable.add(lock.newCondition());
                sticks.add(Boolean.TRUE);
            }
        }

        public void takeSeat() {
            lock.lock();
            try {
                // To avoid deadlock, don't allow max nr of philosophers 
                // eating simultaneously 
                while (eatingNow >= (NR_PHIL_AND_STICKS - 1)) {
                    placeAvailable.await();
                }
                eatingNow++;
            } catch (InterruptedException ex) {
                System.out.println(ex.getStackTrace());
            } finally {
                lock.unlock();
            }
        }

        public void releaseSeat() {
            lock.lock();
            try {
                eatingNow--;
                if (eatingNow < 4) {
                    placeAvailable.signal();
                }
            } finally {
                lock.unlock();
            }
        }

        public void takeStick(int i) {
            lock.lock();
            try {
                // If stick is not available, wait for it
                // then mark it as used
                while (!sticks.get(i)) {
                    sticksAvailable.get(i).await();
                }
                sticks.set(i, false);
            } catch (InterruptedException ex) {
                System.out.println(ex.getStackTrace());
            } finally {
                lock.unlock();
            }
        }

        public void releaseStick(int i) {
            lock.lock();
            try {
                sticks.set(i, true);
                sticksAvailable.get(i).signal();
            } finally {
                lock.unlock();
            }
        }
    }

    public static class Philosopher extends Thread {

        public static final int NR_SERVES = 5;
        private int servesLeft = NR_SERVES;
        private int nr;
        private TableMonitor table;

        public Philosopher(TableMonitor table, int nr) {
            this.nr = nr;
            this.table = table;
        }

        @Override
        public void run() {
            while (servesLeft > 0) {
                // Wait until there are at least two sticks at the table
                table.takeSeat();

                // Take left stick
                table.takeStick(nr);
                System.out.printf("Philosopher %d picks up left chopstick.\n", nr + 1);
                // Take right stick
                table.takeStick((nr + 1) % NR_PHIL_AND_STICKS);
                System.out.printf("Philosopher %d picks up right chopstick.\n", nr + 1);

                // Eat
                System.out.printf("Philosopher %d eats.\n", nr + 1);

                //Release sticks
                table.releaseStick(nr);
                System.out.printf("Philosopher %d puts down left chopstick.\n", nr + 1);

                table.releaseStick((nr + 1) % NR_PHIL_AND_STICKS);
                System.out.printf("Philosopher %d puts down right chopstick.\n", nr + 1);

                table.releaseSeat();
                servesLeft--;
            }
        }
    }
   
    public static void main(String[] args) throws InterruptedException {

        TableMonitor t = new TableMonitor();
        ArrayList<Philosopher> phils = new ArrayList<>(NR_PHIL_AND_STICKS);
        for (int i = 0; i < NR_PHIL_AND_STICKS; i++) {
            phils.add(new Philosopher(t, i));
        }

        System.out.printf("Dinner is starting!\n\n");
        for (Philosopher p : phils) {
            p.start();
        }
        for (Philosopher p : phils) {
            p.join();
        }
        System.out.printf("Dinner is over!\n");
    }
}
