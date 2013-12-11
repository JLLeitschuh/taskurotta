package ru.taskurotta.schedule.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.schedule.adapter.tasks.AddJobCallable;
import ru.taskurotta.schedule.adapter.tasks.RemoveJobRunnable;
import ru.taskurotta.schedule.adapter.tasks.UpdateErrorCountRunnable;
import ru.taskurotta.schedule.adapter.tasks.UpdateJobRunnable;
import ru.taskurotta.schedule.adapter.tasks.UpdateJobStatusRunnable;
import ru.taskurotta.schedule.storage.JobStore;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Proxies JobStore's method calls to be executed on every
 * hazelcast node (via distributed executor service)
 *
 * Date: 10.12.13 14:35
 */
public class HzJobStoreAdapter implements JobStore {
    private static final Logger logger = LoggerFactory.getLogger(HzJobStoreAdapter.class);
    private IExecutorService executorService;
    private static JobStore jobStore;

    public HzJobStoreAdapter(JobStore jobStore, HazelcastInstance hzInstance) {
        this.jobStore = jobStore;
        this.executorService = hzInstance.getExecutorService(getClass().getName());
    }

    public static JobStore getRealJobStore() {
        return jobStore;
    }

    @Override
    public long addJob(final JobVO job) {
        long result = -1l;
        Map<Member, Future<Long>> nodesResults = executorService.submitToAllMembers(new AddJobCallable(job));

        for (Future<Long> nodeResultFuture: nodesResults.values()) {
            Long nodeResult = null;
            try {
                nodeResult = nodeResultFuture.get();
                if (nodeResult != null) {
                    long newResult = nodeResult.longValue();
                    if (result < 0 || result == newResult) {//new result or the same as on prev node: case OK
                        result = newResult;

                    } else {//different results from nodes: nodes unsync, error state
                        throw new IllegalStateException("Cannot execute addJob["+job+"], nodes are not synchronized!");
                    }
                } else {
                    throw new IllegalStateException("Cannot execute addJob["+job+"], node result is null!");
                }
            } catch (Exception e) {
                logger.error("addJob[" + job + "] execution interrupted, possible nodes desynchronization", e);
                result =  -1l;
                break;
            }
        }

        return result;
    }

    @Override
    public void removeJob(long id) {
        executorService.executeOnAllMembers(new RemoveJobRunnable(id));
    }

    @Override
    public void updateJobStatus(long id, int status) {
        executorService.executeOnAllMembers(new UpdateJobStatusRunnable(id, status));
    }

    @Override
    public void updateJob(final JobVO jobVO) {
        executorService.executeOnAllMembers(new UpdateJobRunnable(jobVO));
    }

    @Override
    public void updateErrorCount(final long jobId, final int count, final String message) {
        executorService.executeOnAllMembers(new UpdateErrorCountRunnable(jobId, count, message));
    }

    @Override
    public Collection<Long> getJobIds() {
        return jobStore.getJobIds();
    }

    @Override
    public JobVO getJob(long id) {
        return jobStore.getJob(id);
    }

    @Override
    public int getJobStatus(long jobId) {
        return jobStore.getJobStatus(jobId);
    }

}