package com.cognifide.jms.discovery.model;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

import com.cognifide.jms.discovery.update.UpdateMessage;

public class SimpleInstanceDescription implements InstanceDescription {

	private final String clusterId;

	private final Map<String, String> properties;

	private final String slingId;

	private final boolean isLeader;

	private final boolean isLocal;

	private ClusterView clusterView;

	public SimpleInstanceDescription(UpdateMessage updateMsg, boolean isLeader, boolean isLocal) {
		this.clusterId = updateMsg.getClusterId();
		this.properties = updateMsg.getProperties();
		this.slingId = updateMsg.getSlingId();
		this.isLeader = isLeader;
		this.isLocal = isLocal;
	}

	String getClusterId() {
		return clusterId;
	}

	void setClusterView(ClusterView view) {
		this.clusterView = view;
	}

	@Override
	public ClusterView getClusterView() {
		return clusterView;
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public String getProperty(String key) {
		return properties.get(key);
	}

	@Override
	public String getSlingId() {
		return slingId;
	}

	@Override
	public boolean isLeader() {
		return isLeader;
	}

	@Override
	public boolean isLocal() {
		return isLocal;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		SimpleInstanceDescription rhs = (SimpleInstanceDescription) obj;
		return new EqualsBuilder().append(clusterId, rhs.clusterId).append(isLeader, rhs.isLeader)
				.append(isLocal, rhs.isLocal).append(properties, rhs.properties).append(slingId, rhs.slingId)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(clusterId).append(isLeader).append(isLocal)
				.append(properties).append(slingId).toHashCode();
	}

	public String getInfo() {
		StringBuilder builder = new StringBuilder();
		builder.append("Sling id:   ").append(slingId).append('\n');
		builder.append("Is leader:  ").append(isLeader).append('\n');
		builder.append("Is local:   ").append(isLocal).append('\n');
		builder.append("Properties:\n");
		for (Entry<String, String> e : properties.entrySet()) {
			builder.append(" * ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
		}
		return builder.toString();
	}

}
