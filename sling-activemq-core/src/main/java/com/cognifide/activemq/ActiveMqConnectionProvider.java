package com.cognifide.activemq;

import java.util.Dictionary;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

import com.cognifide.activemq.blob.SlingBlobServlet;
import com.cognifide.activemq.blob.SlingBlobTransferPolicy;
import com.cognifide.jms.api.JmsConnectionProvider;

@Component(immediate = true, metatype = false)
@Service
@Properties({ @Property(name = ActiveMqConnectionProvider.ACTIVEMQ_URL_PROPERTY_NAME, value = ActiveMqConnectionProvider.ACTIVEMQ_URL_DEFAULT, label = "ActiveMQ URL") })
public class ActiveMqConnectionProvider implements JmsConnectionProvider {

	protected static final String ACTIVEMQ_URL_PROPERTY_NAME = "activeMqUrl";

	protected static final String ACTIVEMQ_URL_DEFAULT = "tcp://localhost:61616";

	private ActiveMQConnectionFactory connectionFactory;

	@Reference
	private SlingBlobServlet blobServlet;
	
	@Activate
	protected void activate(ComponentContext context) {
		Dictionary<?, ?> config = context.getProperties();
		String url = PropertiesUtil.toString(config.get(ACTIVEMQ_URL_PROPERTY_NAME), ACTIVEMQ_URL_DEFAULT);
		connectionFactory = new ActiveMQConnectionFactory(url);
		connectionFactory.setBlobTransferPolicy(new SlingBlobTransferPolicy(blobServlet));
	}

	public Connection getConnection() throws JMSException {
		return connectionFactory.createConnection();
	}
}
