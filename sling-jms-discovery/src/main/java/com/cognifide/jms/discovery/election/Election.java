package com.cognifide.jms.discovery.election;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.discovery.DiscoveryJmsProducer;

public class Election {

	private static final Logger LOG = LoggerFactory.getLogger(Election.class);

	private final String clusterId;

	private final String slingId;

	private final DiscoveryJmsProducer jms;

	private final ExecutorService executor;

	private final List<String> votes;

	private volatile ElectionState state;

	public Election(String clusterId, String slingId, DiscoveryJmsProducer jms) throws JMSException {
		this.jms = jms;
		this.slingId = slingId;
		this.clusterId = clusterId;
		this.executor = Executors.newCachedThreadPool();
		this.votes = new ArrayList<String>();
	}

	public void getLeader() throws JMSException {
		LOG.info("Election: get leader for " + clusterId);
		state = ElectionState.WAITING_FOR_LEADER;
		jms.sendElectionReq(ElectionRequestType.WHO_IS_LEADER, clusterId, slingId);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				sleep(10);
				if (state == ElectionState.WAITING_FOR_LEADER) {
					try {
						performElection();
					} catch (JMSException e) {
						LOG.error("Can't perform election: " + e.getMessage());
					}
				}
			}
		});
	}

	public void shutDown() {
		state = ElectionState.SHUT_DOWN;
	}

	private void performElection() throws JMSException {
		LOG.info("Election: perform election for " + clusterId);
		state = ElectionState.WAITING_FOR_VOTES;
		jms.sendElectionReq(ElectionRequestType.ELECTION, clusterId, slingId);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				sleep(5);
				try {
					if (state == ElectionState.WAITING_FOR_VOTES) {
						didIWin();
					}
				} catch (JMSException e) {
					LOG.error("Can't choose leader for " + clusterId, e);
				}
			}
		});
	}

	public void gotRequest(ElectionRequest req) {
		if (clusterId.equals(req.getClusterId()) && req.getType() == ElectionRequestType.VOTE) {
			LOG.info("Election for " + clusterId + ": got vote from " + req.getSlingId());
			votes.add(req.getSlingId());
		}
	}

	private static final void sleep(int seconds) {
		try {
			Thread.sleep(1000 * seconds);
		} catch (InterruptedException e) {
			LOG.error("Sleeping interrupted", e);
		}
	}

	private void didIWin() throws JMSException {
		Collections.sort(votes);
		if (votes.isEmpty()) {
			LOG.error("Election: no votes for " + clusterId);
		}
		if (!votes.isEmpty() && slingId.equals(votes.get(0))) {
			LOG.info("This instance is new leader for " + clusterId);
			jms.sendElectionReq(ElectionRequestType.I_AM_LEADER, clusterId, slingId);
		}
	}
}
