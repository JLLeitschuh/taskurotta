package ru.taskurotta.service.console.model;

import java.util.List;

/**
 * User: moroz
 * Date: 23.05.13
 */
public class GenericPage<T> {

    private int pageNumber;
    private long totalCount;
    private int pageSize;
    private List<T> items;
    private long currentTimeMillis = System.currentTimeMillis();

    public GenericPage(List<T> items, int pageNumber, int pageSize, long totalCount) {
        this.items = items;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getCurrentTimeMillis() {
        return currentTimeMillis;
    }

    @Override
    public String toString() {
        return "GenericPage{" +
                "pageNumber=" + pageNumber +
                ", totalCount=" + totalCount +
                ", pageSize=" + pageSize +
                ", items=" + items +
                ", currentTimeMillis=" + currentTimeMillis +
                '}';
    }
}
