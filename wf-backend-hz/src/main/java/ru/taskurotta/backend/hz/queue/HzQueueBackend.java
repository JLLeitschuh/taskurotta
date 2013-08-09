package ru.taskurotta.backend.hz.queue;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.InstanceEvent;
import com.hazelcast.core.InstanceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.hz.Constants;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by void, dudin 07.06.13 11:00
 */
public class HzQueueBackend implements QueueBackend, QueueInfoRetriever, InstanceListener {

    private final static Logger logger = LoggerFactory.getLogger(HzQueueBackend.class);

    private int pollDelay = 60;
    private TimeUnit pollDelayUnit = TimeUnit.SECONDS;
    private String queueNamePrefix;

    //Hazelcast specific
    private HazelcastInstance hazelcastInstance;
    private String queueListName = Constants.DEFAULT_QUEUE_LIST_NAME;

    public HzQueueBackend(int pollDelay, TimeUnit pollDelayUnit, HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        this.pollDelay = pollDelay;
        this.pollDelayUnit = pollDelayUnit;

        this.hazelcastInstance.addInstanceListener(this);
        logger.debug("HzQueueBackend created and registered as Hazelcast instance listener");
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        IMap<String, Boolean> queueNamesMap = hazelcastInstance.getMap(queueListName);
        logger.debug("Stored queue names for queue backend are [{}]", new ArrayList(queueNamesMap.keySet()));
        List<String> result = new ArrayList<>(pageSize);
        String[] queueNames = queueNamesMap.keySet().toArray(new String[queueNamesMap.size()]);
        if (queueNames.length > 0) {
            int pageStart = (pageNum - 1) * pageSize;
            int pageEnd = pageSize * pageNum >= queueNames.length ? queueNames.length : pageSize * pageNum;
            result.addAll(Arrays.asList(queueNames).subList(pageStart, pageEnd));
        }
        return new GenericPage<>(prefixStrip(result), pageNum, pageSize, queueNames.length);
    }

    private List<String> prefixStrip(List<String> target) {
        if(queueNamePrefix==null) {
            return target;
        }
        List<String> result = null;
        if(target!=null && !target.isEmpty()) {
            result = new ArrayList<>();
            for(String item: target) {
                result.add(item.substring(queueNamePrefix.length()));
            }
        }
        return result;
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        if(queueNamePrefix!=null && !queueName.startsWith(queueNamePrefix)) {
            queueName = queueNamePrefix + queueName;
        }
        return hazelcastInstance.getQueue(queueName).size();
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        if(queueNamePrefix!=null && !queueName.startsWith(queueNamePrefix)) {
            queueName = queueNamePrefix + queueName;
        }
        List<TaskQueueItem> result = new ArrayList<>();
        IQueue<TaskQueueItem> queue = hazelcastInstance.getQueue(queueName);
        TaskQueueItem[] queueItems = queue.toArray(new TaskQueueItem[queue.size()]);

        if (queueItems.length > 0) {
            for (int i = (pageNum - 1) * pageSize; i < ((pageSize * pageNum >= queueItems.length) ? queueItems.length : pageSize * pageNum); i++) {
                result.add(queueItems[i]);
            }
        }
        return new GenericPage<>(result, pageNum, pageSize, queueItems.length);
    }

    @Override
    public void instanceCreated(InstanceEvent event) {
        if (event.getInstanceType().isQueue()) {//storing all new queues name
            IMap<String, Boolean> queueNamesMap = hazelcastInstance.getMap(queueListName);
            String queueName = ((IQueue) event.getInstance()).getName();
            queueNamesMap.put(queueName, Boolean.TRUE);
            logger.debug("Queue [{}] added to cluster", queueName);
        }
    }

    @Override
    public void instanceDestroyed(InstanceEvent event) {
        if (event.getInstanceType().isQueue()) {//removing queues names
            IMap<String, Boolean> queueNamesMap = hazelcastInstance.getMap(queueListName);
            String queueName = ((IQueue) event.getInstance()).getName();
            queueNamesMap.remove(queueName);
            logger.debug("Queue [{}] removed from cluster", queueName);
        }
    }

    @Override
    public TaskQueueItem poll(String actorId, String taskList) {
        IQueue<TaskQueueItem> queue = hazelcastInstance.getQueue(createQueueName(actorId, taskList));

        TaskQueueItem result = null;
        try {
            result = queue.poll(pollDelay, pollDelayUnit);

        } catch (InterruptedException e) {
            logger.error("Thread was interrupted at poll, releasing it", e);
        }

        logger.debug("poll() returns taskId [{}]. Queue.size: {}", result, queue.size());

        return result;

    }

    @Override
    public void pollCommit(String actorId, UUID taskId, UUID processId) {
    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }

        IQueue<TaskQueueItem> queue = hazelcastInstance.getQueue(createQueueName(actorId, taskList));
        TaskQueueItem item = new TaskQueueItem();
        item.setTaskId(taskId);
        item.setProcessId(processId);
        item.setStartTime(startTime);
        item.setEnqueueTime(System.currentTimeMillis());
        item.setTaskList(taskList);
        item.setCreatedDate(new Date());
        queue.add(item);

        logger.debug("enqueueItem() actorId [{}], taskId [{}], startTime [{}]; Queue.size: {}", actorId, taskId, startTime, queue.size());
    }

    @Override
    public CheckpointService getCheckpointService() {
        return null;
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return null;
    }

    private String createQueueName(String actorId, String taskList) {
        if(queueNamePrefix!=null) {
            actorId = queueNamePrefix+actorId;
        }
        return (taskList == null) ? actorId : actorId + "#" + taskList;
    }

    public void setQueueListName(String queueListName) {
        this.queueListName = queueListName;
    }

    public void setQueueNamePrefix(String queueNamePrefix) {
        this.queueNamePrefix = queueNamePrefix;
    }
}
