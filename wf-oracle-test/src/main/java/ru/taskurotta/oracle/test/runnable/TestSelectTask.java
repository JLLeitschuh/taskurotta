package ru.taskurotta.oracle.test.runnable;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.util.StopWatch;
import ru.taskurotta.oracle.test.DbDAO;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * User: greg
 */
public class TestSelectTask implements Runnable {

    public static final int OPERATION_COUNT = 200;
    private BasicDataSource dataSource;
    private int jobType;
    private CountDownLatch countDownLatch;

    public TestSelectTask(BasicDataSource dataSource, int jobType, CountDownLatch countDownLatch) {
        this.dataSource = dataSource;
        this.jobType = jobType;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        final UUID threadId = UUID.randomUUID();
        System.out.println(String.format("Thread started for %s", threadId));
        int count = 0;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        while (count < OPERATION_COUNT) {
            count++;
            final DbDAO dbDAO = new DbDAO(dataSource);
            try {
                dbDAO.selectLastTaskWithTypeImproved(jobType);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        stopWatch.stop();
        countDownLatch.countDown();
        double operPerSecond = OPERATION_COUNT / stopWatch.getTotalTimeSeconds();
        String message = String.format("Thread ID: %s finished for %s exprox: %s operations per second", threadId, stopWatch.getTotalTimeSeconds(), operPerSecond);
        System.out.println(message);

    }
}
