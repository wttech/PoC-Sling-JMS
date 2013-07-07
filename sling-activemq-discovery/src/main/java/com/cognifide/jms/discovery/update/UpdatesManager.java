package com.cognifide.jms.discovery.update;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.sling.discovery.TopologyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.discovery.RefreshType;
import com.cognifide.jms.discovery.model.SimpleInstanceDescription;
import com.cognifide.jms.discovery.model.SimpleTopologyView;

public class UpdatesManager {

	private static final Logger LOG = LoggerFactory.getLogger(UpdatesManager.class);

	private final String localSlingId;

	private final int ttl;

	private final Map<String, UpdateMessage> updates;

	private final Map<String, Calendar> updateTimes;

	private final Map<String, String> clusterLeaders;

	public UpdatesManager(String localSlingId, int ttl) {
		this.localSlingId = localSlingId;
		this.ttl = ttl;
		updates = new LinkedHashMap<String, UpdateMessage>();
		updateTimes = new LinkedHashMap<String, Calendar>();
		clusterLeaders = new HashMap<String, String>();
	}

	public RefreshType handleTopologyUpdateMsg(UpdateMessage updateMsg) throws JMSException {
		LOG.info("Got heartbeat message from " + updateMsg.getSlingId());
		String slingId = updateMsg.getSlingId();
		updateTimes.put(slingId, Calendar.getInstance());
		if (updateMsg.equals(updates.get(slingId))) {
			return RefreshType.NO_REFRESH;
		}

		RefreshType refreshType = RefreshType.REFRESH_TOPOLOGY;
		UpdateMessage previousMsg = updates.put(slingId, updateMsg);
		if (previousMsg == null) {
			refreshType = RefreshType.REFRESH_PROPERTIES;
		} else if (!previousMsg.getProperties().equals(updateMsg.getProperties())) {
			refreshType = RefreshType.REFRESH_PROPERTIES;
		}
		LOG.info("Got new heartbeat, refreshing topology view");
		return refreshType;
	}

	public RefreshType checkInstanceTtl() throws JMSException {
		Calendar notBefore = Calendar.getInstance();
		notBefore.add(Calendar.SECOND, -ttl);
		List<String> toRemove = new ArrayList<String>();
		for (String slingId : updates.keySet()) {
			Calendar lastUpdate = updateTimes.get(slingId);
			if (lastUpdate.before(notBefore)) {
				toRemove.add(slingId);
			}
		}
		for (String slingId : toRemove) {
			updates.remove(slingId);
			updateTimes.remove(slingId);
		}
		return toRemove.isEmpty() ? RefreshType.NO_REFRESH : RefreshType.REFRESH_TOPOLOGY;
	}

	public TopologyView generateView() {
		List<SimpleInstanceDescription> instances = new ArrayList<SimpleInstanceDescription>();
		for (UpdateMessage msg : updates.values()) {
			boolean isLeader = msg.getSlingId().equals(clusterLeaders.get(msg.getClusterId()));
			boolean isLocal = localSlingId.equals(msg.getSlingId());
			instances.add(new SimpleInstanceDescription(msg, isLeader, isLocal));
		}
		return new SimpleTopologyView(instances);
	}

	public String getLeader(String clusterId) {
		return clusterLeaders.get(clusterId);
	}

	public void setLeader(String clusterId, String slingId) {
		clusterLeaders.put(clusterId, slingId);
	}

}
