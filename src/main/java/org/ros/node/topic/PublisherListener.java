package org.ros.node.topic;

import org.ros.internal.node.RegistrantListener;
import org.ros.internal.node.topic.SubscriberIdentifier;

/**
 * A lifecycle listener for {@link Publisher} instances.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface PublisherListener<T> extends RegistrantListener<Publisher<T>> {

  /**
   * A {@link Subscriber} has connected to the {@link Publisher}.
   * 
   * @param publisher
   *          the {@link Publisher} that the {@link Subscriber} connected to
   * @param subscriberIdentifier
   *          the {@link SubscriberIdentifier} of the new {@link Subscriber}
   */
  void onNewSubscriber(Publisher<T> publisher, SubscriberIdentifier subscriberIdentifier);

  /**
   * The {@link Publisher} has been shut down.
   * 
   * @param publisher
   *          the {@link Publisher} that was shut down
   */
  void onShutdown(Publisher<T> publisher);
}
