package ru.taskurotta.service.ora;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.hazelcast.util.ConfigUtil;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.ora.storage.OraProcessService;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * User: moroz
 * Date: 29.04.13
 */
public class ProcessTestIT {

    private DbConnect connection = new DbConnect();
    private OraProcessService dao = new OraProcessService(ConfigUtil.newInstanceWithoutMulticast(), connection
            .getDataSource());

    @Ignore
    @Test
    public void test() {
        TaskContainer task = SerializationTest.createTaskContainer();
        dao.startProcess(task);

        Process process = dao.getProcess(task.getProcessId());
        Assert.assertNull(process.getStartTask());
        TaskContainer getedTask = dao.getStartTask(task.getProcessId());
        Assert.assertEquals(task.getTaskId(), getedTask.getTaskId());

    }

}
