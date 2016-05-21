package org.ros.internal.node.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.ros.internal.node.client.SlaveClient;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.SlaveServer;
import org.ros.internal.transport.ProtocolDescription;
import org.ros.internal.transport.ProtocolNames;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

/**
 * A {@link Runnable} which is used whenever new publishers are being added to a
 * {@link DefaultSubscriber}. It takes care of registration between the {@link Subscriber}
 * and remote {@link Publisher}.
 * 
 * @author jg
 */
class UpdatePublisherRunnable<MessageType> implements Runnable {

  private static final Log log = LogFactory.getLog(UpdatePublisherRunnable.class);

  private final DefaultSubscriber<MessageType> subscriber;
  private final PublisherIdentifier publisherIdentifier;
  private final NodeIdentifier nodeIdentifier;

  /**
   * @param subscriber
   * @param nodeIdentifier
   *          {@link NodeIdentifier} of the {@link Subscriber}'s
   *          {@link SlaveServer}
   * @param publisherIdentifier
   *          {@link PublisherIdentifier} of the new {@link Publisher}
   */
  public UpdatePublisherRunnable(DefaultSubscriber<MessageType> subscriber,
      NodeIdentifier nodeIdentifier, PublisherIdentifier publisherIdentifier) {
    this.subscriber = subscriber;
    this.nodeIdentifier = nodeIdentifier;
    this.publisherIdentifier = publisherIdentifier;
  }

  @Override
  public void run() {
    SlaveClient slaveClient;
    try {
      log.info("Attempting to create SlaveClient:"+nodeIdentifier.getName()+" pub:"+publisherIdentifier.getNodeUri());
      slaveClient = new SlaveClient(nodeIdentifier.getName(), publisherIdentifier.getNodeUri());
      log.info("Slave client created "+nodeIdentifier.getName()+" pub:"+publisherIdentifier.getNodeUri());
      log.info("Requesting topic name "+subscriber.getTopicName());
      Response<ProtocolDescription> response =
          slaveClient.requestTopic(subscriber.getTopicName(), ProtocolNames.SUPPORTED);
      // If null there is no publisher for the requested topic
      if( response != null ) {
    	  ProtocolDescription selected = response.getResult();
    	  if (ProtocolNames.SUPPORTED.contains(selected.getName())) {
    		  subscriber.addPublisher(publisherIdentifier, selected.getAddress());
    	  } else {
    		  log.error("Publisher returned unsupported protocol selection: " + response);
    	  }
      } else {
    	  log.error("There are NO publishers available for topic "+subscriber.getTopicName());
      }
    } catch (Exception e) {
      // TODO(damonkohler): Retry logic is needed at the RPC layer.
      log.error(e);
    }
  }
}
