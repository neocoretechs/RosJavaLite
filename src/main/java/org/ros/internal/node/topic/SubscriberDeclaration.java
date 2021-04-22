package org.ros.internal.node.topic;

import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.namespace.GraphName;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class SubscriberDeclaration {

  private final SubscriberIdentifier subscriberIdentifier;
  private final TopicDeclaration topicDeclaration;

  /**
   * Creates a subscriber definition from the headers in a connection header.
   * 
   * @param header The header data. 
   * @return The subscriber definition from the header data.
   */
  public static SubscriberDeclaration newFromHeader(Map<String, String> header) {
    NodeIdentifier nodeIdentifier =
        new NodeIdentifier(GraphName.of(header.get(ConnectionHeaderFields.CALLER_ID)), null);
    TopicDeclaration topicDeclaration = TopicDeclaration.newFromHeader(header);
    return new SubscriberDeclaration(new SubscriberIdentifier(nodeIdentifier,
        topicDeclaration.getIdentifier()), topicDeclaration);
  }

  public SubscriberDeclaration(SubscriberIdentifier subscriberIdentifier,
      TopicDeclaration topicDeclaration) {
    this.subscriberIdentifier = subscriberIdentifier;
    this.topicDeclaration = topicDeclaration;
  }

  public NodeIdentifier getNodeIdentifier() {
    return subscriberIdentifier.getNodeIdentifier();
  }

  public InetSocketAddress getSlaveUri() {
    return subscriberIdentifier.getUri();
  }

  public GraphName getTopicName() {
    return topicDeclaration.getName();
  }

  public ConnectionHeader toConnectionHeader() {
    ConnectionHeader connectionHeader = new ConnectionHeader();
    connectionHeader.merge(subscriberIdentifier.toConnectionHeader());
    connectionHeader.merge(topicDeclaration.toConnectionHeader());
    return connectionHeader;
  }

  @Override
  public String toString() {
    return "SubscriberDefinition<" + subscriberIdentifier + ", " + topicDeclaration + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((subscriberIdentifier == null) ? 0 : subscriberIdentifier.hashCode());
    result = prime * result + ((topicDeclaration == null) ? 0 : topicDeclaration.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SubscriberDeclaration other = (SubscriberDeclaration) obj;
    if (subscriberIdentifier == null) {
      if (other.subscriberIdentifier != null)
        return false;
    } else if (!subscriberIdentifier.equals(other.subscriberIdentifier))
      return false;
    if (topicDeclaration == null) {
      if (other.topicDeclaration != null)
        return false;
    } else if (!topicDeclaration.equals(other.topicDeclaration))
      return false;
    return true;
  }
}
