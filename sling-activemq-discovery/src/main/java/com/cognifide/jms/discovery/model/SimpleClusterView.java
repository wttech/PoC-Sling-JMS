package com.cognifide.jms.discovery.model;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

public class SimpleClusterView implements ClusterView {

	private final String id;

	private final List<InstanceDescription> instances;

	private final InstanceDescription leader;

	public SimpleClusterView(String id, List<InstanceDescription> instances) {
		this.id = id;
		this.instances = Collections.unmodifiableList(instances);
		InstanceDescription leader = null;
		for (InstanceDescription desc : instances) {
			if (desc.isLeader()) {
				leader = desc;
				break;
			}
		}
		this.leader = leader;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public List<InstanceDescription> getInstances() {
		return instances;
	}

	@Override
	public InstanceDescription getLeader() {
		return leader;
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
		SimpleClusterView rhs = (SimpleClusterView) obj;
		return new EqualsBuilder().append(id, rhs.id).append(instances, rhs.instances)
				.append(leader, rhs.leader).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(id).append(instances).append(leader).toHashCode();
	}
}
