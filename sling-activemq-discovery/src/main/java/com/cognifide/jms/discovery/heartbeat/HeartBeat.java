package com.cognifide.jms.discovery.heartbeat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.scheduler.impl.QuartzScheduler;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.discovery.DiscoveryJmsProducer;
import com.cognifide.jms.discovery.update.UpdateMessage;

@Component(immediate = true, metatype = true)
@Service(value = { Runnable.class, HeartBeat.class })
@Properties({
		@Property(name = QuartzScheduler.PROPERTY_SCHEDULER_IMMEDIATE, boolValue = true, propertyPrivate = true),
		@Property(name = QuartzScheduler.PROPERTY_SCHEDULER_PERIOD, longValue = 30, label = "Update period", propertyPrivate = false),
		@Property(name = QuartzScheduler.PROPERTY_SCHEDULER_CONCURRENT, boolValue = false, propertyPrivate = true),
		@Property(name = HeartBeat.CLUSTER_ID_PROPERTY_NAME, value = HeartBeat.CLUSTER_ID_PROPERTY_DEFAULT, label = "Cluster id") })
public class HeartBeat implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(HeartBeat.class);

	protected static final String CLUSTER_ID_PROPERTY_NAME = "clusterId";

	protected static final String CLUSTER_ID_PROPERTY_DEFAULT = "default";

	@Reference(referenceInterface = PropertyProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private Map<PropertyProvider, ProviderWithNames> providers = new HashMap<PropertyProvider, ProviderWithNames>();

	@Reference
	private JmsConnectionProvider connectionProvider;

	@Reference
	private SlingSettingsService slingSettingsService;

	private String slingId;

	private String clusterId;

	private DiscoveryJmsProducer jms;

	@Activate
	protected void activate(Map<String, Object> config) throws JMSException {
		this.slingId = slingSettingsService.getSlingId();
		this.clusterId = (String) config.get(CLUSTER_ID_PROPERTY_NAME);
		jms = new DiscoveryJmsProducer(connectionProvider);
	}

	protected void deactivate() throws JMSException {
		jms.close();
	}

	@Override
	public void run() {
		LOG.info("Sending heartbeat");
		Map<String, String> properties;
		try {
			properties = gatherProperties();
		} catch (InvalidSyntaxException e) {
			properties = Collections.emptyMap();
			LOG.error("Can't gather properties", e);
		}
		UpdateMessage msg = new UpdateMessage(clusterId, slingId, properties);
		try {
			jms.sendObjectMsg(msg);
		} catch (JMSException e) {
			LOG.error("Can't send heartbeat", e);
		}
	}

	private Map<String, String> gatherProperties() throws InvalidSyntaxException {
		Map<String, String> properties = new HashMap<String, String>();
		for (ProviderWithNames p : providers.values()) {
			properties.putAll(p.getProperties());
		}
		return properties;
	}

	public String getLocalSlingId() {
		return slingId;
	}

	public String getLocalClusterId() {
		return clusterId;
	}

	public void bindProviders(PropertyProvider provider, Map<String, Object> config) {
		providers.put(provider,
				new ProviderWithNames(provider, (String[]) config.get(PropertyProvider.PROPERTY_PROPERTIES)));
	}

	public void unbindProviders(PropertyProvider provider) {
		providers.remove(provider);
	}

	private static final class ProviderWithNames {
		private PropertyProvider provider;

		private String[] names;

		public ProviderWithNames(PropertyProvider provider, String[] names) {
			this.provider = provider;
			this.names = names;
		}

		public Map<String, String> getProperties() {
			Map<String, String> map = new HashMap<String, String>();
			if (ArrayUtils.isNotEmpty(names)) {
				for (String name : names) {
					map.put(name, provider.getProperty(name));
				}
			}
			return map;
		}
	}
}
