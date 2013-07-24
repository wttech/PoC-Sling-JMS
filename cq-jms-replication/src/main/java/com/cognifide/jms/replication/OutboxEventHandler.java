package com.cognifide.jms.replication;

import java.util.Map;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.EventUtil;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.api.SlingJmsProperties;

@Component(immediate = true, metatype = true)
@Service(value = EventHandler.class)
@Properties({
		@Property(name = OutboxEventHandler.RUN_MODE_PROPERTY, value = OutboxEventHandler.RUN_MODE_DEFAULT),
		@Property(name = OutboxEventHandler.AGENT_ID_PROPERTY, value = OutboxEventHandler.AGENT_ID_DEFAULT),
		@Property(name = EventConstants.EVENT_TOPIC, value = "org/apache/sling/event/notification/job/FINISHED", propertyPrivate = true),
		@Property(name = EventConstants.EVENT_FILTER, value = "(event.job.topic=com/day/cq/replication/job/outbox)", propertyPrivate = true) })
public class OutboxEventHandler implements EventHandler {

	private static final Logger LOG = LoggerFactory.getLogger(OutboxEventHandler.class);

	protected static final String RUN_MODE_PROPERTY = "runMode";

	protected static final String RUN_MODE_DEFAULT = "author";

	protected static final String AGENT_ID_PROPERTY = "agentId";

	protected static final String AGENT_ID_DEFAULT = "publish_reverse";

	public static final String TOPIC = "jmsTransportHandler";

	@Reference
	private JmsConnectionProvider connectionProvider;

	@Reference
	private SlingSettingsService settingService;

	private boolean isPublish;

	private Connection connection;

	private Session session;

	private MessageProducer producer;

	private String runMode;

	private String agentId;

	@Activate
	protected void activate(Map<String, Object> config) throws JMSException {
		isPublish = settingService.getRunModes().contains("publish");
		if (!isPublish) {
			return;
		}

		connection = connectionProvider.getConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination dest = session.createTopic(TOPIC);
		producer = session.createProducer(dest);
		connection.start();

		agentId = (String) config.get(AGENT_ID_PROPERTY);
		runMode = (String) config.get(RUN_MODE_PROPERTY);
	}

	@Deactivate
	protected void deactivate() throws JMSException {
		if (!isPublish) {
			return;
		}

		producer.close();
		session.close();
		connection.close();
	}

	public void poll() {
		try {
			MapMessage msg = session.createMapMessage();
			msg.setStringProperty(SlingJmsProperties.DESTINATION_RUN_MODE, runMode);
			msg.setString("agentId", agentId);
			producer.send(msg);
		} catch (JMSException e) {
			LOG.error("Can't send message", e);
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (isPublish && EventUtil.isLocal(event)) {
			poll();
		}
	}
}
