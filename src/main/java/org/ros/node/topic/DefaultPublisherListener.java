package org.ros.node.topic;

import org.ros.internal.node.topic.SubscriberIdentifier;

/**
 * A {@link PublisherListener} which provides empty defaults for all signals.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class DefaultPublisherListener<T> implements PublisherListener<T> {

  @Override
  public void onMasterRegistrationSuccess(Publisher<T> publisher) {
  }

  @Override
  public void onMasterRegistrationFailure(Publisher<T> publisher) {
  }

  @Override
  public void onMasterUnregistrationSuccess(Publisher<T> publisher) {
  }

  @Override
  public void onMasterUnregistrationFailure(Publisher<T> publisher) {
  }

  @Override
  public void onNewSubscriber(Publisher<T> publisher, SubscriberIdentifier subscriberIdentifier) {
  }

  @Override
  public void onShutdown(Publisher<T> publisher) {
  }
}
