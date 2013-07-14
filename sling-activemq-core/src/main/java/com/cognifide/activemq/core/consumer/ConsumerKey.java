package com.cognifide.activemq.core.consumer;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.cognifide.jms.api.JmsConstants;

public class ConsumerKey {
	private final ConsumerType type;

	private final String subject;

	public ConsumerKey(Map<String, Object> properties) {
		this.type = ConsumerType.valueOf((String) properties.get(JmsConstants.CONSUMER_TYPE));
		this.subject = (String) properties.get(JmsConstants.CONSUMER_SUBJECT);
	}

	public ConsumerType getType() {
		return type;
	}

	public String getSubject() {
		return subject;
	}

	public Destination createDestination(Session session) throws JMSException {
		switch (type) {
			case QUEUE:
				return session.createQueue(subject);
			case TOPIC:
				return session.createTopic(subject);
			default:
				return null;
		}
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
		ConsumerKey rhs = (ConsumerKey) obj;
		return new EqualsBuilder().append(this.type, rhs.type).append(this.subject, rhs.subject).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(type).append(subject).toHashCode();
	}

}
