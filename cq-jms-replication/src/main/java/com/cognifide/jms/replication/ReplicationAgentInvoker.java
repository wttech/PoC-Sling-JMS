package com.cognifide.jms.replication;

import java.lang.reflect.Method;

import javax.jcr.Session;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.consumer.MessageConsumerProperties;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;

@Component(immediate = true, metatype = false)
@Service
@Properties({ @Property(name = MessageConsumerProperties.CONSUMER_SUBJECT, value = OutboxEventHandler.TOPIC),
		@Property(name = MessageConsumerProperties.DESTINATION_TYPE, value = MessageConsumerProperties.TYPE_TOPIC) })
public class ReplicationAgentInvoker implements MessageListener {

	private static final Logger LOG = LoggerFactory.getLogger(ReplicationAgentInvoker.class);

	@Reference
	private SlingSettingsService slingSettings;

	@Reference
	private AgentManager agentManager;

	@Reference
	private ResourceResolverFactory resolverFactory;

	private Object reverseReplicationHandler;

	@Activate
	public void activate(ComponentContext context) {
		BundleContext bundleCtx = context.getBundleContext();
		ServiceReference reference = bundleCtx
				.getServiceReference("com.day.cq.replication.impl.ReverseReplicationHandler");
		reverseReplicationHandler = bundleCtx.getService(reference);
	}

	@Override
	public void onMessage(Message message) {
		try {
			MapMessage map = (MapMessage) message;
			String agentId = map.getString("agentId");
			poll(agentId);
		} catch (JMSException e) {
			LOG.error("Can't read message", e);
		} catch (ReplicationException e) {
			LOG.error("Can't replicate", e);
		} catch (LoginException e) {
			LOG.error("Can't get resource resolver", e);
		}
	}

	private void poll(String agentId) throws ReplicationException, LoginException {
		Agent agent = agentManager.getAgents().get(agentId);
		if (agent == null) {
			LOG.error("No such agent: " + agentId);
			return;
		}
		ResourceResolver resolver = resolverFactory.getAdministrativeResourceResolver(null);
		try {
			Session session = resolver.adaptTo(Session.class);
			ReplicationAction action = new ReplicationAction(ReplicationActionType.INTERNAL_POLL, "");
			Method method = reverseReplicationHandler.getClass().getMethod("poll", Agent.class,
					Session.class, ReplicationAction.class);
			method.invoke(reverseReplicationHandler, agent, session, action);
		} catch (Exception e) {
			LOG.error("Can't reverse replicate the content", e);
		} finally {
			resolver.close();
		}
	}
}
