package ru.taskurotta.backend.storage;

import net.sf.cglib.core.CollectionUtils;
import net.sf.cglib.core.Predicate;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 8:02 PM
 */
public class MemoryProcessBackend implements ProcessBackend, ProcessInfoRetriever {

    private CheckpointService checkpointService = new MemoryCheckpointService();
    private Map<UUID, ProcessVO> processesStorage = new ConcurrentHashMap<>();


    @Override
    public void startProcess(TaskContainer task) {
        ProcessVO process = new ProcessVO();
        process.setStartTime(System.currentTimeMillis());
        process.setProcessUuid(task.getProcessId());
        process.setStartTaskUuid(task.getTaskId());
        processesStorage.put(task.getProcessId(), process);

        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_SCHEDULE_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime()));
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_COMMIT, task.getProcessId(), task.getActorId(), task.getStartTime()));
    }

    @Override
    public void startProcessCommit(TaskContainer task) {

        //should be at the end of the method
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime()));
        checkpointService.removeEntityCheckpoints(task.getProcessId(), TimeoutType.PROCESS_START_TO_COMMIT);
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {

        ProcessVO process = processesStorage.get(processId);
        process.setEndTime(System.currentTimeMillis());
        process.setReturnValueJson(returnValue);
        processesStorage.put(processId, process);

        //should be at the end of the method
        checkpointService.removeEntityCheckpoints(processId, TimeoutType.PROCESS_START_TO_CLOSE);
        checkpointService.removeEntityCheckpoints(processId, TimeoutType.PROCESS_SCHEDULE_TO_CLOSE);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    @Override
    public ProcessVO getProcess(UUID processUUID) {
        return processesStorage.get(processUUID);
    }

    @Override
    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize) {
        List<ProcessVO> result = new ArrayList<>();
        if (!processesStorage.isEmpty()) {
            ProcessVO[] processes = new ProcessVO[processesStorage.values().size()];
            processes = processesStorage.values().toArray(processes);
            int pageStart = (pageNumber - 1) * pageSize;
            int pageEnd = (pageSize * pageNumber >= processes.length) ? processes.length : pageSize * pageNumber;
            result.addAll(Arrays.asList(processes).subList(pageStart, pageEnd));
        }
        return new GenericPage<>(result, pageNumber, pageSize, processesStorage.values().size());

    }

    @Override
    public List<ProcessVO> findProcesses(String type, final String id) {
        List<ProcessVO> result = new ArrayList<>();
        if ((id != null) && (!id.isEmpty())) {
            if (SEARCH_BY_ID.equals(type)) {
                result.addAll(CollectionUtils.filter(processesStorage.values(), new Predicate() {
                    @Override
                    public boolean evaluate(Object o) {
                        ProcessVO process = (ProcessVO) o;
                        return process.getProcessUuid().toString().startsWith(id);
                    }
                }));
            } else if (SEARCH_BY_CUSTOM_ID.equals(type)) {
                result.addAll(CollectionUtils.filter(processesStorage.values(), new Predicate() {
                    @Override
                    public boolean evaluate(Object o) {
                        ProcessVO process = (ProcessVO) o;
                        return process.getCustomId().startsWith(id);
                    }
                }));
            }
        }
        return result;
    }
}
