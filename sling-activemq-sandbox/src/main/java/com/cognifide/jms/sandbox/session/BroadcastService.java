package com.cognifide.jms.sandbox.session;

import java.net.MalformedURLException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQSession;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.sandbox.session.wrapper.SessionListener;

@Component
@Service(value = BroadcastService.class)
public class BroadcastService {

	@Reference
	private JmsConnectionProvider connectionProvider;

	private ActiveMQConnection connection;

	private ActiveMQSession session;

	private MessageProducer producer;

	@Activate
	public void activate() throws JMSException, MalformedURLException {
		connection = (ActiveMQConnection) connectionProvider.getConnection();
		session = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination dest = session.createTopic(SharedSessionStorage.TOPIC);
		producer = session.createProducer(dest);
		connection.start();
	}

	@Deactivate
	public void deactivate() throws JMSException {
		producer.close();
		session.close();
		connection.close();
	}

	public SessionListener getBroadcastingListener(String sharedSessionId) {
		return new BroadcastingListener(session, producer, sharedSessionId, this);
	}
}
