package ru.taskurotta.service.hz.gc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.delay.CachedDelayQueue;
import ru.taskurotta.hazelcast.queue.delay.QueueFactory;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.AbstractGCTask;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskDao;
import ru.taskurotta.util.Shutdown;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HzGarbageCollectorService implements GarbageCollectorService {

    private static final Logger logger = LoggerFactory.getLogger(HzGarbageCollectorService.class);

    private boolean enabled;

    private CachedDelayQueue<UUID> garbageCollectorQueue;

    public static AtomicLong deletedProcessCounter = new AtomicLong();

    public HzGarbageCollectorService(final ProcessService processService, final GraphDao graphDao,
                                     final TaskDao taskDao, QueueFactory queueFactory, String garbageCollectorQueueName,
                                     int poolSize, boolean enabled) {

        logger.debug("Garbage Collector initialization. Enabled: {}", enabled);

        this.enabled = enabled;

        if (!enabled) {
            return;
        }

        this.garbageCollectorQueue = queueFactory.create(garbageCollectorQueueName);

        final ExecutorService executorService = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("GC-" + counter++);
                thread.setDaemon(true);
                return thread;
            }
        });

        for (int i = 0; i < poolSize; i++) {
            executorService.submit(new AbstractGCTask(processService, graphDao, taskDao) {
                @Override
                public void run() {
                    while (!Shutdown.isTrue()) {
                        try {
                            logger.trace("Try to get process for garbage collector");

                            UUID processId = garbageCollectorQueue.poll(50, TimeUnit.SECONDS);
                            if (processId != null) {
                                gc(processId);
                                deletedProcessCounter.incrementAndGet();
                            }
                        } catch (Exception e) {
                            logger.error(e.getLocalizedMessage(), e);
                        }

                    }
                }
            });
        }
    }

    @Override
    public void collect(UUID processId, long timeout) {
        if (!enabled) {
            return;
        }
        try {
            garbageCollectorQueue.delayOffer(processId, timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getCurrentSize() {
        if (!enabled) {
            return 0;
        }
        return garbageCollectorQueue.size();
    }
}
