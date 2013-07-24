package com.cognifide.jms.session;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.consumer.DestinationType;
import com.cognifide.jms.api.consumer.SlingMessageConsumer;
import com.cognifide.jms.session.model.SessionDiff;
import com.cognifide.jms.session.model.SharedSession;

@Service(value = { MessageListener.class, SharedSessionStorage.class })
@SlingMessageConsumer(destinationType = DestinationType.TOPIC, subject = SharedSessionStorage.TOPIC, generateService = false)
public class SharedSessionStorage implements MessageListener {

	private static final Logger LOG = LoggerFactory.getLogger(SharedSessionStorage.class);

	private static final String PREFIX = SharedSessionStorage.class.getName();

	public static final String ACTION = PREFIX + ".action";

	public static final String SHARED_SESSION_ID = PREFIX + ".sharedSessionId";

	static final String TOPIC = "sharedSessionTopic";

	private Map<String, SharedSession> sessions;

	private ScheduledExecutorService executor;

	@Reference
	private ClassLoaderRegistry clRegistry;

	@Activate
	public void activate() throws JMSException, MalformedURLException {
		sessions = new HashMap<String, SharedSession>();

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				reaper();
			}
		}, 0, 1, TimeUnit.SECONDS);
	}

	@Deactivate
	public void deactivate() throws JMSException {
		executor.shutdown();
	}

	public synchronized SharedSession getSharedSession(String sessionId) {
		if (sessionId == null) {
			throw new NullPointerException();
		}
		if (!sessions.containsKey(sessionId)) {
			sessions.put(sessionId, new SharedSession(sessionId));
		}
		return sessions.get(sessionId);
	}

	@Override
	public synchronized void onMessage(Message msg) {
		try {
			SharedSession session = getSharedSession(msg.getStringProperty(SHARED_SESSION_ID));
			Action action = Action.valueOf(msg.getStringProperty(ACTION));
			if (action == Action.UPDATE) {
				SessionDiff diff = getObject((ObjectMessage) msg);
				session.applyDiff(diff);
			} else if (action == Action.REFRESH) {
				session.refreshSession();
			}
		} catch (JMSException e) {
			LOG.error("Can't parse incoming message", e);
		}
	}

	private synchronized void reaper() {
		Iterator<SharedSession> iterator = sessions.values().iterator();
		while (iterator.hasNext()) {
			SharedSession s = iterator.next();
			if (!s.isValid()) {
				iterator.remove();
			}
		}
	}

	private SessionDiff getObject(ObjectMessage msg) throws JMSException {
		Thread currentThread = Thread.currentThread();
		ClassLoader cl = currentThread.getContextClassLoader();
		try {
			currentThread.setContextClassLoader(clRegistry.getClassLoader());
			return (SessionDiff) msg.getObject();
		} finally {
			currentThread.setContextClassLoader(cl);
		}
	}
}
