package com.cognifide.jms.sandbox.session.model;

import javax.servlet.http.Cookie;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

public class SharedSessionCookie {

	private static final String SHARED_SESSION_COOKIE = "JSESSIONID_SHARED";

	private final SlingHttpServletResponse response;

	private final boolean cookieExists;

	private final String instanceId;

	private final String sharedSessionId;

	public SharedSessionCookie(SlingHttpServletRequest request, SlingHttpServletResponse response) {
		this.response = response;

		Cookie cookie = request.getCookie(SHARED_SESSION_COOKIE);
		if (cookie == null) {
			cookieExists = false;
			instanceId = null;
			sharedSessionId = null;
		} else {
			cookieExists = true;
			String[] value = StringUtils.split(cookie.getValue(), '|');
			instanceId = value[0];
			sharedSessionId = value[1];
		}
	}

	public void set(String instanceId, String sharedSessionId) {
		String value = String.format("%s|%s", instanceId, sharedSessionId);
		Cookie cookie = new Cookie(SHARED_SESSION_COOKIE, value);
		cookie.setMaxAge(-1);
		response.addCookie(cookie);
	}

	public boolean isCookieExists() {
		return cookieExists;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public String getSharedSessionId() {
		return sharedSessionId;
	}

}
