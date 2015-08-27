/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.node.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.namespace.GraphName;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a collection of {@link Publisher}s and {@link Subscriber}s.
 * 
 * @author kwc@willowgarage.com (Ken Conley)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TopicParticipantManager {

	private static Log log = LogFactory.getLog(TopicParticipantManager.class);
  /**
   * A mapping from topic name to {@link Subscriber}.
   */
  private final Map<GraphName, DefaultSubscriber<?>> subscribers;

  /**
   * A mapping from topic name to {@link Publisher}.
   */
  private final Map<GraphName, DefaultPublisher<?>> publishers;

  /**
   * A mapping from {@link Subscriber} to its connected
   * {@link PublisherIdentifier}s.
   */
  private final HashMap<DefaultSubscriber<?>, List<PublisherIdentifier>> subscriberConnections;

  /**
   * A mapping from {@link Publisher} to its connected
   * {@link SubscriberIdentifier}s.
   */
  private final HashMap<DefaultPublisher<?>,List<SubscriberIdentifier>> publisherConnections;

  // TODO(damonkohler): Change to ListenerGroup.
  private TopicParticipantManagerListener listener;

  public TopicParticipantManager() {
    publishers = new ConcurrentHashMap<GraphName, DefaultPublisher<?>>();
    subscribers = new ConcurrentHashMap<GraphName, DefaultSubscriber<?>>();
    subscriberConnections = new HashMap<DefaultSubscriber<?>, List<PublisherIdentifier>>();
    publisherConnections = new HashMap<DefaultPublisher<?>, List<SubscriberIdentifier>>();
  }

  public void setListener(TopicParticipantManagerListener listener) {
    this.listener = listener;
  }

  public boolean hasSubscriber(GraphName topicName) {
    return subscribers.containsKey(topicName);
  }

  public boolean hasPublisher(GraphName topicName) {
    return publishers.containsKey(topicName);
  }

  public DefaultPublisher<?> getPublisher(GraphName topicName) {
    return publishers.get(topicName);
  }

  public DefaultSubscriber<?> getSubscriber(GraphName topicName) {
    return subscribers.get(topicName);
  }

  public void addPublisher(DefaultPublisher<?> publisher) {
    publishers.put(publisher.getTopicName(), publisher);
    if (listener != null) {
      listener.onPublisherAdded(publisher);
    }
  }

  public void removePublisher(DefaultPublisher<?> publisher) {
    publishers.remove(publisher.getTopicName());
    if (listener != null) {
      listener.onPublisherRemoved(publisher);
    }
  }

  public void addSubscriber(DefaultSubscriber<?> subscriber) {
    subscribers.put(subscriber.getTopicName(), subscriber);
    if (listener != null) {
      listener.onSubscriberAdded(subscriber);
    }
  }

  public void removeSubscriber(DefaultSubscriber<?> subscriber) {
    subscribers.remove(subscriber.getTopicName());
    if (listener != null) {
      listener.onSubscriberRemoved(subscriber);
    }
  }

  public void addSubscriberConnection(DefaultSubscriber<?> subscriber, PublisherIdentifier publisherIdentifier) {
	  //log.info("sub:"+subscriber+" pub:"+publisherIdentifier);
	  List<PublisherIdentifier> pubs = subscriberConnections.get(subscriber);
	  //for(PublisherIdentifier p: pubs) log.info("Pub: "+p);
	  assert(pubs != null);
	  if( pubs.contains(publisherIdentifier))
		  return;
	  pubs.add(publisherIdentifier);
  }

  public void removeSubscriberConnection(DefaultSubscriber<?> subscriber, PublisherIdentifier publisherIdentifier) {
	  List<PublisherIdentifier> pubs = subscriberConnections.get(subscriber);
	  assert(pubs != null);
	  pubs.remove(publisherIdentifier);
  }

  public void addPublisherConnection(DefaultPublisher<?> publisher, SubscriberIdentifier subscriberIdentifier) {
	  List<SubscriberIdentifier> subs = publisherConnections.get(publisher);
	  assert(subs != null);
	  if( subs.contains(subscriberIdentifier) )
		  return;
	  subs.add(subscriberIdentifier);
  }

  public void removePublisherConnection(DefaultPublisher<?> publisher, SubscriberIdentifier subscriberIdentifier) {
	  List<SubscriberIdentifier> subs = publisherConnections.get(publisher);
	  assert(subs != null);
	  subs.remove(subscriberIdentifier);
  }

  public Collection<DefaultSubscriber<?>> getSubscribers() {
    return subscribers.values();
  }

  public Collection<PublisherIdentifier> getSubscriberConnections(DefaultSubscriber<?> subscriber) {
    return subscriberConnections.get(subscriber);
  }

  public Collection<DefaultPublisher<?>> getPublishers() {
    return publishers.values();
  }

  public Collection<SubscriberIdentifier> getPublisherConnections(DefaultPublisher<?> publisher) {
    return publisherConnections.get(publisher);
  }
}
