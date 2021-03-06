package ru.taskurotta.test.mongofail;

import com.mongodb.DB;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.bootstrap.Bootstrap;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.core.TaskConfig;
import ru.taskurotta.test.mongofail.decider.TimeLogDeciderClient;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 18.02.14 15:45
 */
public class WorkflowStarter {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStarter.class);

    private ClientServiceManager clientServiceManager;

    private DB mongoDB;

    private int count;

    private long failDelay;

    private long processStartDelay;

    private long checkDelay;

    private long actorDelay;

    private AtomicInteger started = new AtomicInteger(0);//TODO: should it really be atomic?

    private FinishedCountRetriever finishedCountRetriever;

    public void start() {
        final String customId = "mongo_fail_test_" + System.currentTimeMillis();//unique for each test invocation

        logger.info("Start process[{}]: count[{}], failDelay[{}], checkDelay[{}], actorDelay[{}]", customId, count, failDelay, checkDelay, actorDelay);

        Thread starter = new Thread(new Runnable() {
            @Override
            public void run() {
                final DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
                final TimeLogDeciderClient decider = deciderClientProvider.getDeciderClient(TimeLogDeciderClient.class);

                TaskConfig opts = new TaskConfig().setCustomId(customId);
                opts.setCustomId(customId);
                for (int i = 1; i <= count; i++) {
                    decider.execute(opts);
                    started.incrementAndGet();
                    if (i % 10 == 0) {
                        logger.info("Started [{}]/[{}] processes", i, count);
                    }

                    if (processStartDelay > 0) {
                        try {
                            Thread.sleep(processStartDelay);
                        } catch (InterruptedException e) {
                            logger.error("Starter thread interrupted!", e);
                        }
                    }
                }

                logger.info("Started [{}] processes", count);
            }
        });

        Thread dataKiller = new Thread(new Runnable() {
            @Override
            public void run() {
                if(failDelay > 0) {
                    try {
                        Thread.sleep(failDelay);
                    } catch (InterruptedException e) {
                        logger.error("DataKiller thread interrupted!", e);
                    }
                }

                mongoDB.dropDatabase();
                logger.info("Mongo DB drop triggered!");
            }
        });

        Thread checker = new Thread(new Runnable() {
            @Override
            public void run() {

                if (checkDelay > 0) {
                    try {
                        Thread.sleep(checkDelay);
                    } catch (InterruptedException e) {
                        logger.error("Checker thread interrupted!", e);
                    }
                }

                int actual = finishedCountRetriever.getFinishedCount();
                int strd = started.get();

                logger.info("Checking processes finished: should be [{}], started[{}], actual [{}]", count, strd, actual);

                if (actual != strd) {
                    logger.error("Data were not restored after fail: should be ["+count+"], started["+strd+"], actual ["+actual+"]", new IllegalStateException("Check failed"));
                } else {
                    logger.info("Data were successfully restored after fail: should be ["+count+"], started["+strd+"], actual ["+actual+"]");
                }

                System.exit(0);

            }
        });

        checker.setName("Checker");
        starter.setName("Starter");
        dataKiller.setName("DataKiller");

        starter.start();
        checker.start();
        dataKiller.start();

        if (actorDelay > 0) {
            try {
                Thread.sleep(actorDelay);
            } catch (InterruptedException e) {
                logger.error("Actor main thread interrupted!", e);
            }
        }
    }

    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
        new Bootstrap("ru/taskurotta/test/mongofail/conf.yml").start();
    }

    @Required
    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    @Required
    public void setMongoDB(DB mongoDB) {
        this.mongoDB = mongoDB;
    }

    @Required
    public void setCount(int preCount) {
        this.count = preCount;
    }

    @Required
    public void setFailDelay(long failDelay) {
        this.failDelay = failDelay;
    }

    @Required
    public void setProcessStartDelay(long processStartDelay) {
        this.processStartDelay = processStartDelay;
    }

    @Required
    public void setCheckDelay(long checkDelay) {
        this.checkDelay = checkDelay;
    }

    @Required
    public void setActorDelay(long actorDelay) {
        this.actorDelay = actorDelay;
    }

    @Required
    public void setFinishedCountRetriever(FinishedCountRetriever finishedCountRetriever) {
        this.finishedCountRetriever = finishedCountRetriever;
    }
}
