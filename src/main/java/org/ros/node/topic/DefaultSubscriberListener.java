package org.ros.node.topic;

import org.ros.internal.node.topic.PublisherIdentifier;

/**
 * A {@link SubscriberListener} which provides empty defaults for all signals.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class DefaultSubscriberListener<T> implements SubscriberListener<T> {

  @Override
  public void onMasterRegistrationSuccess(Subscriber<T> subscriber) {
  }

  @Override
  public void onMasterRegistrationFailure(Subscriber<T> subscriber) {
  }

  @Override
  public void onMasterUnregistrationSuccess(Subscriber<T> subscriber) {
  }

  @Override
  public void onMasterUnregistrationFailure(Subscriber<T> subscriber) {
  }

  @Override
  public void onNewPublisher(Subscriber<T> subscriber, PublisherIdentifier publisherIdentifier) {
  }

  @Override
  public void onShutdown(Subscriber<T> subscriber) {
  }
}
