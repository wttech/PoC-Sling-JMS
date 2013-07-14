package com.cognifide.jms.discovery;

import java.io.IOException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.discovery.DiscoveryService;

import com.cognifide.jms.discovery.model.SimpleTopologyView;

@SlingServlet(paths = "/bin/jms/discovery/info", generateComponent = true, generateService = true)
public class InfoServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = -1903359993395642416L;

	@Reference
	private DiscoveryService discoveryService;

	@Override
	public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		response.getWriter().write(((SimpleTopologyView) discoveryService.getTopology()).getInfo());
	}
}
