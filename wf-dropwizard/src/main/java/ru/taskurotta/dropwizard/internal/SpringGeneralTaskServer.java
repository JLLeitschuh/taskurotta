package ru.taskurotta.dropwizard.internal;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.impl.ConfigBackendAware;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.util.ActorDefinition;

public class SpringGeneralTaskServer implements TaskServer, ConfigBackendAware {

	private static final Logger logger = LoggerFactory.getLogger(SpringGeneralTaskServer.class);
	
	private GeneralTaskServer taskServer;
	
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;
	
    private Map<String, Runnable> daemonTasks;
    
	@PostConstruct
    public void init() {
    	taskServer = new GeneralTaskServer(taskBackend, queueBackend, dependencyBackend, configBackend);
    	
    	if(daemonTasks!=null && !daemonTasks.isEmpty()) {
    		for(String daemonName: daemonTasks.keySet()) {
    			Thread runner = new Thread(daemonTasks.get(daemonName));
    			runner.setDaemon(true);
    			runner.setName(daemonName);
    			runner.start();
    		}
    		logger.info("Started [{}] daemon tasks: [{}]", daemonTasks.size(), daemonTasks.keySet());
    	}
    }
    
	public void setQueueBackend(QueueBackend queueBackend) {
		this.queueBackend = queueBackend;
	}

	public void setDependencyBackend(DependencyBackend dependencyBackend) {
		this.dependencyBackend = dependencyBackend;
	}

	@Override
	public void startProcess(TaskContainer task) {
		taskServer.startProcess(task);
	}

	@Override
	public TaskContainer poll(ActorDefinition actorDefinition) {
		return taskServer.poll(actorDefinition);
	}

	@Override
	public void release(DecisionContainer taskResult) {
		taskServer.release(taskResult);
	}

	public void setTaskBackend(TaskBackend taskBackend) {
		this.taskBackend = taskBackend;
	}

	@Override
	public void setConfigBackend(ConfigBackend config) {
		this.configBackend = config;
	}

	public void setDaemonTasks(Map<String, Runnable> daemonTasks) {
		this.daemonTasks = daemonTasks;
	}
	
}
