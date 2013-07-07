package com.cognifide.jms.discovery.election;

import java.io.Serializable;

public class ElectionRequest implements Serializable {

	private static final long serialVersionUID = 7626809651139358313L;

	private final ElectionRequestType type;

	private final String clusterId;

	private final String slingId;

	public ElectionRequest(ElectionRequestType type, String clusterId, String slingId) {
		this.type = type;
		this.clusterId = clusterId;
		this.slingId = slingId;
	}

	public ElectionRequestType getType() {
		return type;
	}

	public String getClusterId() {
		return clusterId;
	}

	public String getSlingId() {
		return slingId;
	}

}
