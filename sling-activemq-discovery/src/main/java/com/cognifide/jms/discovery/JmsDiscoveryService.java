package com.cognifide.jms.discovery;

import java.util.Dictionary;
import java.util.concurrent.Executors;

import javax.jms.JMSException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.TopologyView;
import org.osgi.service.component.ComponentContext;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.discovery.heartbeat.HeartBeat;

@Component(immediate = true, metatype = true)
@Service(value = { JmsDiscoveryService.class, DiscoveryService.class })
@Properties({ @Property(name = JmsDiscoveryService.TTL_PROPERTY_NAME, intValue = JmsDiscoveryService.TTL_DEFAULT) })
public class JmsDiscoveryService implements DiscoveryService {

	protected static final String TTL_PROPERTY_NAME = "ttl";

	protected static final int TTL_DEFAULT = 45;

	public static final String TOPIC = JmsDiscoveryService.class.getName();

	@Reference
	private TopologyEventListenerRegistry eventManager;

	@Reference
	private JmsConnectionProvider connectionProvider;

	@Reference
	private HeartBeat heartBeat;

	private ToplogyUpdateProcess runnable;

	protected void activate(ComponentContext context) throws JMSException {
		Dictionary<?, ?> config = context.getProperties();
		int ttl = PropertiesUtil.toInteger(config.get(TTL_PROPERTY_NAME), TTL_DEFAULT);
		runnable = new ToplogyUpdateProcess(connectionProvider, TOPIC, heartBeat.getLocalSlingId(),
				heartBeat.getLocalClusterId(), eventManager, ttl);
		Executors.newSingleThreadExecutor().execute(runnable);
	}

	protected void deactivate() {
		runnable.stop();
	}

	@Override
	public TopologyView getTopology() {
		return runnable.getView();
	}
}
