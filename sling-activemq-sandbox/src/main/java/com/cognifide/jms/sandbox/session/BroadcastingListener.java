package com.cognifide.jms.sandbox.session;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.sandbox.session.wrapper.SessionListener;

public class BroadcastingListener implements SessionListener {

	private static final Logger LOG = LoggerFactory.getLogger(BroadcastingListener.class);

	private final Session session;

	private final MessageProducer producer;

	private final String sharedSessionId;

	private final Object mutex;

	public BroadcastingListener(Session session, MessageProducer producer, String sharedSessionId,
			Object mutex) {
		this.session = session;
		this.producer = producer;
		this.sharedSessionId = sharedSessionId;
		this.mutex = mutex;
	}

	@Override
	public void invalidate() {
		try {
			MapMessage msg = session.createMapMessage();
			msg.setString(SharedSessionStorage.ACTION, Action.INVALIDATE.name());
			msg.setString(SharedSessionStorage.SHARED_SESSION_ID, sharedSessionId);
			sendMessage(msg);
		} catch (JMSException e) {
			LOG.error("Can't broadcast session change", e);
		}
	}

	@Override
	public void putValue(String key, Object value) {
		try {
			MapMessage msg = session.createMapMessage();
			msg.setString(SharedSessionStorage.ACTION, Action.PUT_VALUE.name());
			msg.setString(SharedSessionStorage.SHARED_SESSION_ID, sharedSessionId);
			msg.setString(SharedSessionStorage.KEY, key);
			msg.setObject(SharedSessionStorage.VALUE, value);
			sendMessage(msg);
		} catch (JMSException e) {
			LOG.error("Can't broadcast session change", e);
		}
	}

	@Override
	public void removeValue(String key) {
		try {
			MapMessage msg = session.createMapMessage();
			msg.setString(SharedSessionStorage.ACTION, Action.REMOVE_VALUE.name());
			msg.setString(SharedSessionStorage.SHARED_SESSION_ID, sharedSessionId);
			msg.setString(SharedSessionStorage.KEY, key);
			sendMessage(msg);
		} catch (JMSException e) {
			LOG.error("Can't broadcast session change", e);
		}
	}

	private void sendMessage(MapMessage msg) throws JMSException {
		synchronized (mutex) {
			producer.send(msg);
		}
	}
}
