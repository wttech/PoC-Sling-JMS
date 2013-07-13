package com.cognifide.jms.session.model;

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

	private SharedSessionCookie(SlingHttpServletResponse response, String instanceId, String sharedSessionId) {
		this.response = response;
		this.instanceId = instanceId;
		this.sharedSessionId = sharedSessionId;
		this.cookieExists = true;
	}

	public SharedSessionCookie set(String instanceId, String sharedSessionId) {
		String header = String.format("%s=%s|%s; Path=/; HttpOnly", SHARED_SESSION_COOKIE, instanceId,
				sharedSessionId);
		response.setHeader("Set-cookie", header);
		return new SharedSessionCookie(response, instanceId, sharedSessionId);
	}

	public boolean exists() {
		return cookieExists;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public String getSharedSessionId() {
		return sharedSessionId;
	}

}
