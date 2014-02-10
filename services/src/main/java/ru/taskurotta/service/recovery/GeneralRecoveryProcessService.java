package ru.taskurotta.service.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.BrokenProcessService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 21.10.13
 * Time: 18:24
 */
public class GeneralRecoveryProcessService implements RecoveryProcessService {

    private static final Logger logger = LoggerFactory.getLogger(GeneralRecoveryProcessService.class);

    private QueueService queueService;
    private DependencyService dependencyService;
    private ProcessService processService;
    private TaskService taskService;
    private BrokenProcessService brokenProcessService;
    private GarbageCollectorService garbageCollectorService;
    // time out between recovery process in milliseconds
    private long recoveryProcessChangeTimeout;
    private long findIncompleteProcessPeriod;

    public GeneralRecoveryProcessService() {}

    public GeneralRecoveryProcessService(QueueService queueService, DependencyService dependencyService,
                                         ProcessService processService, TaskService taskService, BrokenProcessService brokenProcessService,
                                         GarbageCollectorService garbageCollectorService, long recoveryProcessChangeTimeout,
                                         long findIncompleteProcessPeriod) {
        this.queueService = queueService;
        this.dependencyService = dependencyService;
        this.processService = processService;
        this.taskService = taskService;
        this.brokenProcessService = brokenProcessService;
        this.garbageCollectorService = garbageCollectorService;
        this.recoveryProcessChangeTimeout = recoveryProcessChangeTimeout;
        this.findIncompleteProcessPeriod = findIncompleteProcessPeriod;
    }

    @Override
    public boolean restartProcess(final UUID processId) {
        logger.trace("Try to restart process [{}]", processId);

        boolean result = false;//val=true if some tasks have been placed to queue

        Graph graph = dependencyService.getGraph(processId);

        if (graph == null) {//have only process service info => restart whole process
            logger.warn("#[{}]: graph was not found (possible data loss?), try to restart process from start task", processId);
            result = restartProcessFromBeginning(processId);

        } else if (graph.isFinished()) {// process is already finished => just mark process as finished
            logger.debug("#[{}]: isn't finished, but graph is finished, force finish process", processId);
            TaskContainer startTaskContainer = processService.getStartTask(processId);
            finishProcess(processId, startTaskContainer.getTaskId(), graph.getProcessTasks());

        } else if (hasRecentActivity(graph)) {//was restarted or updated recently  => leave it alone for now
            logger.debug("#[{}]: graph was recently applied or recovered, skip it", processId);

        } else {// require restart => try to find process's tasks for restart
            Collection<TaskContainer> taskContainers = findIncompleteTaskContainers(graph);
            if (taskContainers == null) {//there is a problem in task store => restart process
                logger.warn("#[{}]: task containers were not found (possible data loss?), try to restart process from start task", processId);
                result = restartProcessFromBeginning(processId);

            } else {//restart unfinished tasks
                result = restartTasks(taskContainers, processId) > 0;
            }

        }

        if (result) {//has some tasks restarted -> mark graph as touched

            logger.trace("#[{}]: try to update graph", processId);
            dependencyService.changeGraph(new GraphDao.Updater() {
                @Override
                public UUID getProcessId() {
                    return processId;
                }

                @Override
                public boolean apply(Graph graph) {
                    graph.setTouchTimeMillis(System.currentTimeMillis());
                    if (logger.isTraceEnabled()) {
                        logger.trace("#[{}]: update touch time to [{} ({})]", processId, graph.getTouchTimeMillis(),
                                new Date(graph.getTouchTimeMillis()));
                    }

                    return true;
                }
            });

            brokenProcessService.delete(processId);
            logger.info("Process[{}] has been recovered (with broken process service clean up)", processId);
        }

        return result;
    }

    private boolean hasRecentActivity(Graph graph) {
        boolean result = false;
        if (graph != null) {
            long lastChange = Math.max(graph.getLastApplyTimeMillis(), graph.getTouchTimeMillis());
            if (lastChange > 0) {//has some modifications, check if they expired
                long changeTimeout = System.currentTimeMillis() - lastChange;
                logger.debug("Activity check for graph[{}]: change timeout[{}], last change[{}]", graph.getGraphId(), changeTimeout, lastChange);
                result = changeTimeout < recoveryProcessChangeTimeout;
            }
        }
        return result;
    }

    @Override
    public Collection<UUID> restartProcessCollection(Collection<UUID> processIds) {

        Set<UUID> successfullyRestartedProcesses = new TreeSet<>();

        for (UUID processId : processIds) {
            if (restartProcess(processId)) {
                successfullyRestartedProcesses.add(processId);
            }
        }

        brokenProcessService.deleteCollection(successfullyRestartedProcesses);

        return successfullyRestartedProcesses;
    }

    private int restartTasks(Collection<TaskContainer> taskContainers, UUID processId) {
        logger.trace("Try to restart [{}] task containers", taskContainers);

        int restartedTasks = 0;

        if (taskContainers != null && !taskContainers.isEmpty()) {

            long lastRecoveryStartTime = System.currentTimeMillis() - findIncompleteProcessPeriod;

            for (TaskContainer taskContainer : taskContainers) {
                UUID taskId = taskContainer.getTaskId();
                long startTime = taskContainer.getStartTime();
                String taskList = TransportUtils.getTaskList(taskContainer);
                String actorId = taskContainer.getActorId();

                if (isReadyToRecover(processId, taskId, startTime, actorId, taskList, lastRecoveryStartTime)
                        && queueService.enqueueItem(taskContainer.getActorId(), taskContainer.getTaskId(), processId, taskContainer.getStartTime(), taskList)) {
                    logger.trace("#[{}]: task [{}] have been restarted", processId, taskContainer);
                    restartedTasks++;
                }
            }

        }

        logger.debug("#[{}]: complete restart of [{}] tasks", processId, restartedTasks);

        return restartedTasks;
    }

    private boolean isReadyToRecover(UUID processId, UUID taskId, long startTime, String actorId, String taskList, long lastRecoveryStartTime) {

        boolean result = true;//consider every task as ready by default
        logger.trace("#[{}]/[{}]: try to restart", processId, taskId);

        if (startTime > System.currentTimeMillis()) {//task must be started in future => skip it //recovery iterations may take some time so check current date here

            if (logger.isDebugEnabled()) {
                logger.debug("#[{}]/[{}]: must be started later at [{}]", processId, taskId, new Date(startTime));
            }

            result = false;

        } else {//task is OK but it should be checked if queue is ready
            String queueName = queueService.createQueueName(actorId, taskList);
            long lastEnqueueTime = queueService.getLastPolledTaskEnqueueTime(queueName);

            if (lastEnqueueTime <= 0l) { //never polled => not ready

                logger.debug("Skip process[{}]/[{}] restart: queue[{}] is not polled by any actor", processId, taskId, queueName);

                result = false;

            } else if (lastEnqueueTime < lastRecoveryStartTime) {//still filled with old tasks => not ready

                if (logger.isDebugEnabled()) {
                    logger.debug("Skip process [{}]/[{}] restart: earlier tasks in queue [{}] (last enqueue time [{}], last recovery start time [{}])",
                            processId, taskId, queueName, lastEnqueueTime, lastRecoveryStartTime);
                }

                result = false;
            }
        }

        return result;
    }

    private Collection<TaskContainer> findIncompleteTaskContainers(Graph graph) {

        if (graph == null) {
            return null;
        }

        UUID processId = graph.getGraphId();

        logger.trace("#[{}]: try to find incomplete tasks", processId);

        Map<UUID, Long> notFinishedItems = graph.getNotFinishedItems();
        if (logger.isDebugEnabled()) {
            logger.debug("#[{}]: found [{}] not finished taskIds", processId, notFinishedItems.size());
        }

        Collection<TaskContainer> taskContainers = new ArrayList<>(notFinishedItems.size());
        Set<UUID> keys = notFinishedItems.keySet();
        for (UUID taskId : keys) {

            TaskContainer taskContainer = taskService.getTask(taskId, processId);

            if (taskContainer == null) {
                logger.warn("#[{}]: not found task container [{}] in task repository", processId, taskId);
                return null;
            }

            logger.trace("#[{}]: found not finished task container [{}]", processId, taskContainer);
            taskContainers.add(taskContainer);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("#[{}]: found [{}] not finished task containers", processId, taskContainers.size());
        }

        return taskContainers;
    }

    private boolean restartProcessFromBeginning(UUID processId) {

        if (processId == null) {
            return false;
        }

        TaskContainer startTaskContainer = processService.getStartTask(processId);
        logger.debug("#[{}]: start task is [{}]", processId, startTaskContainer);

        // emulate TaskServer.startProcess()
        taskService.startProcess(startTaskContainer);
        dependencyService.startProcess(startTaskContainer);
        logger.debug("Restart process[{}] from start: start task [{}]", processId, startTaskContainer);

        return restartTasks(Arrays.asList(startTaskContainer), processId) > 0;

    }

    private void finishProcess(UUID processId, UUID startTaskId, Collection<UUID> finishedTaskIds) {
        // save result to process storage
        DecisionContainer decisionContainer = taskService.getDecision(startTaskId, processId);
        ArgContainer argContainer = decisionContainer.getValue();
        String returnValue = argContainer.getJSONValue();
        processService.finishProcess(processId, returnValue);

        if (finishedTaskIds != null && !finishedTaskIds.isEmpty()) {
            taskService.finishProcess(processId, finishedTaskIds);
        }

        logger.debug("#[{}]: finish process. Save result [{}] from [{}] as process result", processId, returnValue, startTaskId);

        // send process to GC
        garbageCollectorService.delete(processId);

    }

    public void setDependencyService(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    public void setProcessService(ProcessService processService) {
        this.processService = processService;
    }

    public void setBrokenProcessService(BrokenProcessService brokenProcessService) {
        this.brokenProcessService = brokenProcessService;
    }
}
