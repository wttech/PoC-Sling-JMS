package com.cognifide.jms.impl.activemq.consumer;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

import com.cognifide.jms.api.JmsConnectionProvider;

@Component(immediate = true, metatype = false)
public class MessageListenerRegistry {

	@Reference
	private SlingSettingsService settingsService;

	@Reference
	private JmsConnectionProvider connectionProvider;

	@Reference(referenceInterface = MessageListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private Map<MessageListener, Consumer> consumers = new HashMap<MessageListener, Consumer>();

	private Connection connection;

	private Session session;

	private Set<String> runModes;

	@Activate
	public void activate(ComponentContext ctx) throws JMSException, MalformedURLException,
			InvalidSyntaxException {
		runModes = settingsService.getRunModes();
		connection = connectionProvider.getConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		connection.start();
		for (Consumer c : consumers.values()) {
			c.connect(session);
		}
	}

	@Deactivate
	public void deactivate() throws JMSException {
		for (Consumer c : consumers.values()) {
			c.close();
		}
		session.close();
		connection.close();
		consumers.clear();
	}

	public void bindConsumers(MessageListener listener, Map<String, Object> properties) throws JMSException,
			InvalidSyntaxException {
		Consumer consumer = new Consumer(listener, runModes, properties);
		consumers.put(listener, consumer);
		if (session != null) {
			consumer.connect(session);
		}
	}

	public void unbindConsumers(MessageListener listener, Map<String, Object> properties) throws JMSException {
		Consumer consumer = consumers.get(consumers);
		if (consumer != null) {
			consumer.close();
		}
	}
}