package com.cognifide.jms.sandbox;

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;

@SlingServlet(paths = "/bin/jms/discovery/info", generateComponent = true, generateService = true)
public class InfoServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = -1903359993395642416L;

	@Reference
	private DiscoveryService discoveryService;

	@Override
	public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		response.getWriter().write(showTopology(discoveryService.getTopology()));
	}

	private String showTopology(TopologyView view) {
		StringBuilder builder = new StringBuilder();
		for (ClusterView cluster : view.getClusterViews()) {
			builder.append(showClusterView(cluster));
		}
		return builder.toString();
	}

	public String showClusterView(ClusterView view) {
		StringBuilder builder = new StringBuilder();
		builder.append("### Cluster ").append(view.getId()).append(" ###\n");
		for (InstanceDescription desc : view.getInstances()) {
			builder.append(showInstance(desc)).append("\n");
		}
		return builder.toString();
	}

	private String showInstance(InstanceDescription desc) {
		StringBuilder builder = new StringBuilder();
		builder.append("Sling id:   ").append(desc.getSlingId()).append('\n');
		builder.append("Is leader:  ").append(desc.isLeader()).append('\n');
		builder.append("Is local:   ").append(desc.isLocal()).append('\n');
		builder.append("Properties:\n");
		for (Entry<String, String> e : desc.getProperties().entrySet()) {
			builder.append(" * ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
		}
		return builder.toString();
	}
}
