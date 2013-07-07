package com.cognifide.jms.discovery.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyView;

public class SimpleTopologyView implements TopologyView {

	private final Set<ClusterView> clusterViews;

	private final Set<InstanceDescription> instances;

	private final InstanceDescription local;

	public SimpleTopologyView(Collection<SimpleInstanceDescription> instances) {
		this.instances = createInstanceSet(instances);
		this.local = findLocal(instances);
		this.clusterViews = createClusterViews(instances);
	}

	@Override
	public Set<InstanceDescription> findInstances(InstanceFilter filter) {
		Set<InstanceDescription> result = new LinkedHashSet<InstanceDescription>();
		for (InstanceDescription desc : instances) {
			if (filter.accept(desc)) {
				result.add(desc);
			}
		}
		return result;
	}

	@Override
	public Set<ClusterView> getClusterViews() {
		return clusterViews;
	}

	@Override
	public Set<InstanceDescription> getInstances() {
		return instances;
	}

	@Override
	public InstanceDescription getLocalInstance() {
		return local;
	}

	@Override
	public boolean isCurrent() {
		return true;
	}

	private Set<InstanceDescription> createInstanceSet(Collection<SimpleInstanceDescription> instances) {
		Set<InstanceDescription> instanceSet = new LinkedHashSet<InstanceDescription>();
		instanceSet.addAll(instances);
		return Collections.unmodifiableSet(instanceSet);
	}

	private InstanceDescription findLocal(Collection<SimpleInstanceDescription> instances) {
		InstanceDescription localInstance = null;
		for (InstanceDescription desc : instances) {
			if (desc.isLocal()) {
				localInstance = desc;
				break;
			}
		}
		return localInstance;
	}

	private Set<ClusterView> createClusterViews(Collection<SimpleInstanceDescription> instances) {
		Map<String, List<SimpleInstanceDescription>> clusters = new LinkedHashMap<String, List<SimpleInstanceDescription>>();
		for (SimpleInstanceDescription desc : instances) {
			String clusterId = desc.getClusterId();
			if (!clusters.containsKey(clusterId)) {
				clusters.put(clusterId, new ArrayList<SimpleInstanceDescription>());
			}
			List<SimpleInstanceDescription> list = clusters.get(clusterId);
			list.add(desc);
		}

		Set<ClusterView> clusterViews = new LinkedHashSet<ClusterView>();
		for (Entry<String, List<SimpleInstanceDescription>> entry : clusters.entrySet()) {
			List<InstanceDescription> newList = new ArrayList<InstanceDescription>();
			for (SimpleInstanceDescription desc : entry.getValue()) {
				newList.add(desc);
			}
			ClusterView view = new SimpleClusterView(entry.getKey(), newList);
			clusterViews.add(view);
			for (SimpleInstanceDescription desc : entry.getValue()) {
				desc.setClusterView(view);
			}
		}
		return Collections.unmodifiableSet(clusterViews);
	}

	public String getInfo() {
		StringBuilder builder = new StringBuilder();
		for (ClusterView cluster : clusterViews) {
			builder.append(((SimpleClusterView) cluster).getInfo());
		}
		return builder.toString();
	}
}
