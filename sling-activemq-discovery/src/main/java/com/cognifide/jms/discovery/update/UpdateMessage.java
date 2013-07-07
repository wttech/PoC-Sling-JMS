package com.cognifide.jms.discovery.update;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class UpdateMessage implements Serializable {

	private static final long serialVersionUID = -5664640936079982675L;

	private final String clusterId;

	private final String slingId;

	private final Map<String, String> properties;

	public UpdateMessage(String clusterId, String slingId, Map<String, String> properties) {
		this.clusterId = clusterId;
		this.slingId = slingId;
		this.properties = properties;
	}

	public String getClusterId() {
		return clusterId;
	}

	public String getSlingId() {
		return slingId;
	}

	public Map<String, String> getProperties() {
		return properties;
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
		UpdateMessage rhs = (UpdateMessage) obj;
		return new EqualsBuilder().append(clusterId, rhs.clusterId).append(slingId, rhs.slingId)
				.append(properties, rhs.properties).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(clusterId).append(slingId).append(properties).toHashCode();
	}

}
