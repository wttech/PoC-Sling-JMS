package com.cognifide.activemq.blob;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

@Component(immediate = true, metatype = true)
@Service(value = { SlingBlobServlet.class, Servlet.class })
@Properties({ @Property(name = "sling.servlet.paths", value = "/bin/jms/blob", propertyPrivate = true),
		@Property(name = "sling.servlet.extensions", value = "bin", propertyPrivate = true),
		@Property(name = SlingBlobServlet.EXTERNAL_URL_NAME, value = SlingBlobServlet.EXTERNAL_URL_DEFAULT) })
public class SlingBlobServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = -8867615561279547594L;

	protected static final String EXTERNAL_URL_NAME = "externalUrl";

	protected static final String EXTERNAL_URL_DEFAULT = "http://localhost:4502/bin/jms/blob.bin";

	private Map<String, String> mappings;

	private ResourceResolver adminResolver;

	private String externalUrl;

	@Reference
	private ResourceResolverFactory resolverFactory;

	@Activate
	protected void activate(ComponentContext ctx) throws LoginException {
		Dictionary<?, ?> config = ctx.getProperties();
		externalUrl = PropertiesUtil.toString(config.get(EXTERNAL_URL_NAME), EXTERNAL_URL_DEFAULT);

		mappings = new HashMap<String, String>();
		adminResolver = resolverFactory.getAdministrativeResourceResolver(null);
	}

	@Deactivate
	protected void deactivate() {
		adminResolver.close();
	}

	@Override
	public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		String uuid = request.getRequestPathInfo().getSuffix().substring(1);
		String path = mappings.get(uuid);
		if (path == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		try {
			javax.jcr.Property p = getProperty(path);
			InputStream is = p.getBinary().getStream();
			IOUtils.copy(is, response.getOutputStream());
		} catch (RepositoryException e) {
			throw new IOException(e);
		}
	}

	public void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws IOException {
		String uuid = request.getRequestPathInfo().getSuffix().substring(1);
		String path = mappings.remove(uuid);
		if (path == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	public String addMapping(String path) throws IOException {
		getProperty(path);
		String uuid;
		do {
			uuid = UUID.randomUUID().toString();
		} while (mappings.containsKey(uuid));
		mappings.put(uuid, path);
		return externalUrl + "/" + uuid;
	}

	private javax.jcr.Property getProperty(String path) throws IOException {
		Session session = adminResolver.adaptTo(Session.class);
		String nodePath = StringUtils.substringBeforeLast(path, "/");
		String propertyName = StringUtils.substringAfterLast(path, "/");

		try {
			if (!session.nodeExists(nodePath)) {
				throw new FileNotFoundException(nodePath);
			}
			Node node = session.getNode(nodePath);
			if (!node.hasProperty(propertyName)) {
				throw new FileNotFoundException(path);
			}
			javax.jcr.Property property = node.getProperty(propertyName);
			return property;
		} catch (RepositoryException e) {
			throw new IOException(e);
		}
	}
}
