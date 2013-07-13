package com.cognifide.jms.api;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

public class ObjectMessageUtils {
	private ObjectMessageUtils() {
	}

	@SuppressWarnings("unchecked")
	public static <T> T getObject(ObjectMessage msg, Class<T> clazz) throws JMSException {
		return (T) getObjectInContext(msg, clazz);
	}

	public static Serializable getObjectInContext(ObjectMessage msg, Class<?> clazz) throws JMSException {
		Thread currentThread = Thread.currentThread();
		ClassLoader cl = currentThread.getContextClassLoader();
		try {
			currentThread.setContextClassLoader(clazz.getClassLoader());
			return msg.getObject();
		} finally {
			currentThread.setContextClassLoader(cl);
		}
	}
}
