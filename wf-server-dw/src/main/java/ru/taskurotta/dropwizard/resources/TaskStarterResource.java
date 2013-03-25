package ru.taskurotta.dropwizard.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.client.serialization.wrapper.TaskContainerWrapper;
import ru.taskurotta.server.TaskServer;

import com.yammer.metrics.annotation.Timed;

@Path("/tasks/start")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskStarterResource {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskStarterResource.class);	
	private TaskServer taskServer;
	
	@POST
	@Timed
	public Response startAction(TaskContainerWrapper taskContainerWrapper) {
		logger.debug("startAction resource called with entity[{}]", taskContainerWrapper);
		
		try {
			taskServer.startProcess(taskContainerWrapper.getTaskContainer());	
		} catch(Exception e) {
			logger.error("Starting of task["+taskContainerWrapper+"] failed!", e);
			return Response.serverError().build();
		}
		
		return Response.ok().build();
		
	}

	public void setTaskServer(TaskServer taskServer) {
		this.taskServer = taskServer;
	}
	
}
