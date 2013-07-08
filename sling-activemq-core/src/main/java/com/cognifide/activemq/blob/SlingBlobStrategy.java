package com.cognifide.activemq.blob;

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
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

public class SlingBlobStrategy implements BlobDownloadStrategy, BlobUploadStrategy {

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
		if (!message.getBooleanProperty("jcr_blob")) {
			return blobUploadStrategy.uploadFile(message, file);
		}
		return new URL(blobServlet.addMapping(file.getPath()));
	}

	@Override
	public URL uploadStream(ActiveMQBlobMessage message, InputStream in) throws JMSException, IOException {
		return blobUploadStrategy.uploadStream(message, in);
	}

	@Override
	public InputStream getInputStream(ActiveMQBlobMessage message) throws IOException, JMSException {
		if (!message.getBooleanProperty("jcr_blob")) {
			return blobDownloadStrategy.getInputStream(message);
		}
		HttpClient client = new DefaultHttpClient();
		HttpUriRequest request = new HttpGet(message.getRemoteBlobUrl());
		authenticate(request);
		HttpResponse response = client.execute(request);
		return response.getEntity().getContent();
	}

	@Override
	public void deleteFile(ActiveMQBlobMessage message) throws IOException, JMSException {
		if (!message.getBooleanProperty("jcr_blob")) {
			blobDownloadStrategy.deleteFile(message);
			return;
		}
		HttpClient client = new DefaultHttpClient();
		HttpUriRequest request = new HttpDelete(message.getRemoteBlobUrl());
		authenticate(request);
		client.execute(request);
	}

	private void authenticate(HttpRequest request) throws IOException {
		try {
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "admin");
			request.addHeader(new BasicScheme().authenticate(creds, request));
		} catch (AuthenticationException e) {
			throw new IOException(e);
		}
	}
}
