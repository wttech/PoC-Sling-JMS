package com.cognifide.jms.discovery;

import java.util.Collections;

import javax.jms.JMSException;

import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.discovery.election.ElectionManager;
import com.cognifide.jms.discovery.election.ElectionRequest;
import com.cognifide.jms.discovery.model.SimpleInstanceDescription;
import com.cognifide.jms.discovery.model.SimpleTopologyView;
import com.cognifide.jms.discovery.update.UpdateMessage;
import com.cognifide.jms.discovery.update.UpdatesManager;

public class ToplogyUpdateProcess implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ToplogyUpdateProcess.class);

	private static final TopologyView EMPTY_VIEW = new SimpleTopologyView(
			Collections.<SimpleInstanceDescription> emptySet());

	private final JmsAccessObject jms;

	private final TopologyEventListenerRegistry eventManager;

	private final UpdatesManager updateMessageStorage;

	private final ElectionManager electionManager;

	private volatile TopologyView view;

	private volatile boolean stopThread;

	private boolean sentChanging;

	public ToplogyUpdateProcess(JmsConnectionProvider connectionProvider, String topic, String localSlingId,
			String localClusterId, TopologyEventListenerRegistry eventManager, int ttl) throws JMSException {
		this.jms = new JmsAccessObject(connectionProvider);
		this.eventManager = eventManager;
		this.updateMessageStorage = new UpdatesManager(localSlingId, ttl);
		this.electionManager = new ElectionManager(localSlingId, localClusterId, updateMessageStorage, jms);
		view = EMPTY_VIEW;
		// eventManager.post(Type.TOPOLOGY_INIT, null, view);
	}

	@Override
	public void run() {
		stopThread = false;
		while (!stopThread) {
			try {
				refreshTopologyView(updateMessageStorage.checkInstanceTtl());
				processMessage();
			} catch (JMSException e) {
				stopThread = true;
				LOG.error("Can't receive message", e);
			}
		}
		try {
			jms.close();
		} catch (JMSException e) {
			LOG.error("Can't close connection", e);
		}
	}

	public void stop() {
		stopThread = true;
		electionManager.stop();
	}

	public TopologyView getView() {
		return view;
	}

	private void processMessage() throws JMSException {
		Object object = jms.getObjectMsg(1000);
		RefreshType refreshType = RefreshType.NO_REFRESH;
		if (object == null) {
			return;
		} else if (object instanceof ElectionRequest) {
			refreshType = electionManager.handleElectionRequest((ElectionRequest) object);
		} else if (object instanceof UpdateMessage) {
			refreshType = updateMessageStorage.handleTopologyUpdateMsg((UpdateMessage) object);
		} else {
			LOG.error("Wrong type of object: ", object);
		}
		refreshTopologyView(refreshType);
	}

	private void refreshTopologyView(RefreshType refreshType) throws JMSException {
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
