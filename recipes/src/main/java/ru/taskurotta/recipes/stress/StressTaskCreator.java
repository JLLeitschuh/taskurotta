package ru.taskurotta.recipes.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.multiplier.MultiplierDeciderClient;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.taskurotta.recipes.stress.LifetimeProfiler.*;

/**
 * User: greg
 */
public class StressTaskCreator implements Runnable, ApplicationListener<ContextRefreshedEvent> {

    private final static Logger log = LoggerFactory.getLogger(StressTaskCreator.class);

    private ClientServiceManager clientServiceManager;


    private static int THREADS_COUNT = 50;
    private static int initialCount = 4;
    private boolean needRun = true;
    public static CountDownLatch LATCH;
    private ExecutorService executorService;
    private static int shotSize = 4000;


    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("onApplicationEvent");
        if (needRun) {
            Executors.newSingleThreadExecutor().submit(this);
        }
    }

    public void setNeedRun(boolean needRun) {
        this.needRun = needRun;
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void createStartTask(final MultiplierDeciderClient deciderClient) {

        log.info("Sending new " + shotSize + " tasks...");

        for (int i = 0; i < shotSize; i++) {
            final int a = (int) (Math.random() * 100);
            final int b = (int) (Math.random() * 100);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    deciderClient.multiply(a, b);
                }
            });
        }

    }

    public static int getInitialCount() {
        return initialCount;
    }

    public void setInitialCount(int initialCount) {
        StressTaskCreator.initialCount = initialCount;
    }

    public static int getShotSize() {
        return shotSize;
    }

    public void setShotSize(int shotSize) {
        StressTaskCreator.shotSize = shotSize;
    }

    @Override
    public void run() {

        DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
        MultiplierDeciderClient deciderClient = clientProvider.getDeciderClient(MultiplierDeciderClient.class);
        executorService = Executors.newFixedThreadPool(THREADS_COUNT);
        for (int i = 0; i < initialCount; i++) {
            createStartTask(deciderClient);
        }
        while (stabilizationCounter.get() < 10) {
            LATCH = new CountDownLatch(1);
            createStartTask(deciderClient);
            try {
                LATCH.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long deltaTime = lastTime.get() - startTime.get();
        double time = 1.0 * deltaTime / 1000.0;
        long meanTaskCount = taskCount.get();
        double rate = 1000.0 * meanTaskCount / deltaTime;
        double totalDelta = LifetimeProfiler.totalDelta / (meanTaskCount / tasksForStat);
        log.info("Total task count: " + taskCount);
        log.info("Delta time: " + deltaTime);
        log.info(String.format("TOTAL: tasks: %6d; time: %6.3f s; rate: %8.3f tps; totalDelta: %8.3f \n", meanTaskCount, time, rate, totalDelta));
        stopDecorating.set(true);
        log.info("Decoration stopped");
        log.info("End");
        System.exit(0);
    }
}
