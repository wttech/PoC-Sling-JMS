package com.cognifide.activemq.core.consumer;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

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
import com.cognifide.jms.api.JmsConstants;

@Component(immediate = true, metatype = false)
public class MessageListenerRegistry {

	@Reference
	private JmsConnectionProvider connectionProvider;

	@Reference(referenceInterface = MessageListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private Map<ConsumerKey, ListenerList> listeners = new HashMap<ConsumerKey, ListenerList>();

	private Connection connection;

	private Session session;

	@Activate
	public void activate(ComponentContext ctx) throws JMSException, MalformedURLException,
			InvalidSyntaxException {
		connection = connectionProvider.getConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		connection.start();

		for (ListenerList list : listeners.values()) {
			list.open(session);
		}
	}

	@Deactivate
	public void deactivate() throws JMSException {
		for (ListenerList l : listeners.values()) {
			l.close();
		}
		session.close();
		connection.close();
		listeners.clear();
	}

	public void bindListeners(MessageListener listener, Map<String, Object> properties) throws JMSException,
			InvalidSyntaxException {
		ConsumerKey key = new ConsumerKey(properties);
		if (!listeners.containsKey(key)) {
			listeners.put(key, new ListenerList(key));
		}
		ListenerList list = listeners.get(key);
		if (properties.get(JmsConstants.FILTER) != null) {
			list.addFilteredListener(listener, (String) properties.get(JmsConstants.FILTER));
		} else {
			list.addListener(listener);
		}
		if (session != null) {
			list.open(session);
		}
	}

	public void unbindListeners(MessageListener listener, Map<String, Object> properties) throws JMSException {
		ConsumerKey key = new ConsumerKey(properties);
		if (listeners.containsKey(key)) {
			ListenerList list = listeners.get(key);
			list.removeListener(listener);
			if (list.isEmpty()) {
				listeners.remove(key);
			}
		}
	}
}