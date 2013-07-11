package com.cognifide.jms.sandbox.session;

import java.net.MalformedURLException;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQSession;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.sandbox.session.model.SharedSession;

@Component
@Service(value = SharedSessionStorage.class)
public class SharedSessionStorage implements MessageListener {

	private static final Logger LOG = LoggerFactory.getLogger(SharedSessionStorage.class);

	static final String KEY = "key";

	static final String VALUE = "value";

	static final String ACTION = "action";

	static final String SHARED_SESSION_ID = "sharedSessionId";

	static final String TOPIC = SharedSessionStorage.class.getName();

	@Reference
	private JmsConnectionProvider connectionProvider;

	private ActiveMQConnection connection;

	private ActiveMQSession session;

	private MessageConsumer consumer;

	private Map<String, SharedSession> sessions;

	@Activate
	public void activate() throws JMSException, MalformedURLException {
		connection = (ActiveMQConnection) connectionProvider.getConnection();
		session = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination dest = session.createTopic(TOPIC);
		consumer = session.createConsumer(dest);
		connection.start();
		consumer.setMessageListener(this);
	}

	@Deactivate
	public void deactivate() throws JMSException {
		consumer.close();
		session.close();
		connection.close();
	}

	@Override
	public synchronized void onMessage(Message message) {
		try {
			MapMessage map = (MapMessage) message;
			String sessionId = map.getString(SHARED_SESSION_ID);
			Action action = Action.valueOf(map.getString(ACTION));
			String key = map.getString(KEY);
			switch (action) {
				case PUT_VALUE:
					getSharedSession(sessionId).putValue(key, map.getObject(VALUE));
					break;

				case REMOVE_VALUE:
					getSharedSession(sessionId).removeValue(key);
					break;

				case INVALIDATE:
					invalidate(sessionId);
					break;
			}
		} catch (JMSException e) {
			LOG.error("Can't parse incoming message", e);
		}
	}

	public synchronized SharedSession getSharedSession(String sessionId) {
		if (!sessions.containsKey(sessionId)) {
			sessions.put(sessionId, new SharedSession());
		}
		return sessions.get(sessionId);
	}

	private synchronized void invalidate(String sessionId) {
		sessions.remove(sessionId);
	}
}
