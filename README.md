# Sling ActiveMQ

## Bundles

`sling-activemq-osgi`, `sling-activemq-api` and `sling-activemq-core` are main bundles. The other bundles are some implementations using the JMS.

### sling-activemq-osgi

This bundle contains ActiveMQ core package with all necessary dependencies marked as embedded. It exports `org.apache.activemq.*` Java package.

### sling-activemq-api

This package contains declarations of useful services.

`JmsConnectionProvider`

OSGi service providing the JMS connection.

`ObjectMessageUtils`

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

#### Blob

Waiting for the message:

	curl localhost:4503/bin/cognifide/blob.txt/recv

Sending message (in other terminal window):

	curl localhost:4504/bin/cognifide/blob.txt/send
    
#### Session

Display current session:

	curl localhost:4503/bin/cognifide/session.txt
	
Add random value to the current session

	curl localhost:4504/bin/cognifide/session/add.txt

### sling-activemq-session

Bundle provides sharing HTTP session feature based on JMS.

Besides the ordinary `JSESSIONID` each user gets custom `JSESSIONID_SHARED` cookie. This cookie consists of two concatenated UUIDs: `INSTANCE_ID` and `SHARED_SESSION_ID`. Each modification of the `HttpSession` is mapped to the current `SharedSession` (got from the `SharedSessionStorage` using it's id) and broadcasted to all instances. The component responsible for this logic is `SharedSessionFilter`.

When `SharedSessionFilter` notices that `INSTANCE_ID` from the cookie isn't the current instance id (eg. instance has been changed by the loadbalancer), content of the `SharedSession` is copied back to the `HttpSession`.