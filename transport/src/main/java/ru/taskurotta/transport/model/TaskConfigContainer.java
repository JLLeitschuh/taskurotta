package ru.taskurotta.transport.model;

import ru.taskurotta.policy.retry.RetryPolicyConfig;

import java.io.Serializable;

/**
 * User: stukushin
 * Date: 21.05.13
 * Time: 14:03
 */
public class TaskConfigContainer implements Serializable {

    private String customId;
    private long startTime;
    private String taskList;
    private RetryPolicyConfig retryPolicyConfig;

    public TaskConfigContainer() {
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }

    public RetryPolicyConfig getRetryPolicyConfig() {
        return retryPolicyConfig;
    }

    public void setRetryPolicyConfig(RetryPolicyConfig retryPolicyConfig) {
        this.retryPolicyConfig = retryPolicyConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskConfigContainer that = (TaskConfigContainer) o;

        if (startTime != that.startTime) return false;
        if (customId != null ? !customId.equals(that.customId) : that.customId != null) return false;
        if (retryPolicyConfig != null ? !retryPolicyConfig.equals(that.retryPolicyConfig) : that.retryPolicyConfig != null)
            return false;
        if (taskList != null ? !taskList.equals(that.taskList) : that.taskList != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = customId != null ? customId.hashCode() : 0;
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (taskList != null ? taskList.hashCode() : 0);
        result = 31 * result + (retryPolicyConfig != null ? retryPolicyConfig.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskConfigContainer{" +
                "customId='" + customId + '\'' +
                ", startTime=" + startTime +
                ", taskList='" + taskList + '\'' +
                ", retryPolicySettings=" + retryPolicyConfig +
                '}';
    }
}
