package movingballsfx;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BallMonitor {

    private int readerCount = 0;
    private int writerCount = 0;
    private int readersWaiting = 0;
    private final Lock lock = new ReentrantLock();
    private final Condition okToRead = lock.newCondition();
    private final Condition okToWrite = lock.newCondition();

    public void readerEnter() throws InterruptedException {
        lock.lock();

        try {
            while (writerCount != 0) {
                readersWaiting++;
                okToRead.await();
                readersWaiting--;
            }
            readerCount++;
            okToRead.signal();
        } finally {
            lock.unlock();
        }
    }

    public void readerLeave() throws InterruptedException {
        lock.lock();

        try {
            readerCount--;
            if (readerCount == 0)
                okToWrite.signal();
        } finally {
            lock.unlock();
        }
    }

    public void writeEnter() throws InterruptedException {
        lock.lock();

        try {
            while (writerCount != 0 || readerCount != 0) {
                okToWrite.await();
            }
            writerCount++;
            okToWrite.signal();
        } finally {
            lock.unlock();
        }
    }

    public void writerLeave() throws InterruptedException {
        lock.lock();

        try {
            writerCount--;
            if(readersWaiting > 0)
                okToRead.signal();
            else
                okToWrite.signal();
        } finally {
            lock.unlock();
        }
    }

}
