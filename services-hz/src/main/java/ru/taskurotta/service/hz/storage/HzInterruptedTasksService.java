package ru.taskurotta.service.hz.storage;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.storage.InterruptedTasksService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created on 19.03.2015.
 */
public class HzInterruptedTasksService implements InterruptedTasksService {

    private static final Logger logger = LoggerFactory.getLogger(HzInterruptedTasksService.class);

    private static final String WILDCARD_SYMBOL = "%";

    private IMap<UUID, InterruptedTask> storeIMap;

    public HzInterruptedTasksService(HazelcastInstance hazelcastInstance, String storeMapName) {
        this.storeIMap = hazelcastInstance.getMap(storeMapName);
    }

    @Override
    public void save(InterruptedTask task) {
        storeIMap.put(task.getTaskId(), task);
    }

    @Override
    public Collection<InterruptedTask> find(SearchCommand searchCommand) {

        logger.debug("Try to find interrupted tasks by searchCommand [{}]", searchCommand);

        List<Predicate> predicates = new ArrayList<>();

        if (searchCommand.getTaskId() != null) {
            Collection<InterruptedTask> result = null;
            InterruptedTask bp = storeIMap.get(searchCommand.getTaskId());
            if (bp != null) {
                result = new ArrayList<>();
                result.add(bp);
            }
            return result;
        }


        if (StringUtils.hasText(searchCommand.getActorId())) {
            predicates.add(new Predicates.LikePredicate("actorId", searchCommand.getActorId() + WILDCARD_SYMBOL));
        }

        if (StringUtils.hasText(searchCommand.getStarterId())) {
            predicates.add(new Predicates.LikePredicate("starterId", searchCommand.getStarterId() + WILDCARD_SYMBOL));
        }

        if (searchCommand.getProcessId() != null) {
            predicates.add(new Predicates.EqualPredicate("processId", searchCommand.getProcessId()));
        }

        if (StringUtils.hasText(searchCommand.getErrorClassName())) {
            predicates.add(new Predicates.LikePredicate("errorClassName", searchCommand.getErrorClassName() + WILDCARD_SYMBOL));
        }

        if (StringUtils.hasText(searchCommand.getErrorMessage())) {
            predicates.add(new Predicates.LikePredicate("errorMessage", searchCommand.getErrorMessage() + WILDCARD_SYMBOL));
        }

        if (searchCommand.getEndPeriod() > 0) {
            predicates.add(new Predicates.BetweenPredicate("time", 0l, searchCommand.getEndPeriod()));
        }

        if (searchCommand.getStartPeriod() > 0) {
            predicates.add(new Predicates.BetweenPredicate("time", searchCommand.getStartPeriod(), Long.MAX_VALUE));
        }

        Collection<InterruptedTask> result = null;
        if (predicates.isEmpty()) {
            result = storeIMap.values();
        } else {
            Predicate[] predicateArray = new Predicate[predicates.size()];
            Predicate predicate = new Predicates.AndPredicate(predicates.toArray(predicateArray));
            result = storeIMap.values(predicate);
        }
        logger.trace("Found [{}] interrupted tasks by command[{}]", result!=null?result.size():null, searchCommand);
        return result;
    }

    @Override
    public Collection<InterruptedTask> findAll() {
        return storeIMap.values();
    }

    @Override
    public void delete(UUID processId, UUID taskId) {
        storeIMap.delete(taskId);
    }

}