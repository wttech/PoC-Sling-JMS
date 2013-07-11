package com.cognifide.jms.sandbox.session.wrapper;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;


@SuppressWarnings("deprecation")
public class PromiscousSession implements HttpSession {
	private final HttpSession session;

	private final SessionListener listener;

	public PromiscousSession(HttpSession session, SessionListener listener) {
		this.session = session;
		this.listener = listener;
	}

	@Override
	public long getCreationTime() {
		return session.getCreationTime();
	}

	@Override
	public String getId() {
		return session.getId();
	}

	@Override
	public long getLastAccessedTime() {
		return session.getLastAccessedTime();
	}

	@Override
	public int getMaxInactiveInterval() {
		return session.getMaxInactiveInterval();
	}

	@Override
	public HttpSessionContext getSessionContext() {
		return session.getSessionContext();
	}

	@Override
	public Object getValue(String key) {
		return session.getValue(key);
	}

	@Override
	public String[] getValueNames() {
		return session.getValueNames();
	}

	@Override
	public void invalidate() {
		listener.invalidate();
		session.invalidate();
	}

	@Override
	public boolean isNew() {
		return session.isNew();
	}

	@Override
	public void putValue(String key, Object value) {
		listener.putValue(key, value);
		session.putValue(key, value);
	}

	@Override
	public void removeValue(String key) {
		listener.removeValue(key);
		session.removeValue(key);
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		session.setMaxInactiveInterval(interval);
	}
}
