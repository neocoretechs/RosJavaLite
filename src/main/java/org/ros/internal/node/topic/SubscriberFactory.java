package org.ros.internal.node.topic;

import org.ros.internal.node.server.NodeIdentifier;
import org.ros.namespace.GraphName;
import org.ros.node.topic.DefaultSubscriberListener;
import org.ros.node.topic.Subscriber;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A factory for {@link Subscriber} instances.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class SubscriberFactory {

  private final NodeIdentifier nodeIdentifier;
  private final TopicParticipantManager topicParticipantManager;
  private final ScheduledExecutorService executorService;
  private final Object mutex;

  public SubscriberFactory(NodeIdentifier nodeIdentifier, TopicParticipantManager topicParticipantManager, ScheduledExecutorService executorService) {
    this.nodeIdentifier = nodeIdentifier;
    this.topicParticipantManager = topicParticipantManager;
    this.executorService = executorService;
    mutex = new Object();
  }

  /**
   * Gets or creates a {@link Subscriber} instance. {@link Subscriber}s are
   * cached and reused per topic. When a new {@link Subscriber} is generated, it
   * is registered with the master.
   * 
   * @param <T>
   *          the message type associated with the new {@link Subscriber}
   * @param topicDeclaration
   *          {@link TopicDeclaration} that is subscribed to
   * @param messageDeserializer
   *          the {@link MessageDeserializer} to use for incoming messages
   * @return a new or cached {@link Subscriber} instance
 * @throws IOException 
   */
  @SuppressWarnings("unchecked")
  public <T> Subscriber<T> newOrExisting(TopicDeclaration topicDeclaration) throws IOException {
    synchronized (mutex) {
      GraphName topicName = topicDeclaration.getName();
      if (topicParticipantManager.hasSubscriber(topicName)) {
        return (DefaultSubscriber<T>) topicParticipantManager.getSubscriber(topicName);
      } else {
        DefaultSubscriber<T> subscriber =
            DefaultSubscriber.newDefault(nodeIdentifier, topicDeclaration, topicParticipantManager, executorService);
        subscriber.addSubscriberListener(new DefaultSubscriberListener<T>() {
          @Override
          public void onNewPublisher(Subscriber<T> subscriber, PublisherIdentifier publisherIdentifier) {
            topicParticipantManager.addSubscriberConnection((DefaultSubscriber<T>) subscriber, publisherIdentifier);
          }
          @Override
          public void onShutdown(Subscriber<T> subscriber) {
        	assert(subscriber != null );
            topicParticipantManager.removeSubscriber((DefaultSubscriber<T>) subscriber);
          }
        });
        topicParticipantManager.addSubscriber(subscriber);
        return subscriber;
      }
    }
  }
}
