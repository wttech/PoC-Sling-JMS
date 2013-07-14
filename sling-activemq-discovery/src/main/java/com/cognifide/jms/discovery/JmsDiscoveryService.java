package com.cognifide.jms.discovery;

import java.util.Collections;
import java.util.Dictionary;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyView;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.api.JmsConstants;
import com.cognifide.jms.api.ObjectMessageUtils;
import com.cognifide.jms.discovery.election.ElectionManager;
import com.cognifide.jms.discovery.election.ElectionRequest;
import com.cognifide.jms.discovery.heartbeat.HeartBeat;
import com.cognifide.jms.discovery.model.SimpleInstanceDescription;
import com.cognifide.jms.discovery.model.SimpleTopologyView;
import com.cognifide.jms.discovery.update.UpdateMessage;
import com.cognifide.jms.discovery.update.UpdatesManager;

@Component(immediate = true, metatype = true)
@Service(value = { MessageListener.class, DiscoveryService.class })
@Properties({
		@Property(name = JmsConstants.CONSUMER_SUBJECT, value = DiscoveryJmsProducer.TOPIC, propertyPrivate = true),
		@Property(name = JmsConstants.CONSUMER_TYPE, value = JmsConstants.TYPE_TOPIC, propertyPrivate = true),
		@Property(name = JmsDiscoveryService.TTL_PROPERTY_NAME, intValue = JmsDiscoveryService.TTL_DEFAULT) })
public class JmsDiscoveryService implements MessageListener, DiscoveryService {

	protected static final String TTL_PROPERTY_NAME = "ttl";

	protected static final int TTL_DEFAULT = 45;

	private static final Logger LOG = LoggerFactory.getLogger(JmsDiscoveryService.class);

	private static final TopologyView EMPTY_VIEW = new SimpleTopologyView(
			Collections.<SimpleInstanceDescription> emptySet());

	@Reference
	private JmsConnectionProvider connectionProvider;

	@Reference
	private TopologyEventListenerRegistry eventManager;

	@Reference
	private HeartBeat heartBeat;

	private DiscoveryJmsProducer jms;

	private UpdatesManager updateMessageStorage;

	private ElectionManager electionManager;

	private volatile TopologyView view;

	private boolean sentChanging;

	private ScheduledExecutorService service;

	@Activate
	public void activate(ComponentContext context) throws JMSException {
		Dictionary<?, ?> config = context.getProperties();
		int ttl = PropertiesUtil.toInteger(config.get(TTL_PROPERTY_NAME), TTL_DEFAULT);

		String localSlingId = heartBeat.getLocalSlingId();
		String localClusterId = heartBeat.getLocalClusterId();
		this.jms = new DiscoveryJmsProducer(connectionProvider);
		this.updateMessageStorage = new UpdatesManager(localSlingId, ttl);
		this.electionManager = new ElectionManager(localSlingId, localClusterId, updateMessageStorage, jms);
		view = EMPTY_VIEW;

		service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				removeDeadInstances();
			}
		}, 5, 5, TimeUnit.SECONDS);
	}

	@Deactivate
	public void deactivate() throws JMSException {
		electionManager.stop();
		jms.close();
	}

	@Override
	public void onMessage(Message message) {
		try {
			Object object = ObjectMessageUtils.getObjectInContext((ObjectMessage) message,
					UpdateMessage.class);
			RefreshType refreshType = RefreshType.NO_REFRESH;
			if (object instanceof ElectionRequest) {
				refreshType = electionManager.handleElectionRequest((ElectionRequest) object);
			} else if (object instanceof UpdateMessage) {
				refreshType = updateMessageStorage.handleTopologyUpdateMsg((UpdateMessage) object);
			} else {
				LOG.error("Wrong type of object: ", object);
			}
			refreshTopologyView(refreshType);
		} catch (JMSException e) {
			LOG.error("Can't handle message", e);
		}
	}

	@Override
	public TopologyView getTopology() {
		return view;
	}

	private void removeDeadInstances() {
		try {
			RefreshType refreshType = updateMessageStorage.checkInstanceTtl();
			refreshTopologyView(refreshType);
		} catch (JMSException e) {
			LOG.error("Can't remove dead instances");
		}
	}

	private synchronized void refreshTopologyView(RefreshType refreshType) throws JMSException {
		if (refreshType == RefreshType.NO_REFRESH) {
			return;
		}
		if (!sentChanging) {
			sentChanging = true;
			eventManager.post(Type.TOPOLOGY_CHANGING, view, null);
		}
		TopologyView oldView = view;
		view = updateMessageStorage.generateView();
		if (!electionManager.electIfNecessary(view)) {
			eventManager.post(Type.TOPOLOGY_CHANGED, oldView, view);
			sentChanging = false;
			if (refreshType == RefreshType.REFRESH_PROPERTIES) {
				eventManager.post(Type.PROPERTIES_CHANGED, oldView, view);
			}
		}
	}

}
