package com.cognifide.jms.discovery;

import java.io.Serializable;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.discovery.election.ElectionRequest;
import com.cognifide.jms.discovery.election.ElectionRequestType;

public class DiscoveryJmsProducer {

	public static final String TOPIC = "jmsDiscoveryService";

	private final Connection connection;

	private final Session producerSession;

	private final MessageProducer producer;

	public DiscoveryJmsProducer(JmsConnectionProvider connectionProvider) throws JMSException {
		connection = connectionProvider.getConnection();
		connection.start();
		producerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination destination = producerSession.createTopic(TOPIC);
		producer = producerSession.createProducer(destination);
	}

	public void close() throws JMSException {
		producer.close();
		producerSession.close();
		connection.close();
	}

	public void sendObjectMsg(Serializable object) throws JMSException {
		ObjectMessage objMsg = producerSession.createObjectMessage(object);
		producer.send(objMsg);
	}

	public void sendElectionReq(ElectionRequestType type, String clusterId, String slingId)
			throws JMSException {
		ElectionRequest req = new ElectionRequest(type, clusterId, slingId);
		ObjectMessage msg = producerSession.createObjectMessage(req);
		producer.send(msg);
	}
}
