package ru.taskurotta.service.dependency.links;

import ru.taskurotta.service.common.ResultSetCursor;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/8/13
 * Time: 10:23 AM
 */
public interface GraphDao {

    /**
     * @param graphId
     * @return last version of graph links.
     */
    public Graph getGraph(UUID graphId);


    /**
     * Register new process graph.
     *
     * @param graphId
     * @param taskId
     */
    public void createGraph(UUID graphId, UUID taskId);

    public void deleteGraph(UUID graphId);
    
    /**
     * Run graph updater
     *
     * @param updater
     * @return true when update are completed successfully
     */
    boolean changeGraph(Updater updater);


    /**
     * This class delegates synchronization solution to the DAO level.
     */
    public interface Updater {

        public UUID getProcessId();

        /**
         * @param graph
         * @return true if DAO should update graph in storage
         */
        public boolean apply(Graph graph);

    }

    ResultSetCursor<UUID> findLostGraphs(long lastGraphChangeTime, int batchSize);
}
