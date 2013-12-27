package ru.taskurotta.client.jersey;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import ru.taskurotta.server.TaskServerResource;

import javax.annotation.PostConstruct;

/**
 * TaskServer implementation as a simple native jersey client proxy.
 */
public class JerseyTaskServerProxy extends BaseTaskProxy {

    @PostConstruct
    public void init() {
        ClientConfig cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_THREADPOOL_SIZE, threadPoolSize);
        cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
        cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);

        Client client = Client.create(cc);

        startResource = client.resource(getContextUrl(TaskServerResource.START));
        pullResource = client.resource(getContextUrl(TaskServerResource.POLL));
        releaseResource = client.resource(getContextUrl(TaskServerResource.RELEASE));

        //Prints JSON request to console
        //client.addFilter(new LoggingFilter(System.out));

    }

}
