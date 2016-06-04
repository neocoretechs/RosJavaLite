package org.ros.internal.node.topic;

import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.node.topic.DefaultPublisherListener;
import org.ros.node.topic.Publisher;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A factory for {@link Publisher} instances.
 */
public class PublisherFactory {

  private final TopicParticipantManager topicParticipantManager;
  private final MessageFactory messageFactory;
  private final ScheduledExecutorService executorService;
  private final NodeIdentifier nodeIdentifier;
  private final Object mutex;

  public PublisherFactory(NodeIdentifier nodeIdentifier,
      TopicParticipantManager topicParticipantManager, MessageFactory messageFactory,
      ScheduledExecutorService executorService) {
    this.nodeIdentifier = nodeIdentifier;
    this.topicParticipantManager = topicParticipantManager;
    this.messageFactory = messageFactory;
    this.executorService = executorService;
    mutex = new Object();
  }

  /**
   * Gets or creates a {@link Publisher} instance. {@link Publisher}s are cached
   * and reused per topic. When a new {@link Publisher} is generated, it is
   * registered with the master.
   * 
   * @param <T>
   *          the message type associated with the {@link Publisher}
   * @param topicDeclaration
   *          {@link TopicDeclaration} that is being published
 * @param arrayBlockingQueue 
   * @param messageSerializer
   *          the {@link MessageSerializer} used for published messages
   * @return a new or cached {@link Publisher} instance
   * @throws IOException 
   */
  @SuppressWarnings("unchecked")
  public <T> Publisher<T> newOrExisting(TopicDeclaration topicDeclaration, ArrayBlockingQueue<ChannelHandlerContext> arrayBlockingQueue) throws IOException {
    GraphName topicName = topicDeclaration.getName();
    synchronized (mutex) {
      if (topicParticipantManager.hasPublisher(topicName)) {
        return (DefaultPublisher<T>) topicParticipantManager.getPublisher(topicName);
      } else {
        DefaultPublisher<T> publisher =
            new DefaultPublisher<T>(nodeIdentifier, topicDeclaration, messageFactory, executorService, arrayBlockingQueue);
        publisher.addListener(new DefaultPublisherListener<T>() {
          @Override
          public void onNewSubscriber(Publisher<T> publisher, SubscriberIdentifier subscriberIdentifier) {
        	assert(publisher != null);
        	assert(subscriberIdentifier != null);
            topicParticipantManager.addPublisherConnection((DefaultPublisher<T>) publisher, subscriberIdentifier);
          }
          @Override
          public void onShutdown(Publisher<T> publisher) {
            topicParticipantManager.removePublisher((DefaultPublisher<T>) publisher);
          }
        });
        topicParticipantManager.addPublisher(publisher);
        return publisher;
      }
    }
  }
}
