package ru.taskurotta.backend.checkpoint;

import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing checkpoints referred by timeouts.
 * For example, process expiration timeout would refer to the checkpoint
 * of process start time (and thus checkpoint should be removed at process end)
 */
public interface CheckpointService {

    /**
     * Set new given checkpoint
     */
    public void addCheckpoint(Checkpoint checkpoint);

    /**
     * Sets new checkpoints for given timeout type
     */
    public void addCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints);

    /**
     * removes given checkpoint
     */
    public void removeCheckpoint(Checkpoint checkpoint);

    /**
     * Removes checkpoints for given timeout type
     */
    public void removeCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints);

    /**
     * returns list of current checkpoints fitting query criteria
     */
    public List<Checkpoint> listCheckpoints(CheckpointQuery command);

    /**
     * Removes all checkpoints of the timeout type for given task
     * @return number of removed checkpoints
     */
    public int removeTaskCheckpoints(UUID taskId, UUID processId, TimeoutType timeoutType);

}