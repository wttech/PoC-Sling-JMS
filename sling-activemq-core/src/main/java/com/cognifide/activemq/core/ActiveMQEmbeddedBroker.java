package com.cognifide.activemq.core;

import java.util.Map;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.commons.osgi.PropertiesUtil;

@Component(immediate = true, metatype = true)
@Properties({
		@Property(name = ActiveMQEmbeddedBroker.EMBEDDED_BROKER_ENABLED, boolValue = ActiveMQEmbeddedBroker.EMBEDDED_BROKER_ENABLED_DEFAULT, label = "Embedded broker enabled"),
		@Property(name = ActiveMQEmbeddedBroker.BROKER_FACTORY_URI, value = ActiveMQEmbeddedBroker.BROKER_FACTORY_URI_DEFAULT, label = "Broker factory URI") })
public class ActiveMQEmbeddedBroker {

	protected static final String EMBEDDED_BROKER_ENABLED = "enabled";

	protected static final boolean EMBEDDED_BROKER_ENABLED_DEFAULT = false;

	protected static final String BROKER_FACTORY_URI = "brokerFactoryUri";

	protected static final String BROKER_FACTORY_URI_DEFAULT = "broker:(tcp://localhost:61616)?persistent=false&brokerName=sling-activemq-broker";

	private BrokerService broker;

	@Activate
	protected void activate(Map<String, Object> config) throws Exception {
		boolean enabled = PropertiesUtil.toBoolean(config.get(EMBEDDED_BROKER_ENABLED),
				EMBEDDED_BROKER_ENABLED_DEFAULT);
		String uri = PropertiesUtil.toString(config.get(BROKER_FACTORY_URI), BROKER_FACTORY_URI_DEFAULT);
		if (enabled) {
			BrokerService broker = BrokerFactory.createBroker(uri);
			broker.start();
		}
	}

	@Deactivate
	protected void deactivate() throws Exception {
		if (broker != null) {
			broker.stop();
		}
	}
}