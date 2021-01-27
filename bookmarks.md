## Inter-Sling communication with a message queue
#### adaptTo() 2013 presentation code bookmarks 

* Shared session
  * [servlet filter logic](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-session/src/main/java/com/cognifide/jms/session/SharedSessionFilter.java#L106)
  * [demo servlet](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-sandbox/src/main/java/com/cognifide/jms/sandbox/SharedSessionServlet.java)
* Integrating ActiveMQ
  * [pom.xml with all dependencies](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-activemq/pom.xml)
* JMS Connection Provider
  * [interface](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-api/src/main/java/com/cognifide/jms/api/JmsConnectionProvider.java)
  * [implementation](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-impl-activemq/src/main/java/com/cognifide/jms/impl/activemq/ActiveMQConnectionProvider.java)
  * [sample usage](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-session/src/main/java/com/cognifide/jms/session/SharedSessionFilter.java#L70)
* Discovery API
  * [documentation](http://sling.apache.org/documentation/bundles/discovery-api-and-impl.html)
  * [election logic](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-discovery/src/main/java/com/cognifide/jms/discovery/election/Election.java)
  * [demo servlet](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-sandbox/src/main/java/com/cognifide/jms/sandbox/InfoServlet.java)
* Sling Message Consumer
  * [sample usage](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-session/src/main/java/com/cognifide/jms/session/SharedSessionStorage.java#L30)
  * [message listener registry component](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-impl-activemq/src/main/java/com/cognifide/jms/impl/activemq/consumer/MessageListenerRegistry.java)
* Reverse Replication Request
  * [OSGi services](https://github.com/wttech/PoC-Sling-JMS/tree/master/cq-jms-replication/src/main/java/com/cognifide/jms/replication)
* Targeting messages with Sling run mode
  * [sample usage](https://github.com/wttech/PoC-Sling-JMS/blob/master/cq-jms-replication/src/main/java/com/cognifide/jms/replication/OutboxEventHandler.java#L100)
* Additional tools
  * [embedded broker](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-impl-activemq/src/main/java/com/cognifide/jms/impl/activemq/ActiveMQEmbeddedBroker.java)
  * [binary transfer demo](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-sandbox/src/main/java/com/cognifide/jms/sandbox/TransferBlob.java)
  * [object msg utils](https://github.com/wttech/PoC-Sling-JMS/blob/master/sling-jms-api/src/main/java/com/cognifide/jms/api/ObjectMessageUtils.java)
* [Main project repo](https://github.com/wttech/PoC-Sling-JMS)