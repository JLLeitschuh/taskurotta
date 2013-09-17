package ru.taskurotta.backend.console.retriever;

import ru.taskurotta.backend.statistics.DataPointVO;

import java.util.Collection;

/**
 * Interface for retrieving data on metrics
 * User: dimadin
 * Date: 12.09.13 14:17
 */
public interface MetricsDataRetriever {

    /**
     * Retrieves collection of available metric names
     */
    Collection<String> getMetricNames();

    /**
     * Retrieves collection of data sets measured by given metric
     */
    Collection<String> getDataSetsNames(String metricName);

    /**
     * Retrieve aggregated statistic for given metric and dataset
     */
    DataPointVO<Long>[] getCountsForLastHour(String metricName, String datasetName);

    /**
     * Retrieve aggregated statistic for given metric and dataset
     */
    DataPointVO<Long>[] getCountsForLastDay(String metricName, String datasetName);

    /**
     * Retrieve aggregated statistic for given metric and dataset
     */
    DataPointVO<Double>[] getMeansForLastHour(String metricName, String datasetName);

    /**
     * Retrieve aggregated statistic for given metric and dataset
     */
    DataPointVO<Double>[] getMeansForLastDay(String metricName, String datasetName);

}
