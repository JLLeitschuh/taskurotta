package ru.taskurotta.backend.console.model;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO representing queue statistics data
 * User: dimadin
 * Date: 29.11.13 12:41
 */
public class QueueStatVO implements Serializable {

    private String name;
    private int count;
    private Date lastActivity;

    private long inHour;
    private long outHour;

    private long inDay;
    private long outDay;

    private int nodes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
    }

    public long getInHour() {
        return inHour;
    }

    public void setInHour(long inHour) {
        this.inHour = inHour;
    }

    public long getOutHour() {
        return outHour;
    }

    public void setOutHour(long outHour) {
        this.outHour = outHour;
    }

    public long getInDay() {
        return inDay;
    }

    public void setInDay(long inDay) {
        this.inDay = inDay;
    }

    public long getOutDay() {
        return outDay;
    }

    public void setOutDay(long outDay) {
        this.outDay = outDay;
    }

    public int getNodes() {
        return nodes;
    }

    public void setNodes(int nodes) {
        this.nodes = nodes;
    }

    public QueueStatVO sumValuesWith(QueueStatVO qs) {
        if (qs != null) {
            if (qs.getCount() > 0) {
                this.count += qs.getCount();
            }
            if (this.lastActivity==null
                    || (qs.getLastActivity()!=null && qs.getLastActivity().after(this.lastActivity)) ) {
                this.lastActivity = qs.getLastActivity();
            }
            if (qs.getInDay() > 0) {
                this.inDay += qs.getInDay();
            }
            if (qs.getInHour() > 0) {
                this.inHour += qs.getInHour();
            }
            if (qs.getOutDay() > 0) {
                this.outDay += qs.getOutDay();
            }
            if (qs.getOutDay() > 0) {
                this.outHour += qs.getOutDay();
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return "QueueStatVO{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", lastActivity=" + lastActivity +
                ", inHour=" + inHour +
                ", outHour=" + outHour +
                ", inDay=" + inDay +
                ", outDay=" + outDay +
                ", nodes=" + nodes +
                "} ";
    }
}