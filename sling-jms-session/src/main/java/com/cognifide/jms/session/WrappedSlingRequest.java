package com.cognifide.jms.session;

import javax.servlet.http.HttpSession;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

public class WrappedSlingRequest extends SlingHttpServletRequestWrapper {

	private boolean sessionHasBeenUsed;

	public WrappedSlingRequest(SlingHttpServletRequest wrappedRequest) {
		super(wrappedRequest);
		this.sessionHasBeenUsed = false;
	}

	@Override
	public HttpSession getSession() {
		sessionHasBeenUsed = true;
		return super.getSession();
	}

	@Override
	public HttpSession getSession(boolean created) {
		HttpSession session = super.getSession(created);
		if (session != null) {
			sessionHasBeenUsed = true;
		}
		return session;
	}

	public boolean sessionHasBeenUsed() {
		return sessionHasBeenUsed;
	}
}
