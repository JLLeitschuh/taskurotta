package ru.taskurotta.recipes.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.multiplier.MultiplierDeciderClient;

import java.io.Console;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: greg
 */
public class StressTaskCreator implements Runnable, ApplicationListener<ContextRefreshedEvent> {

    private final static Logger log = LoggerFactory.getLogger(StressTaskCreator.class);

    private ClientServiceManager clientServiceManager;


    private static int THREADS_COUNT = 100;

    private int countOfCycles = 125;

    public static final Lock MONITOR = new ReentrantLock(true);
    public static CountDownLatch LATCH;
    public static final AtomicBoolean CAN_WORK = new AtomicBoolean(false);

    private ExecutorService executorService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("onApplicationEvent");
        Executors.newSingleThreadExecutor().submit(this);
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void createStartTask(final MultiplierDeciderClient deciderClient) {
        MONITOR.lock();
        try {
            CAN_WORK.set(false);
            final CountDownLatch latch = new CountDownLatch(1);

            for (int i = 0; i < 2000; i++) {
                final int a = (int) (Math.random() * 100);
                final int b = (int) (Math.random() * 100);
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        deciderClient.multiply(a, b);
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                log.error("latch.await() about process creations was interrupted", e);
            }
        } finally {
            CAN_WORK.set(true);
            MONITOR.unlock();
        }
    }

    public int getCountOfCycles() {
        return countOfCycles;
    }

    public void setCountOfCycles(int countOfCycles) {
        this.countOfCycles = countOfCycles;
    }

    @Override
    public void run() {
        Console console = System.console();

        if (console != null) {
            DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
            MultiplierDeciderClient deciderClient = clientProvider.getDeciderClient(MultiplierDeciderClient.class);
            System.out.println(countOfCycles + " cycle test started");
            CountDownLatch countDownLatch = new CountDownLatch(countOfCycles);
            executorService = Executors.newFixedThreadPool(THREADS_COUNT);
            while (countDownLatch.getCount() > 0) {
                LATCH = new CountDownLatch(1);
                createStartTask(deciderClient);
                System.out.println("Latch locked!");
                try {
                    LATCH.await();
                    System.out.println("Cycle " + countDownLatch.getCount()+" of "+countOfCycles +" finished");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No console available!!");
        }
    }
}
