package com.cognifide.activemq.core.consumer;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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

public class ListenerList implements MessageListener {

	private static final Logger LOG = LoggerFactory.getLogger(ListenerList.class);

	private final List<MessageListener> listeners;

	private final Map<MessageListener, Filter> filters;

	private final ConsumerKey key;

	private MessageConsumer consumer;

	public ListenerList(ConsumerKey key) {
		this.listeners = new ArrayList<MessageListener>();
		this.filters = new HashMap<MessageListener, Filter>();
		this.key = key;
	}

	public synchronized void addListener(MessageListener listener) throws JMSException {
		listeners.add(listener);
	}

	public synchronized void addFilteredListener(MessageListener listener, String filter)
			throws JMSException, InvalidSyntaxException {
		filters.put(listener, FrameworkUtil.createFilter(filter));
		addListener(listener);
	}

	public synchronized void removeListener(MessageListener listener) throws JMSException {
		filters.remove(listener);
		listeners.remove(listener);
	}

	public synchronized void open(Session session) throws JMSException {
		if (consumer != null) {
			return;
		}
		Destination dest = key.createDestination(session);
		consumer = session.createConsumer(dest);
		consumer.setMessageListener(this);
	}

	public synchronized void close() throws JMSException {
		if (consumer != null) {
			consumer.close();
			consumer = null;
		}
	}

	public boolean isEmpty() {
		return listeners.isEmpty();
	}

	@Override
	public synchronized void onMessage(Message message) {
		for (MessageListener l : listeners) {
			if (filterMatch(l, message)) {
				l.onMessage(message);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private boolean filterMatch(MessageListener listener, Message message) {
		Filter filter = filters.get(listener);
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
