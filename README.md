# Sling JMS

Sling JMS is a set of bundles for integrating JMS message queue with Sling web framework. It consists of `sling-jms-api` bundle, default implementation based on `ActiveMQ` and sample applications.

## sling-jms-api

### JmsConnectionProvider

API provides useful mechanisms for integrating JMS MQ with Sling. The basic part is `JmsConnectionProvider`, an interface providing `javax.jms.Connection` to any OSGi component. Example usage:

    @Component
    public class MyComponent {
    
    	@Reference
    	private JmsConnectionProvider connectionProvider;
    	
    	private javax.jms.Connection connection;
    	
    	@Activate
    	protected void activate() throws JMSException {
    		connection = connectionProvider.getConnection();
    	}
    	
    	@Deactivate
    	protected void deactivate() throws JMSException {
    		connection.close();
    	}
    }

### MessageListener OSGi service

In order to create message consumer, you can simply create an OSGi service implementing `javax.jms.MessageListener` interface and add consumer configuration using [SCR](http://felix.apache.org/documentation/subprojects/apache-felix-maven-scr-plugin/scr-annotations.html) properties. Consumer will be created automatically. Example service:

	@Component
	@Service
	@Properties({
		@Property(name = MessageConsumerProperties.CONSUMER_SUBJECT, value = "myTopic"),
		@Property(name = MessageConsumerProperties.CONSUMER_TYPE, value = JmsConstants.TYPE_TOPIC) })
	public class MyListener implements MessageListener {
	
		@Override
		public void onMessage(Message msg) {
			…
		}
	}

Listeners can also filter received messages by their JMS properties. Use `MessageConsumerProperties.FILTER` configuration property and [LDAP filter syntax](http://www.osgi.org/javadoc/r4v43/core/org/osgi/framework/Filter.html). Eg.:

    @Property(name = MessageConsumerProperties.FILTER, value = "(action=REFRESH_TOPOLOGY)")

will match following message:

    Message msg = session.createMessage();
    msg.setStringProperty("action", "REFRESH_TOPOLOGY");

### ObjectMessageUtils

Reading object messages in OSGi container can be tricky, as the JMS client lives in different bundle than the serialized class. Methods in this util switches thread class loader for the moment of deserializing JMS object message.

	Message msg = (ObjectMessage) consumer.receive();
	MyCustomClass obj = ObjectMessageUtils.getObject(msg, MyCustomClass.class);

or

	MyCustomClass obj = (MyCustomClass) ObjectMessageUtils.getObjectInContext(msg, OtherClassFromTheSameBundle.class);

### BlobMessageProvider

`BlobMessageProvider` allows to create blob messages - messages used for sending JCR large binary properties between instances using HTTP:

	@Reference
	private BlobMessageProvider blobMessageProvider;
	
	private void send() {
		Message msg = blobMessageProvider.createBlobMessage(session,
			"/content/dam/myasset/jcr:content",
			"jcr:data");
		producer.send(msg);
	}
	
	private void recv() {
		Message msg = consumer.receive();
		InputStream is = blobMessageProvider.getInputStream(msg);
	}

Default, ActiveMQ-based implementation uses `BlobMessage`. In this case input stream may be received as follows:

	private void recvUsingActiveMq() {
		BlobMessage msg = (BlobMessage) consumer.receive();
		InputStream is = msg.getInputStream();
	}

## sling-jms-scr

This bundle is an extension to `maven-scr-plugin`. It adds support for `@SlingMessageConsumer` annotation which allows to replace following `@Property`-based configuration:

	@Component
	@Service
	@Properties({
		@Property(name = MessageConsumerProperties.CONSUMER_SUBJECT, value = "myTopic"),
		@Property(name = MessageConsumerProperties.CONSUMER_TYPE, value = JmsConstants.TYPE_TOPIC) })
	public class MyListener implements MessageListener {
	…

with this one-liner:

	@SlingMessageConsumer(destinationType = DestinationType.TOPIC, subject = "myTopic")
	public class MyListener implements MessageListener {

It'll automatically create `@Component`, `@Service` and all necessary `@Properties`. All you need to do is to add following dependency to your `pom.xml`:

	<project>
		<build>
			<plugin>
				<groupId>org.apache.felix</groupId>
				<artifactId>maven-scr-plugin</artifactId>
				<version>1.13.0</version>
				<dependencies>
					<dependency>
						<groupId>com.cognifide.cq.jms</groupId>
						<artifactId>sling-jms-scr</artifactId>
						<version>0.1.0-SNAPSHOT</version>
					</dependency>
				</dependencies>
			</plugin>

## sling-jms-activemq

This bundle contains embedded ActiveMQ library and all necessary dependencies. It exports `org.apache.activemq.*` package, so it's a convenient way to provide ActiveMQ to your Sling-based application.

## sling-jms-impl-activemq

It is an ActiveMQ-based implementation of `sling-jms-api`. Besides implementing all services described above, it contains `ActiveMQEmbeddedBroker` component which may be used to launch embedded broker inside the Sling JVM. The only required configuration is a [broker URI](http://activemq.apache.org/broker-uri.html).

## sling-jms-discovery

It's a JMS implementation of [Sling Discovery API](http://sling.apache.org/documentation/bundles/discovery-api-and-impl.html). `HeartBeat` component sends an update every 30 seconds (it can be configured). `JmsDiscoveryService` receives these updates and maintains topology model.

If the created model lacks of leader for some cluster, there is election process performed.

* `WHO_IS_LEADER` request is sent,
* component waits 10 seconds for `I_AM_LEADER` message,
  * if someone sends it, the election will be over,
* if there is still no leader, `ELECTION` request is sent,
* all instances in given cluster have 5 seconds to send `VOTE`s with their own Sling `instanceId`,
* after that, instance with the smallest `instanceId` sends `I_AM_LEADER` message.

The whole process is performed in the cluster scope.

## sling-jms-session

Bundle provides sharing HTTP session feature based on JMS.

Besides the ordinary `JSESSIONID` each user gets custom `JSESSIONID_SHARED` cookie. This cookie consists of two concatenated UUIDs: `INSTANCE_ID` and `SHARED_SESSION_ID`. Each modification of the `HttpSession` is mapped to the current `SharedSession` (got from the `SharedSessionStorage` using it's id) and broadcasted to all instances. The component responsible for this logic is `SharedSessionFilter`.

When `SharedSessionFilter` notices that `INSTANCE_ID` from the cookie isn't the current instance id (eg. instance has been changed by the loadbalancer), content of the `SharedSession` is copied back to the `HttpSession`.

Please notice that all objects put into the `HttpSession` has to implement `Serializable` interface as they are transfered between instances. What's more, if used class are not available in the global OSGi class loader (eg. classes from your custom bundle), ActiveMQ consumer won't be able to deserialize them properly. In order to avoid `ClassNotFoundException` you can implement OSGi interface `com.cognifide.jms.api.session.ClassLoaderProvider`. `SharedSessionStorage` will try to deserialize object messages using each class loader provided by these services. 

## cq-jms-replication

This bundle enhances Adobe CQ reverse-replication mechanism. In the out-of-the-box installation author instance pings the publish every 30 seconds asking for the content to be reverse-replicated. In the worst case users has to wait these 30 seconds before their content got replicated. With JMS publish instance can inform the author when the reverse-replication should happen. Bundle consists of two services:

* `OutboxEventHandler` waits for events related to the reverse-replication outbox and send `POLL` message to the author whenever there is some new user-generated content,
* `ReplicationAgentInvoker` works on the author and invokes reverse-replication process every time it gets `POLL` message form the author.

The first service can be configured to send `POLL` message to instances with specified run mode (`author` by default) and to invoke agents with specific id (`publish_reverse`). It is recommended to disable default reverse-replication process by stopping `com.day.cq.replication.impl.ReverseReplicator` OSGi component.

## sling-jms-sandbox

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
