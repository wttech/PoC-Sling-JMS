# Sling ActiveMQ

## Bundles

### sling-activemq-osgi

This bundle contains ActiveMQ core package with all necessary dependencies marked as embedded. It exports `org.apache.activemq.*` Java package.

### sling-activemq-api

This package contains declarations of useful services.

#### `JmsConnectionProvider`

OSGi service providing the JMS connection.

#### `ObjectMessageUtils`

Reading object messages in OSGi container can be tricky, as the JMS client lives in different bundle than the serialized class. Methods in this util switches thread class loader for the moment of deserializing JMS object message.

### sling-activemq-core

This service contains implementaion of `JmsConnectionProvider` service. Besides that it handles Sling blob transfers.

#### Sling blob transfers

Sling blob transfers allows to send binary JCR properties using dedicated Sling HTTP servlet rather than ActiveMQ channel. In order to use it, first configure OSGi service `com.cognifide.activemq.core.blob.SlingBlobServlet`. You need to enter URL and credentials under which the servlet will be available for other Sling instances. After that any JCR property can be send as follows:

	String propertyPath = "/apps/.../jcr:content/jcr:data";
	BlobMessage msg = session.createBlobMessage(new File(propertyPath));
	msg.setBooleanProperty("jcr_blob", true);
	producer.send(msg);

### sling-activemq-discovery

It's a JMS implementation of [Sling Discovery API](http://sling.apache.org/documentation/bundles/discovery-api-and-impl.html).

### sling-activemq-sandbox

Example usage of shared session and Sling blob transfer.

### sling-activemq-session

Bundle provides sharing HTTP session feature based on JMS and special servlet filter `com.cognifide.jms.session.SharedSessionFilter`.