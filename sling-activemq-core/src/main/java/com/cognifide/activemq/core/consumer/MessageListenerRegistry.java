package com.cognifide.activemq.core.consumer;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

import com.cognifide.jms.api.JmsConnectionProvider;

@Component(immediate = true, metatype = false)
public class MessageListenerRegistry {

	@Reference
	private JmsConnectionProvider connectionProvider;

	@Reference(referenceInterface = MessageListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private Set<MessageListener> listeners = new CopyOnWriteArraySet<MessageListener>();

	private Connection connection;

	private Session session;

	private Map<ConsumerKey, ListenerList> consumers = new HashMap<ConsumerKey, ListenerList>();

	@Activate
	public void activate(ComponentContext ctx) throws JMSException, MalformedURLException,
			InvalidSyntaxException {
		connection = connectionProvider.getConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		connection.start();

		for (ListenerList list : consumers.values()) {
			list.open(session);
		}
	}

	@Deactivate
	public void deactivate() throws JMSException {
		for (ListenerList l : consumers.values()) {
			l.close();
		}
		session.close();
		connection.close();
		consumers.clear();
	}

	public void bindListeners(MessageListener listener, Map<String, Object> properties) throws JMSException {
		ConsumerKey key = new ConsumerKey(properties);
		if (!consumers.containsKey(key)) {
			consumers.put(key, new ListenerList(key));
		}
		ListenerList list = consumers.get(key);
		list.addListener(listener);
		if (session != null) {
			list.open(session);
		}
		listeners.add(listener);
	}

	public void unbindListeners(MessageListener listener, Map<String, Object> properties) throws JMSException {
		ConsumerKey key = new ConsumerKey(properties);
		if (consumers.containsKey(key)) {
			ListenerList list = consumers.get(key);
			list.removeListener(listener);
			if (list.isEmpty()) {
				consumers.remove(key);
			}
		}
		listeners.remove(listener);
	}
}