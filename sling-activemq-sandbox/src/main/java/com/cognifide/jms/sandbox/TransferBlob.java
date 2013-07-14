package com.cognifide.jms.sandbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.servlet.ServletException;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.BlobMessage;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.jms.api.JmsConnectionProvider;

@Component(immediate = true, metatype = false)
@SlingServlet(paths = "/bin/cognifide/blob", extensions = "txt", generateService = true, generateComponent = false)
public class TransferBlob extends SlingSafeMethodsServlet {

	private static final Logger LOG = LoggerFactory.getLogger(TransferBlob.class);

	private static final long serialVersionUID = -7005192596672482278L;

	@Reference
	private JmsConnectionProvider connectionProvider;

	private ActiveMQConnection connection;

	private ActiveMQSession session;

	private MessageConsumer consumer;

	private MessageProducer producer;

	@Activate
	public void activate() throws JMSException, MalformedURLException {
		LOG.info("Creating JMS connection");
		connection = (ActiveMQConnection) connectionProvider.getConnection();
		session = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination dest = session.createTopic("blob_test");
		consumer = session.createConsumer(dest);
		producer = session.createProducer(dest);
		connection.start();
	}

	@Deactivate
	public void deactivate() throws JMSException {
		producer.close();
		consumer.close();
		session.close();
		connection.close();
	}

	@Override
	public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException {
		String suffix = request.getRequestPathInfo().getSuffix();
		try {
			if ("/send".equals(suffix)) {
				send();
			} else if ("/recv".equals(suffix)) {
				recv(response);
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private void send() throws JMSException, MalformedURLException {
		String propertyPath = "/apps/geometrixx/components/asseteditor/asseteditor.jsp/jcr:content/jcr:data";
		BlobMessage msg = session.createBlobMessage(new File(propertyPath));
		msg.setBooleanProperty("jcr_blob", true);
		producer.send(msg);
	}

	private void recv(SlingHttpServletResponse response) throws JMSException, IOException {
		Message msg = consumer.receive(1000 * 15);
		BlobMessage blob = (BlobMessage) msg;
		OutputStream os = response.getOutputStream();
		InputStream is = blob.getInputStream();

		byte[] buf = new byte[1024];
		int len = 0;
		while ((len = is.read(buf)) > -1) {
			os.write(buf, 0, len);
		}
	}
}
