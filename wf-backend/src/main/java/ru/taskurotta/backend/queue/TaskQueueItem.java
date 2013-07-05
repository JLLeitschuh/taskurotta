package ru.taskurotta.backend.queue;

import java.util.UUID;

/**
 * Pojo representing task queue item
 * User: dimadin
 * Date: 05.07.13 10:44
 */
public class TaskQueueItem {
    protected UUID taskId;
    protected UUID processId;
    protected long startTime;
    protected long enqueueTime;
    protected String taskList;

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEnqueueTime() {
        return enqueueTime;
    }

    public void setEnqueueTime(long enqueueTime) {
        this.enqueueTime = enqueueTime;
    }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }

    @Override
    public String toString() {
        return "TaskQueueItem{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", startTime=" + startTime +
                ", enqueueTime=" + enqueueTime +
                ", taskList='" + taskList + '\'' +
                "} " + super.toString();
    }
}
