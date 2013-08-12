package ru.taskurotta.restarter;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 08.08.13
 * Time: 12:34
 */
public class ProcessVO {
    private UUID id;
    private long startTime;
    private UUID startTaskId;
    private String startJson;

    public ProcessVO() {}

    public ProcessVO(UUID id, long startTime, UUID startTaskId, String startJson) {
        this.id = id;
        this.startTime = startTime;
        this.startTaskId = startTaskId;
        this.startJson = startJson;
    }

    public UUID getId() {
        return id;
    }

    public long getStartTime() {
        return startTime;
    }

    public UUID getStartTaskId() {
        return startTaskId;
    }

    public String getStartJson() {
        return startJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessVO processVO = (ProcessVO) o;

        if (startTime != processVO.startTime) return false;
        if (id != null ? !id.equals(processVO.id) : processVO.id != null) return false;
        if (startJson != null ? !startJson.equals(processVO.startJson) : processVO.startJson != null) return false;
        if (startTaskId != null ? !startTaskId.equals(processVO.startTaskId) : processVO.startTaskId != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (startTaskId != null ? startTaskId.hashCode() : 0);
        result = 31 * result + (startJson != null ? startJson.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProcessVO{" +
                "id=" + id +
                ", startTime=" + startTime +
                ", startTaskId=" + startTaskId +
                ", startJson='" + startJson + '\'' +
                "} " + super.toString();
    }
}
