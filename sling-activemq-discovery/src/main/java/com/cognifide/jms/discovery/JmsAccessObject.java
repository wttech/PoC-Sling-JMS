package com.cognifide.jms.discovery;

import java.io.Serializable;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.discovery.election.ElectionRequest;
import com.cognifide.jms.discovery.election.ElectionRequestType;
import com.cognifide.jms.discovery.update.UpdateMessage;

public class JmsAccessObject {

	private static final Logger LOG = LoggerFactory.getLogger(JmsAccessObject.class);

	private final Connection connection;

	private final Session consumerSession;

	private final Session producerSession;

	private final MessageConsumer consumer;

	private final MessageProducer producer;

	public JmsAccessObject(JmsConnectionProvider connectionProvider) throws JMSException {
		connection = connectionProvider.getConnection();
		connection.start();
		consumerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		producerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination destination = consumerSession.createTopic(JmsDiscoveryService.TOPIC);
		consumer = consumerSession.createConsumer(destination);
		producer = producerSession.createProducer(destination);
	}

	public void close() throws JMSException {
		consumer.close();
		consumerSession.close();
		producer.close();
		producerSession.close();
		connection.close();
	}

	public Object getObjectMsg(long timeout) throws JMSException {
		Message msg = consumer.receive(timeout);
		if (msg == null) {
			return null;
		}
		if (!(msg instanceof ObjectMessage)) {
			LOG.error("Wrong type of message: ", msg.getClass());
			return null;
		}
		ObjectMessage objectMsg = (ObjectMessage) msg;
		Serializable object = getObjectInOwnClassLoader(objectMsg);
		return object;
	}

	public void sendObjectMsg(Serializable object) throws JMSException {
		ObjectMessage objMsg = consumerSession.createObjectMessage(object);
		producer.send(objMsg);
	}

	public void sendElectionReq(ElectionRequestType type, String clusterId, String slingId)
			throws JMSException {
		ElectionRequest req = new ElectionRequest(type, clusterId, slingId);
		ObjectMessage msg = consumerSession.createObjectMessage(req);
		producer.send(msg);
	}

	private Serializable getObjectInOwnClassLoader(ObjectMessage objectMsg) throws JMSException {
		// TODO - it has to be a better way to do it
		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(UpdateMessage.class.getClassLoader());
		try {
			Serializable object = objectMsg.getObject();
			return object;
		} finally {
			Thread.currentThread().setContextClassLoader(oldCl);
		}
	}
}
