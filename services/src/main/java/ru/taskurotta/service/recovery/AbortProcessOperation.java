package ru.taskurotta.service.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.executor.Operation;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 20.04.2015
 * Time: 18:13
 */
public class AbortProcessOperation implements Operation<RecoveryService> {

    private static final Logger logger = LoggerFactory.getLogger(AbortProcessOperation.class);

    private UUID processId;

    private RecoveryService recoveryService;

    public AbortProcessOperation(UUID processId) {
        this.processId = processId;
    }

    public UUID getProcessId() {
        return processId;
    }

    @Override
    public void init(RecoveryService nativePoint) {
        this.recoveryService = nativePoint;
    }

    @Override
    public void run() {
        try {
            recoveryService.abortProcess(processId);
        } catch (Throwable e) {
            logger.error("Error on aborting operation: " + processId, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbortProcessOperation that = (AbortProcessOperation) o;

        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        return !(recoveryService != null ? !recoveryService.equals(that.recoveryService) : that.recoveryService != null);
    }

    @Override
    public int hashCode() {
        int result = processId != null ? processId.hashCode() : 0;
        result = 31 * result + (recoveryService != null ? recoveryService.hashCode() : 0);
        return result;
    }
}
