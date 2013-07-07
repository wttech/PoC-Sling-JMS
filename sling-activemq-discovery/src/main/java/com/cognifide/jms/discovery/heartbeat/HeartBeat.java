package com.cognifide.jms.discovery.heartbeat;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.impl.QuartzScheduler;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.discovery.JmsAccessObject;
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

	@Reference
	private JmsConnectionProvider connectionProvider;

	@Reference
	private SlingSettingsService slingSettingsService;

	private String slingId;

	private String clusterId;

	private BundleContext bundleContext;

	private JmsAccessObject jms;

	@Activate
	protected void activate(ComponentContext context) throws JMSException {
		this.bundleContext = context.getBundleContext();
		this.slingId = slingSettingsService.getSlingId();
		Dictionary<?, ?> config = context.getProperties();
		this.clusterId = PropertiesUtil.toString(config.get(CLUSTER_ID_PROPERTY_NAME),
				CLUSTER_ID_PROPERTY_DEFAULT);
		jms = new JmsAccessObject(connectionProvider);
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
		ServiceReference[] references = bundleContext.getAllServiceReferences(
				PropertyProvider.class.getName(), null);
		for (ServiceReference r : references) {
			String[] propNames = PropertiesUtil.toStringArray(r
					.getProperty(PropertyProvider.PROPERTY_PROPERTIES));
			if (ArrayUtils.isEmpty(propNames)) {
				continue;
			}
			PropertyProvider provider = (PropertyProvider) bundleContext.getService(r);
			for (String name : propNames) {
				properties.put(name, provider.getProperty(name));
			}
		}
		return properties;
	}

	public String getLocalSlingId() {
		return slingId;
	}

	public String getLocalClusterId() {
		return clusterId;
	}
}
