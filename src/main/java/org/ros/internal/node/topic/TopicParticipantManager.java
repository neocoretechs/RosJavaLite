package org.ros.internal.node.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.namespace.GraphName;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.ArrayList;
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
 * @author jg
 */
public class TopicParticipantManager {
	private static boolean DEBUG = true;
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
  private final Map<DefaultSubscriber<?>, List<PublisherIdentifier>> subscriberConnections;

  /**
   * A mapping from {@link Publisher} to its connected
   * {@link SubscriberIdentifier}s.
   */
  private final Map<DefaultPublisher<?>,List<SubscriberIdentifier>> publisherConnections;

  // TODO(damonkohler): Change to ListenerGroup.
  private TopicParticipantManagerListener listener;

  public TopicParticipantManager() {
    publishers = new ConcurrentHashMap<GraphName, DefaultPublisher<?>>();
    subscribers = new ConcurrentHashMap<GraphName, DefaultSubscriber<?>>();
    subscriberConnections = new ConcurrentHashMap<DefaultSubscriber<?>, List<PublisherIdentifier>>();
    publisherConnections = new ConcurrentHashMap<DefaultPublisher<?>, List<SubscriberIdentifier>>();
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
	  if(DEBUG)
		  log.info("Adding publisher:"+publisher+" to "+this);
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
	  if(DEBUG)
		  log.info("Adding subscriber:"+subscriber+" to "+this);
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
	  if(DEBUG)
		  log.info("Connecting subscriber:"+subscriber+" to publisher Identifier:"+publisherIdentifier+" for "+this);
	  List<PublisherIdentifier> pubs = subscriberConnections.get(subscriber);
	  //for(PublisherIdentifier p: pubs) log.info("Pub: "+p);
	  if( pubs == null ) {
		  pubs = new ArrayList<PublisherIdentifier>();
		  pubs.add(publisherIdentifier);
		  subscriberConnections.put(subscriber,  pubs);
	  } else {
		  if( pubs.contains(publisherIdentifier))
			  return;
		  pubs.add(publisherIdentifier);
	  }
  }

  public void removeSubscriberConnection(DefaultSubscriber<?> subscriber, PublisherIdentifier publisherIdentifier) {
	  List<PublisherIdentifier> pubs = subscriberConnections.get(subscriber);
	  if(pubs != null);
	  	pubs.remove(publisherIdentifier);
  }

  public void addPublisherConnection(DefaultPublisher<?> publisher, SubscriberIdentifier subscriberIdentifier) {
	  if(DEBUG)
		  log.info("Connecting publisher:"+publisher+" to subscriber Identifier:"+subscriberIdentifier+" for "+this);
	  List<SubscriberIdentifier> subs = publisherConnections.get(publisher);
	  if( subs == null) {
		  subs = new ArrayList<SubscriberIdentifier>();
		  subs.add(subscriberIdentifier);
		  publisherConnections.put(publisher, subs);
	  } else {
		  if( subs.contains(subscriberIdentifier) )
			  return;
		  subs.add(subscriberIdentifier);
	  }
  }

  public void removePublisherConnection(DefaultPublisher<?> publisher, SubscriberIdentifier subscriberIdentifier) {
	  List<SubscriberIdentifier> subs = publisherConnections.get(publisher);
	  if(subs != null);
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
