package com.cognifide.jms.sandbox.session.model;

import java.util.Collections;
import java.util.Map;

public class SharedSession {
	private Map<String, Object> attributes;

	public void putValue(String name, Object value) {
		attributes.put(name, value);
	}

	public void removeValue(String key) {
		attributes.remove(key);
	}

	public Map<String, Object> getMap() {
		return Collections.unmodifiableMap(attributes);
	}
}
