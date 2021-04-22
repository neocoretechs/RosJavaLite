package org.ros.node.topic;

import org.ros.internal.node.RegistrantListener;
import org.ros.internal.node.topic.PublisherIdentifier;

/**
 * A lifecycle listener for {@link Subscriber} instances.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface SubscriberListener<T> extends RegistrantListener<Subscriber<T>> {

  /**
   * A new {@link Publisher} has connected to the {@link Subscriber}.
   * 
   * @param subscriber the {@link Subscriber} that the {@link Publisher} connected to
   * @param publisherIdentifier the {@link PublisherIdentifier} of the new {@link Publisher}
   */
  void onNewPublisher(Subscriber<T> subscriber, PublisherIdentifier publisherIdentifier);

  /**
   * The {@link Subscriber} has been shut down.
   * 
   * @param subscriber the {@link Subscriber} that was shut down
   */
  void onShutdown(Subscriber<T> subscriber);
}
