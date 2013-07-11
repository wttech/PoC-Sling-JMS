package com.cognifide.jms.sandbox.session.wrapper;

public interface SessionListener {

	void invalidate();

	void putValue(String key, Object value);

	void removeValue(String key);
}
