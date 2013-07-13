package com.cognifide.jms.sandbox;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

@Component(immediate = true, metatype = false)
@SlingServlet(paths = "/bin/cognifide/session", extensions = "txt", generateService = true, generateComponent = false)
public class SharedSessionServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = -7005192596672482278L;

	@Override
	public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException {
		String suffix = request.getRequestPathInfo().getSuffix();
		try {
			HttpSession session = request.getSession();
			if ("/add".equals(suffix)) {
				session.setAttribute(new Date().toString(), UUID.randomUUID().toString());
			}
			PrintWriter writer = response.getWriter();
			@SuppressWarnings("unchecked")
			Enumeration<String> names = session.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				writer.append(name).append(": ").append(session.getAttribute(name).toString()).append('\n');
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

}
