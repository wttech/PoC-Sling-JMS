package com.cognifide.jms.session.model;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.ArrayUtils;

public class SharedSession {
	private final Map<String, Object> attributes;

	private final String id;

	private Calendar lastRefreshed;

	private int maxInactiveInterval;

	public SharedSession(String id) {
		this.id = id;
		attributes = new HashMap<String, Object>();
		lastRefreshed = Calendar.getInstance();
		maxInactiveInterval = 60 * 60 * 24;
	}

	public void putValue(String name, Object value) {
		attributes.put(name, value);
	}

	public void removeValue(String key) {
		attributes.remove(key);
	}

	public Map<String, Object> getMap() {
		return Collections.unmodifiableMap(attributes);
	}

	public void copyTo(HttpSession session) {
		for (Entry<String, Object> entry : attributes.entrySet()) {
			if (session.getValue(entry.getKey()) == null) {
				session.putValue(entry.getKey(), entry.getValue());
			}
		}
	}

	public void refreshSession() {
		lastRefreshed = Calendar.getInstance();
	}

	public void setInactiveInterval(int interval) {
		maxInactiveInterval = interval;
	}

	public String getId() {
		return id;
	}

	public boolean isValid() {
		if (maxInactiveInterval < 0) {
			return true;
		}
		Calendar xSecondsAgo = Calendar.getInstance();
		xSecondsAgo.add(Calendar.SECOND, -maxInactiveInterval);
		return lastRefreshed.after(xSecondsAgo);
	}

	public void applyDiff(SessionDiff diff) {
		this.maxInactiveInterval = diff.getMaxInactiveInterval();
		for (Entry<String, Object> e : diff.getValues().entrySet()) {
			if (e.getValue() == null) {
				attributes.remove(e.getKey());
			} else {
				attributes.put(e.getKey(), e.getValue());
			}
		}
	}

	public SessionDiff getDiff(HttpSession httpSession) throws JMSException {
		if (httpSession == null) {
			return null;
		}

		boolean different = false;
		if (this.maxInactiveInterval != httpSession.getMaxInactiveInterval()) {
			different = true;
		}

		String[] valueNames = httpSession.getValueNames();
		Map<String, Object> values = new HashMap<String, Object>();
		for (String key : valueNames) {
			Object value = httpSession.getValue(key);
			if (value != null && !value.equals(attributes.get(key))) {
				different = true;
				values.put(key, value);
			}
		}

		for (String key : attributes.keySet()) {
			if (!ArrayUtils.contains(valueNames, key)) {
				different = true;
				values.put(key, null);
			}
		}

		if (different) {
			return new SessionDiff(values, httpSession.getMaxInactiveInterval());
		} else {
			return null;
		}
	}

}
