package org.ros.internal.node.topic;

import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.namespace.GraphName;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class PublisherDeclaration {

  private final PublisherIdentifier publisherIdentifier;
  private final TopicDeclaration topicDeclaration;

  public static PublisherDeclaration newFromNodeIdentifier(NodeIdentifier nodeIdentifier,
      TopicDeclaration topicDeclaration) {
    assert(nodeIdentifier != null);
    assert(topicDeclaration != null);
    return new PublisherDeclaration(new PublisherIdentifier(nodeIdentifier,
        topicDeclaration.getIdentifier()), topicDeclaration);
  }

  public PublisherDeclaration(PublisherIdentifier publisherIdentifier,
      TopicDeclaration topicDeclaration) {
    assert(publisherIdentifier != null);
    assert(topicDeclaration != null);
    assert(publisherIdentifier.getTopicIdentifier().equals( topicDeclaration.getIdentifier()));
    this.publisherIdentifier = publisherIdentifier;
    this.topicDeclaration = topicDeclaration;
  }
  
  public ConnectionHeader toConnectionHeader() {
    ConnectionHeader connectionHeader = publisherIdentifier.toConnectionHeader();
    connectionHeader.merge(topicDeclaration.toConnectionHeader());
    return connectionHeader;
  }

  public NodeIdentifier getSlaveIdentifier() {
    return publisherIdentifier.getNodeIdentifier();
  }

  public GraphName getSlaveName() {
    return publisherIdentifier.getNodeIdentifier().getName();
  }

  public InetSocketAddress getSlaveUri() {
    return publisherIdentifier.getNodeUri();
  }

  public GraphName getTopicName() {
    return topicDeclaration.getName();
  }

  public String getTopicMessageType() {
    return topicDeclaration.getMessageType();
  }

  @Override
  public String toString() {
    return "PublisherDefinition<" + publisherIdentifier + ", " + topicDeclaration + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((publisherIdentifier == null) ? 0 : publisherIdentifier.hashCode());
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
    PublisherDeclaration other = (PublisherDeclaration) obj;
    if (publisherIdentifier == null) {
      if (other.publisherIdentifier != null)
        return false;
    } else if (!publisherIdentifier.equals(other.publisherIdentifier))
      return false;
    if (topicDeclaration == null) {
      if (other.topicDeclaration != null)
        return false;
    } else if (!topicDeclaration.equals(other.topicDeclaration))
      return false;
    return true;
  }
}
