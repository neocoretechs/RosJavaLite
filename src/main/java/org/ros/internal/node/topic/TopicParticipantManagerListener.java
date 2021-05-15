package org.ros.internal.node.topic;

import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

/**
 * Listener for {@link TopicParticipantManager} events.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface TopicParticipantManagerListener {

  /**
   * Called when a new {@link Publisher} is added.
   * 
   * @param publisher
   *          the {@link Publisher} that was added
   */
  void onPublisherAdded(DefaultPublisher<?> publisher);

  /**
   * Called when a new {@link Publisher} is removed.
   * 
   * @param publisher
   *          the {@link Publisher} that was removed
   */
  void onPublisherRemoved(DefaultPublisher<?> publisher);

  /**
   * Called when a {@link Subscriber} is added.
   * 
   * @param subscriber
   *          the {@link Subscriber} that was added
   */
  void onSubscriberAdded(DefaultSubscriber<?> subscriber);

  /**
   * Called when a {@link Subscriber} is removed.
   * 
   * @param subscriber the {@link Subscriber} that was removed
   */
  void onSubscriberRemoved(DefaultSubscriber<?> subscriber);

  /**
   * Called when a {@link Subscriber} is removed for shutdown.
   * 
   * @param subscriber the {@link Subscriber} that was removed
   */
  void onSubscriberRemoved(DefaultSubscriber<?> subscriber, boolean remove);
}
