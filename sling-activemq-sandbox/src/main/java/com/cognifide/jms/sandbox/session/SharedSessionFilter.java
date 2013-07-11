package com.cognifide.jms.sandbox.session;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import com.cognifide.jms.sandbox.session.model.SharedSession;
import com.cognifide.jms.sandbox.session.model.SharedSessionCookie;
import com.cognifide.jms.sandbox.session.wrapper.RequestWithPromiscousSession;
import com.cognifide.jms.sandbox.session.wrapper.SessionListener;

@SlingFilter(order = Integer.MIN_VALUE)
public class SharedSessionFilter implements Filter {

	@Reference
	private SharedSessionStorage storage;

	@Reference
	private BroadcastService broadcastService;

	private String instanceId;

	@Activate
	protected void activate() {
		this.instanceId = UUID.randomUUID().toString();
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		ServletRequest wrappedRequest = request;
		if (request instanceof SlingHttpServletRequest && response instanceof SlingHttpServletResponse) {
			SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
			SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
			wrappedRequest = handleRequest(slingRequest, slingResponse);
		}
		chain.doFilter(wrappedRequest, response);
	}

	private ServletRequest handleRequest(SlingHttpServletRequest slingRequest,
			SlingHttpServletResponse slingResponse) {
		ServletRequest wrappedRequest;
		SharedSessionCookie cookie = new SharedSessionCookie(slingRequest, slingResponse);
		boolean copySharedSessionAttributesToRequest = false;
		String sharedSessionId;
		if (cookie.isCookieExists()) {
			if (!instanceId.equals(cookie.getInstanceId())) {
				copySharedSessionAttributesToRequest = true;
				cookie.set(instanceId, cookie.getSharedSessionId());
			}
			sharedSessionId = cookie.getSharedSessionId();
		} else {
			cookie.set(instanceId, sharedSessionId = UUID.randomUUID().toString());
		}
		SessionListener broadcastingListener = broadcastService.getBroadcastingListener(sharedSessionId);
		if (copySharedSessionAttributesToRequest) {
			SharedSession sharedSession = storage.getSharedSession(cookie.getSharedSessionId());
			wrappedRequest = new RequestWithPromiscousSession(slingRequest, broadcastingListener,
					sharedSession.getMap());
		} else {
			wrappedRequest = new RequestWithPromiscousSession(slingRequest, broadcastingListener);
		}
		return wrappedRequest;
	}
}
