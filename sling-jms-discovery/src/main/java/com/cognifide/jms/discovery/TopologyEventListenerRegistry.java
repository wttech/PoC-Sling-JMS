package com.cognifide.jms.discovery;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = false)
@Service(value = TopologyEventListenerRegistry.class)
public class TopologyEventListenerRegistry {

	private static final Logger LOG = LoggerFactory.getLogger(TopologyEventListenerRegistry.class);

	@Reference(referenceInterface = TopologyEventListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private Set<TopologyEventListener> listeners = new CopyOnWriteArraySet<TopologyEventListener>();

	public void post(TopologyEvent.Type type, TopologyView oldView, TopologyView newView) {
		LOG.info("Sending " + type + " TopologyEvent");
		TopologyEvent event = new TopologyEvent(type, oldView, newView);
		for (TopologyEventListener l : listeners) {
			try {
				l.handleTopologyEvent(event);
			} catch (Exception e) {
				LOG.error("Topology event listener threw an error", e);
			}
		}
	}

	void bindListeners(TopologyEventListener service) {
		listeners.add(service);
	}

	void unbindListeners(TopologyEventListener service) {
		listeners.remove(service);
	}
}
