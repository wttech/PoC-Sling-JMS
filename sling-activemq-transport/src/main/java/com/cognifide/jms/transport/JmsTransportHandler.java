package com.cognifide.jms.transport;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationResult;
import com.day.cq.replication.ReplicationTransaction;
import com.day.cq.replication.TransportContext;
import com.day.cq.replication.TransportHandler;

@Component(immediate = true, metatype = false)
@Service
public class JmsTransportHandler implements TransportHandler {

	@Override
	public boolean canHandle(AgentConfig config) {
		return false;
	}

	@Override
	public ReplicationResult deliver(TransportContext ctx, ReplicationTransaction transaction)
			throws ReplicationException {
		return null;
	}

}
