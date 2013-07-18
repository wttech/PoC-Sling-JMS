package com.cognifide.activemq.core.consumer;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConstants;
import com.cognifide.jms.api.SlingJmsProperties;

public class Consumer implements MessageListener {

	private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

	private final Map<String, Object> properties;

	private final MessageListener listener;

	private final Set<String> runModes;

	private final Filter filter;

	private MessageConsumer msgConsumer;

	public Consumer(MessageListener listener, Set<String> runModes, Map<String, Object> properties)
			throws JMSException, InvalidSyntaxException {
		this.properties = properties;
		this.listener = listener;
		this.runModes = runModes;
		if (properties.containsKey(JmsConstants.FILTER)) {
			this.filter = FrameworkUtil.createFilter((String) properties.get(JmsConstants.FILTER));
		} else {
			this.filter = null;
		}
	}

	public void connect(Session session) throws JMSException {
		if (msgConsumer != null) {
			return;
		}

		ConsumerType type = ConsumerType.valueOf((String) properties.get(JmsConstants.CONSUMER_TYPE));
		String name = (String) properties.get(JmsConstants.CONSUMER_SUBJECT);
		Destination dest;
		if (type == ConsumerType.QUEUE) {
			dest = session.createQueue(name);
		} else if (type == ConsumerType.TOPIC) {
			dest = session.createTopic(name);
		} else {
			throw new IllegalArgumentException("Not supported consumer type: " + type);
		}
		msgConsumer = session.createConsumer(dest);
		msgConsumer.setMessageListener(this);
	}

	public void close() throws JMSException {
		if (msgConsumer != null) {
			msgConsumer.close();
			msgConsumer = null;
		}
	}

	public boolean connected() {
		return msgConsumer != null;
	}

	@Override
	public void onMessage(Message message) {
		if (filterMatch(message) && instanceMatch(message)) {
			listener.onMessage(message);
		}
	}

	private boolean instanceMatch(Message message) {
		try {
			if (message.propertyExists(SlingJmsProperties.DESTINATION_RUN_MODE)) {
				String destRunMode = message.getStringProperty(SlingJmsProperties.DESTINATION_RUN_MODE);
				return runModes.contains(destRunMode);
			} else {
				return true;
			}
		} catch (JMSException e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean filterMatch(Message message) {
		if (filter == null) {
			return true;
		}
		Dictionary<String, Object> dictionary = new Hashtable<String, Object>();

		try {
			Enumeration<String> enumeration = message.getPropertyNames();
			while (enumeration.hasMoreElements()) {
				String name = enumeration.nextElement();
				Object property = message.getObjectProperty(name);
				dictionary.put(name, property);
			}
		} catch (JMSException e) {
			LOG.error("Can't get properties", e);
		}
		return filter.match(dictionary);
	}

}
