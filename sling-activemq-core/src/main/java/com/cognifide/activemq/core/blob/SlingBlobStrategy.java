package com.cognifide.activemq.core.blob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.jms.JMSException;

import org.apache.activemq.blob.BlobDownloadStrategy;
import org.apache.activemq.blob.BlobUploadStrategy;
import org.apache.activemq.command.ActiveMQBlobMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.ContextAwareAuthScheme;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.cognifide.jms.api.SlingJmsProperties;

public class SlingBlobStrategy implements BlobDownloadStrategy, BlobUploadStrategy {

	private static final String LOGIN_PARAMETER = SlingBlobStrategy.class.getName() + ".login";

	private static final String PASSWORD_PARAMETER = SlingBlobStrategy.class.getName() + ".password";

	private SlingBlobServlet blobServlet;

	private BlobDownloadStrategy blobDownloadStrategy;

	private BlobUploadStrategy blobUploadStrategy;

	public SlingBlobStrategy(BlobUploadStrategy uploadStrategy, SlingBlobServlet blobServlet) {
		this.blobUploadStrategy = uploadStrategy;
		this.blobDownloadStrategy = null;
		this.blobServlet = blobServlet;
	}

	public SlingBlobStrategy(BlobDownloadStrategy downloadStrategy, SlingBlobServlet blobServlet) {
		this.blobUploadStrategy = null;
		this.blobDownloadStrategy = downloadStrategy;
		this.blobServlet = blobServlet;
	}

	@Override
	public URL uploadFile(ActiveMQBlobMessage message, File file) throws JMSException, IOException {
		if (!message.getBooleanProperty(SlingJmsProperties.JCR_BLOB_MESSAGE)) {
			return blobUploadStrategy.uploadFile(message, file);
		}
		message.setStringProperty(LOGIN_PARAMETER, blobServlet.getLogin());
		message.setStringProperty(PASSWORD_PARAMETER, blobServlet.getPassword());
		return new URL(blobServlet.addMapping(file.getPath()));
	}

	@Override
	public URL uploadStream(ActiveMQBlobMessage message, InputStream in) throws JMSException, IOException {
		return blobUploadStrategy.uploadStream(message, in);
	}

	@Override
	public InputStream getInputStream(ActiveMQBlobMessage message) throws IOException, JMSException {
		if (!message.getBooleanProperty(SlingJmsProperties.JCR_BLOB_MESSAGE)) {
			return blobDownloadStrategy.getInputStream(message);
		}
		HttpClient client = new DefaultHttpClient();
		HttpContext context = new BasicHttpContext();
		HttpUriRequest request = new HttpGet(message.getRemoteBlobUrl());
		authenticate(request, context, message);
		HttpResponse response = client.execute(request, context);
		return response.getEntity().getContent();
	}

	@Override
	public void deleteFile(ActiveMQBlobMessage message) throws IOException, JMSException {
		if (!message.getBooleanProperty("jcr_blob")) {
			blobDownloadStrategy.deleteFile(message);
			return;
		}
		HttpClient client = new DefaultHttpClient();
		HttpContext context = new BasicHttpContext();
		HttpUriRequest request = new HttpDelete(message.getRemoteBlobUrl());
		authenticate(request, context, message);
		client.execute(request, context);
	}

	private void authenticate(HttpRequest request, HttpContext context, ActiveMQBlobMessage message)
			throws IOException, JMSException {
		try {
			String login = message.getStringProperty(LOGIN_PARAMETER);
			String password = message.getStringProperty(PASSWORD_PARAMETER);
			ContextAwareAuthScheme authScheme = new BasicScheme();
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(login, password);
			authScheme.authenticate(creds, request, context);
		} catch (AuthenticationException e) {
			throw new IOException(e);
		}
	}
}
