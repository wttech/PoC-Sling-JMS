package com.cognifide.jms.session;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConnectionProvider;
import com.cognifide.jms.session.model.SessionDiff;
import com.cognifide.jms.session.model.SharedSession;
import com.cognifide.jms.session.model.SharedSessionCookie;

@Component(immediate = true, metatype = true)
@SlingFilter(order = Integer.MIN_VALUE, scope = { SlingFilterScope.REQUEST }, generateComponent = false)
@Properties({ @Property(name = SharedSessionFilter.ATTRIBUTE_PATTERN_PROPERTY, value = { ".*" }) })
public class SharedSessionFilter implements Filter {

	private static final Logger LOG = LoggerFactory.getLogger(SharedSessionFilter.class);

	protected static final String ATTRIBUTE_PATTERN_PROPERTY = "attributePattern";

	@Reference
	private SharedSessionStorage storage;

	private String instanceId;

	@Reference
	private JmsConnectionProvider connectionProvider;

	private Connection connection;

	private Session session;

	private MessageProducer producer;

	private String[] attributePatterns;

	@Activate
	protected void activate(Map<String, Object> configuration) throws JMSException {
		this.instanceId = UUID.randomUUID().toString();
		this.attributePatterns = PropertiesUtil.toStringArray(configuration.get(ATTRIBUTE_PATTERN_PROPERTY));

		connection = connectionProvider.getConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination dest = session.createTopic(SharedSessionStorage.TOPIC);
		producer = session.createProducer(dest);
		connection.start();
	}

	@Deactivate
	protected void deactivate() throws JMSException {
		producer.close();
		session.close();
		connection.close();
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof SlingHttpServletRequest && response instanceof SlingHttpServletResponse) {
			SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
			SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
			handleRequest(slingRequest, slingResponse, chain);
		} else {
			chain.doFilter(request, response);
		}
	}

	private void handleRequest(SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse,
			FilterChain chain) throws IOException, ServletException {
		SharedSessionCookie cookie = new SharedSessionCookie(slingRequest, slingResponse);
		if (!cookie.exists()) {
			LOG.info("There is no shared session cookie, creating one");
			String sharedSessionId = UUID.randomUUID().toString();
			cookie = cookie.set(instanceId, sharedSessionId);
		}

		SharedSession sharedSession = storage.getSharedSession(cookie.getSharedSessionId());
		if (!instanceId.equals(cookie.getInstanceId())) {
			LOG.info("Host in shared session cookie doesn't match, updating local session");
			sharedSession.copyTo(slingRequest.getSession(), attributePatterns);
			cookie.set(instanceId, cookie.getSharedSessionId());
		}

		WrappedSlingRequest wrapped = new WrappedSlingRequest(slingRequest);
		chain.doFilter(wrapped, slingResponse);

		broadcastChanges(slingRequest, sharedSession, wrapped);
	}

	private synchronized void broadcastChanges(SlingHttpServletRequest slingRequest,
			SharedSession sharedSession, WrappedSlingRequest wrapped) {
		try {
			SessionDiff diff = sharedSession.getDiff(slingRequest.getSession(false));
			if (diff != null) {
				LOG.info("Sending diff");
				ObjectMessage objectMsg = session.createObjectMessage(diff);
				objectMsg.setStringProperty(SharedSessionStorage.ACTION, Action.UPDATE.name());
				objectMsg.setStringProperty(SharedSessionStorage.SHARED_SESSION_ID, sharedSession.getId());
				producer.send(objectMsg);
			}
			if (wrapped.sessionHasBeenUsed()) {
				LOG.info("Sending refresh");
				Message refreshMsg = session.createMessage();
				refreshMsg.setStringProperty(SharedSessionStorage.ACTION, Action.REFRESH.name());
				refreshMsg.setStringProperty(SharedSessionStorage.SHARED_SESSION_ID, sharedSession.getId());
				producer.send(refreshMsg);
			}
		} catch (JMSException e) {
			LOG.error("Can't send session changes", e);
		}
	}
}
