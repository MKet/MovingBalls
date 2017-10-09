package movingballsfx;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderWriterMonitor {

    private int readerCount = 0;
    private int writerCount = 0;
    private int writersWaiting = 0;
    private int readersWaiting = 0;
    private final Lock lock = new ReentrantLock();
    private final Condition okToRead = lock.newCondition();
    private final Condition okToWrite = lock.newCondition();

    public void readerEnter() throws InterruptedException {
        lock.lock();

        try {
            while (writerCount != 0) {
                try {
                    readersWaiting++;
                    okToRead.await();
                } finally {
                    readersWaiting--;
                }
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
            signal();
        } finally {
            lock.unlock();
        }
    }

    public void writeEnter() throws InterruptedException {
        lock.lock();

        try {
            while (writerCount+readerCount != 0) {
                try {
                    writersWaiting++;
                    okToWrite.await();
                } finally {
                    writersWaiting--;
                }
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
            signal();
        } finally {
            lock.unlock();
        }
    }

    private void signal() {
        if (writersWaiting != 0)
            okToWrite.signal();
        else
            okToRead.signal();
    }
}