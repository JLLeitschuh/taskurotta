package ru.taskurotta.dropwizard.test.client.serialization;

import java.util.UUID;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.core.ArgType;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.util.ActorDefinition;

public class EntitiesFactory {

    public static ActorDefinition createActorDefinition() {
        return ActorDefinition.valueOf("test.me.worker", "7.6.5");
    }

    public static TaskContainer createTaskContainer() {
        UUID originalUuid = UUID.randomUUID();
        UUID processUuid = UUID.randomUUID();
        TaskType originalTaskType = TaskType.WORKER;
        String originalName = "test.me.worker";
        String originalVersion = "7.6.5";
        String originalMethod = "doSomeWork";
        String originalActorId = originalName + "#" + originalVersion;
        long originalStartTime = System.currentTimeMillis();
        int originalNumberOfAttempts = 5;

        String origArg1ClassName = "null";
        String origArg1Value = "null";
        ArgContainer originalArg1 = new ArgContainer(origArg1ClassName, ArgContainer.ValueType.PROMISE, originalUuid, false, origArg1Value);

        String origArg2ClassName = "java.lang.String";
        String origArg2Value = "string value here";
        ArgContainer originalArg2 = new ArgContainer(origArg2ClassName, ArgContainer.ValueType.PLAIN, originalUuid, true, origArg2Value);


        ArgType[] argTypes = new ArgType[]{ArgType.WAIT, ArgType.NONE};
        TaskOptionsContainer originalOptions = new TaskOptionsContainer(argTypes);

        return new TaskContainer(originalUuid, processUuid, originalMethod, originalActorId, originalTaskType,originalStartTime, originalNumberOfAttempts, new ArgContainer[]{originalArg1, originalArg2}, originalOptions);
    }

    public static DecisionContainer createDecisionContainer(boolean isError) {
        UUID taskId =UUID.randomUUID();
        UUID processId =UUID.randomUUID();
        TaskContainer[] tasks = new TaskContainer[2];
        tasks[0] = createTaskContainer();
        tasks[1] = createTaskContainer();
        if(isError) {
            return new DecisionContainer(taskId, processId, null, createErrorContainer(), System.currentTimeMillis()+9000l, tasks);
        } else {
            return new DecisionContainer(taskId, processId, createArgSimpleValue(taskId), null, TaskDecision.NO_RESTART, tasks);
        }

    }

    public static ErrorContainer createErrorContainer() {
        ErrorContainer result = new ErrorContainer();
        result.setClassName(Throwable.class.getName());
        result.setMessage("Test exception");
        result.setStackTrace("Test stack trace");
        return result;
    }

    public static ArgContainer createArgSimpleValue(UUID taskId) {
        String value = "simple string value";
        return new ArgContainer(value.getClass().getName(), ArgContainer.ValueType.PLAIN, taskId, true, value);
    }

}
