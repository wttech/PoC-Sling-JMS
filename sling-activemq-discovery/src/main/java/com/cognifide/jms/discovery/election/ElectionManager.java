package com.cognifide.jms.discovery.election;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.TopologyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.discovery.JmsAccessObject;
import com.cognifide.jms.discovery.RefreshType;
import com.cognifide.jms.discovery.update.UpdatesManager;

public class ElectionManager {

	private static final Logger LOG = LoggerFactory.getLogger(ElectionManager.class);

	private final Map<String, Election> elections;

	private final String localSlingId;

	private final String localClusterId;

	private final UpdatesManager updateMessageStorage;

	private final JmsAccessObject jms;

	public ElectionManager(String localSlingId, String localClusterId,
			UpdatesManager updateMessageStorage, JmsAccessObject jms) {
		this.elections = new HashMap<String, Election>();
		this.localSlingId = localSlingId;
		this.localClusterId = localClusterId;
		this.updateMessageStorage = updateMessageStorage;
		this.jms = jms;
	}

	public RefreshType handleElectionRequest(ElectionRequest remoteReq) throws JMSException {
		boolean topologyUpdated = false;

		String clusterId = remoteReq.getClusterId();
		String slingId = remoteReq.getSlingId();
		ElectionRequestType type = remoteReq.getType();
		if (localClusterId.equals(clusterId)) {
			if (type == ElectionRequestType.WHO_IS_LEADER
					&& localSlingId.equals(updateMessageStorage.getLeader(clusterId))) {
				jms.sendElectionReq(ElectionRequestType.I_AM_LEADER, localClusterId, localSlingId);
			} else if (type == ElectionRequestType.ELECTION) {
				jms.sendElectionReq(ElectionRequestType.VOTE, localClusterId, localSlingId);
			}
		}

		if (type == ElectionRequestType.I_AM_LEADER) {
			updateMessageStorage.setLeader(clusterId, slingId);
			Election e = elections.remove(clusterId);
			if (e != null) {
				e.shutDown();
			}
			LOG.info("Got new leader " + slingId + " for cluster " + clusterId + ", refreshing topology view");
			topologyUpdated = true;
		}
		for (Election e : elections.values()) {
			e.gotRequest(remoteReq);
		}
		return topologyUpdated ? RefreshType.REFRESH_TOPOLOGY : RefreshType.NO_REFRESH;
	}

	public boolean electIfNecessary(TopologyView view) throws JMSException {
		boolean isElectionNeeded = false;
		LOG.info("Checking if topology is stable");
		for (ClusterView cluster : view.getClusterViews()) {
			if (cluster.getLeader() == null) {
				isElectionNeeded = true;
				if (updateMessageStorage.getLeader(cluster.getId()) != null) {
					LOG.info("Leader for " + cluster.getId() + " hasn't sent his heartbeat yet.");
				} else {
					LOG.info("Cluster " + cluster.getId() + " has no leader");
					if (!elections.containsKey(cluster.getId())) {
						Election e = new Election(cluster.getId(), localSlingId, jms);
						elections.put(cluster.getId(), e);
						e.getLeader();
					}
				}
			}
		}
		return isElectionNeeded;
	}

	public void stop() {
		for (Election e : elections.values()) {
			e.shutDown();
		}
	}
}
