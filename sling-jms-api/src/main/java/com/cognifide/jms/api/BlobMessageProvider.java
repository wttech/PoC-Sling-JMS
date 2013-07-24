package com.cognifide.jms.api;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

public interface BlobMessageProvider {
	public Message createBlobMessage(Session session, String nodePath, String property) throws JMSException;
}
