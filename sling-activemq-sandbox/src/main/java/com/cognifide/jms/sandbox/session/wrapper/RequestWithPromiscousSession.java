package com.cognifide.jms.sandbox.session.wrapper;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

public class RequestWithPromiscousSession extends SlingHttpServletRequestWrapper {

	private final SessionListener listener;

	private final Map<String, Object> defaultValues;

	private boolean defaultValuesSet = false;

	public RequestWithPromiscousSession(SlingHttpServletRequest wrappedRequest, SessionListener listener) {
		super(wrappedRequest);
		this.listener = listener;
		this.defaultValues = null;
	}

	public RequestWithPromiscousSession(SlingHttpServletRequest wrappedRequest, SessionListener listener,
			Map<String, Object> defaultValues) {
		super(wrappedRequest);
		this.listener = listener;
		this.defaultValues = defaultValues;
	}

	@Override
	public HttpSession getSession() {
		HttpSession session = super.getSession();
		setDefaultValues(session);
		return new PromiscousSession(session, listener);
	}

	@Override
	public HttpSession getSession(boolean create) {
		HttpSession session = super.getSession(create);
		if (session != null) {
			setDefaultValues(session);
			return new PromiscousSession(session, listener);
		} else {
			return null;
		}
	}

	private void setDefaultValues(HttpSession session) {
		if (defaultValues == null || defaultValuesSet) {
			return;
		}
		for (Entry<String, Object> entry : defaultValues.entrySet()) {
			if (session.getValue(entry.getKey()) == null) {
				session.putValue(entry.getKey(), entry.getValue());
			}
		}
	}
}
