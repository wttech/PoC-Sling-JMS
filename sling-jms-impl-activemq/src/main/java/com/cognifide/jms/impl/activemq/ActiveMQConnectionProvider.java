package com.cognifide.jms.impl.activemq;

import java.util.Map;

import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.impl.activemq.blob.SlingBlobServlet;
import com.cognifide.jms.impl.activemq.blob.SlingBlobTransferPolicy;

@Component(immediate = true, metatype = false)
@Service
@Properties({ @Property(name = ActiveMQConnectionProvider.ACTIVEMQ_URL_PROPERTY_NAME, value = ActiveMQConnectionProvider.ACTIVEMQ_URL_DEFAULT, label = "ActiveMQ URL") })
public class ActiveMQConnectionProvider implements JmsConnectionProvider {

	protected static final String ACTIVEMQ_URL_PROPERTY_NAME = "activeMqUrl";

	protected static final String ACTIVEMQ_URL_DEFAULT = "tcp://localhost:61616";

	private ActiveMQConnectionFactory connectionFactory;

	@Reference
	private SlingBlobServlet blobServlet;

	@Activate
	protected void activate(Map<String, Object> config) {
		String url = PropertiesUtil.toString(config.get(ACTIVEMQ_URL_PROPERTY_NAME), ACTIVEMQ_URL_DEFAULT);
		connectionFactory = new ActiveMQConnectionFactory(url);
		connectionFactory.setBlobTransferPolicy(new SlingBlobTransferPolicy(blobServlet));
	}

	public ActiveMQConnection getConnection() throws JMSException {
		return (ActiveMQConnection) connectionFactory.createConnection();
	}

}
