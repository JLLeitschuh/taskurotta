package ru.taskurotta.recipes.calculate.worker.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.recipes.calculate.RandomException;
import ru.taskurotta.recipes.calculate.worker.Summarizer;

public class SummarizerImpl implements Summarizer {

    private static final Logger logger = LoggerFactory.getLogger(SummarizerImpl.class);
    private long sleep = -1l;
    private double errPossibility = 0.0d;
    private boolean varyExceptions = false;

    @Override
    public Integer summarize(Integer a, Integer b) throws Exception {
        logger.trace("summarize() called");
        if (RandomException.isEventHappened(errPossibility)) {
            logger.error("Summarizer: RANDOMLY FAILED!");
            if (varyExceptions) {
                throw RandomException.getRandomException();
            } else {
                throw new RandomException("Its multiply exception time");
            }
        }

        if (sleep > 0) {

            logger.info("Sleep for [{}] ms", sleep);

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                logger.error("Sleep interrupted", e);
            }
        }

        Integer result = a+b;

        logger.debug("Summ result is[{}]", result);
        return result;
    }

    public void init() {
        logger.info("SummarizerImpl initialized with errPossibility[{}], sleep[{}] ", errPossibility, sleep);
    }

    public void setSleep(long sleep) {
        this.sleep = sleep;
    }

    public void setErrPossibility(double errPossibility) {
        this.errPossibility = errPossibility;
    }

    public void setVaryExceptions(boolean varyExceptions) {
        this.varyExceptions = varyExceptions;
    }
}
