package com.cognifide.jms.api;

import javax.jms.Connection;
import javax.jms.JMSException;

public interface JmsConnectionProvider {
	Connection getConnection() throws JMSException;
}
