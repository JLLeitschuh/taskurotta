package ru.taskurotta.service.recovery;

import java.util.Collection;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 21.10.13
 * Time: 18:24
 */
public interface RecoveryProcessService {
    /**
     * @return result of restart process
     */
    boolean resurrect(UUID processId);

    /**
     * @return UUID's collection of successfully restarted processes
     */
    Collection<UUID> restartProcessCollection(Collection<UUID> processIds);

}
