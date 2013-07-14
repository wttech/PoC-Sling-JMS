package com.cognifide.activemq.core.consumer;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

public class ListenerList implements MessageListener {
	private final List<MessageListener> listeners;

	private final ConsumerKey key;

	private MessageConsumer consumer;

	public ListenerList(ConsumerKey key) {
		this.listeners = new ArrayList<MessageListener>();
		this.key = key;
	}

	public synchronized void addListener(MessageListener listener) throws JMSException {
		listeners.add(listener);
	}

	public synchronized void removeListener(MessageListener listener) throws JMSException {
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
			l.onMessage(message);
		}
	}
}
