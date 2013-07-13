package com.cognifide.jms.session.model;

import java.io.Serializable;
import java.util.Map;

public class SessionDiff implements Serializable {
	private static final long serialVersionUID = 4034711610854696880L;

	private final Map<String, Object> values;

	private final int maxInactiveInterval;

	public SessionDiff(Map<String, Object> values, int maxInactiveInterval) {
		this.values = values;
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

}
