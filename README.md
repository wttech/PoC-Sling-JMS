# Sling ActiveMQ

`sling-activemq-osgi`, `sling-activemq-api` and `sling-activemq-core` are bundles integrating ActiveMQ with Sling framework. `sling-activemq-session` and `sling-activemq-discovery` implements some features using ActiveMQ. `sling-activemq-sandbox` is a playground.

## sling-activemq-osgi

This bundle contains ActiveMQ core package with all necessary dependencies marked as embedded. It exports `org.apache.activemq.*` Java package.

## sling-activemq-api

This package contains declarations of useful services.

`JmsConnectionProvider`

OSGi service providing the JMS connection.

`ObjectMessageUtils`

Reading object messages in OSGi container can be tricky, as the JMS client lives in different bundle than the serialized class. Methods in this util switches thread class loader for the moment of deserializing JMS object message.

## sling-activemq-core

This service contains implementation of `JmsConnectionProvider` service. Besides that it implements a few useful features.

#### Sling blob transfers

Sling blob transfers allows to send binary JCR properties using dedicated Sling HTTP servlet rather than ActiveMQ channel. In order to use it, first configure OSGi service `com.cognifide.activemq.core.blob.SlingBlobServlet`. You need to enter URL and credentials under which the servlet will be available for other Sling instances. After that any JCR property can be sent as follows:

	String propertyPath = "/apps/.../jcr:content/jcr:data";
	BlobMessage msg = session.createBlobMessage(new File(propertyPath));
	msg.setBooleanProperty(JmsConstants.JCR_BLOB_MESSAGE, true);
	producer.send(msg);

#### OSGi MessageListener

`MessageListenerRegistry` OSGi components collects services implementing `javax.jms.MessageListener` interface and creates appropriate message consumer for them using configuration set in the SCR properties. Example service is `com.cognifide.jms.session.SharedSessionStorage`:

	@Component(immediate = true, metatype = false)
	@Service(value = { SharedSessionStorage.class, MessageListener.class })
	@Properties({
		@Property(name = JmsConstants.CONSUMER_SUBJECT, value = "sharedSessionTopic"),
		@Property(name = JmsConstants.CONSUMER_TYPE, value = JmsConstants.TYPE_TOPIC) })
	public class SharedSessionStorage implements MessageListener {
	
		@Override
		public synchronized void onMessage(Message msg) {
			…
		}
	}

Listeners can also filter received messages by their JMS properties. Use `JmsConstants.FILTER` configuration property and [LDAP filter syntax](http://www.osgi.org/javadoc/r4v43/core/org/osgi/framework/Filter.html). Eg.:

    @Property(name = JmsConstants.FILTER, value = "(action=REFRESH_TOPOLOGY)")

will match following message:

    Message msg = session.createMessage();
    msg.setStringProperty("action", "REFRESH_TOPOLOGY");
    
## sling-activemq-scr

Above SCR annotations can be replaced with custom `@SlingMessageConsumer`:

	@SlingMessageConsumer(destinationType = DestinationType.TOPIC, subject = "sharedSessionTopic")

It'll automatically create `@Component`, `@Service` and all necessary `@Properties`. All you need to do is to add following dependency to your `pom.xml`:


	<project>
		<build>
			<plugin>
				<groupId>org.apache.felix</groupId>
				<artifactId>maven-scr-plugin</artifactId>
				<version>1.13.0</version>
				<dependencies>
					<dependency>
						<groupId>com.cognifide.activemq</groupId>
						<artifactId>sling-activemq-scr</artifactId>
						<version>0.1.0-SNAPSHOT</version>
					</dependency>
				</dependencies>
			</plugin>

## sling-activemq-discovery

It's a JMS implementation of [Sling Discovery API](http://sling.apache.org/documentation/bundles/discovery-api-and-impl.html). `HeartBeat` component sends an update every 30 seconds (it can be configured). `JmsDiscoveryService` receives these updates and maintains topology model.

If the created model lacks of leader for some cluster, there is election process performed.

* `WHO_IS_LEADER` request is sent,
* component waits 10 seconds for `I_AM_LEADER` message,
  * if someone sends it, the election will be over,
* if there is still no leader, `ELECTION` request is sent,
* all instances in given cluster have 5 seconds to send `VOTE`s with their own Sling `instanceId`,
* after that, instance with the smallest `instanceId` sends `I_AM_LEADER` message.

The whole process is performed in the cluster scope.

## sling-activemq-session

Bundle provides sharing HTTP session feature based on JMS.

Besides the ordinary `JSESSIONID` each user gets custom `JSESSIONID_SHARED` cookie. This cookie consists of two concatenated UUIDs: `INSTANCE_ID` and `SHARED_SESSION_ID`. Each modification of the `HttpSession` is mapped to the current `SharedSession` (got from the `SharedSessionStorage` using it's id) and broadcasted to all instances. The component responsible for this logic is `SharedSessionFilter`.

When `SharedSessionFilter` notices that `INSTANCE_ID` from the cookie isn't the current instance id (eg. instance has been changed by the loadbalancer), content of the `SharedSession` is copied back to the `HttpSession`.

Please notice that all objects put into the `HttpSession` has to implement `Serializable` interface as they are transfered between instances. What's more, if used class are not available in the global OSGi class loader (eg. classes from your custom bundle), ActiveMQ consumer won't be able to deserialize them properly. In order to avoid `ClassNotFoundException` you can implement OSGi interface `com.cognifide.jms.api.session.ClassLoaderProvider`. `SharedSessionStorage` will try to deserialize object messages using each class loader provided by these services. 

## sling-activemq-transport

This bundle enhances Adobe CQ reverse-replication mechanism. In the out-of-the-box installation author instance pings the publish every 30 seconds asking for the content to be reverse-replicated. In the worst case users has to wait these 30 seconds before their content got replicated. With JMS publish instance can inform the author when the reverse-replication should happen. Bundle consists of two services:

* `OutboxEventHandler` waits for events related to the reverse-replication outbox and send `POLL` message to the author whenever there is some new user-generated content,
* `ReplicationAgentInvoker` works on the author and invokes reverse-replication process every time it gets `POLL` message form the author.

The first service can be configured to send `POLL` message to instances with specified run mode (`author` by default) and to invoke agents with specific id (`publish_reverse`). It is recommended to disable default reverse-replication process by stopping `com.day.cq.replication.impl.ReverseReplicator` OSGi component.

## sling-activemq-sandbox

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
	
#### Discovery

Display current topology:

	curl localhost:4502/bin/jms/discovery/info.txt
