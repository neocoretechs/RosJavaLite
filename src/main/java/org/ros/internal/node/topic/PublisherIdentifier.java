package org.ros.internal.node.topic;

import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * All information needed to identify a publisher.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class PublisherIdentifier {

  private final NodeIdentifier nodeIdentifier;
  private final TopicIdentifier topicIdentifier;

  public static Collection<PublisherIdentifier> newCollectionFromUris(
      Collection<InetSocketAddress> publisherUris, TopicDeclaration topicDeclaration) {
    Set<PublisherIdentifier> publishers = new HashSet<PublisherIdentifier>();
    for (InetSocketAddress uri : publisherUris) {
      NodeIdentifier nodeIdentifier = new NodeIdentifier(null, uri);
      publishers.add(new PublisherIdentifier(nodeIdentifier, topicDeclaration.getIdentifier()));
    }
    return publishers;
  }

  public static PublisherIdentifier newFromStrings(String nodeName, String uri, String port, String topicName) {
    return new PublisherIdentifier(NodeIdentifier.forNameAndUri(nodeName, uri, Integer.valueOf(port)),
        TopicIdentifier.forName(topicName));
  }

  public PublisherIdentifier(NodeIdentifier nodeIdentifier, TopicIdentifier topicIdentifier) {
    assert(nodeIdentifier != null);
    assert(topicIdentifier != null);
    this.nodeIdentifier = nodeIdentifier;
    this.topicIdentifier = topicIdentifier;
  }

  public ConnectionHeader toConnectionHeader() {
    ConnectionHeader connectionHeader = new ConnectionHeader();
    connectionHeader.merge(nodeIdentifier.toConnectionHeader());
    connectionHeader.merge(topicIdentifier.toConnectionHeader());
    return connectionHeader;
  }

  public NodeIdentifier getNodeIdentifier() {
    return nodeIdentifier;
  }

  /**
   * @return the {@link GraphName} of the {@link Node} hosting this {@link Publisher}
   */
  public GraphName getNodeName() {
    return nodeIdentifier.getName();
  }

  /**
   * @return the {@link InetAddress} of the {@link Node} hosting this {@link Publisher}
   */
  public InetSocketAddress getNodeUri() {
    return nodeIdentifier.getUri();
  }

  /**
   * @return the {@link TopicIdentifier} for the {@link Publisher}'s topic
   */
  public TopicIdentifier getTopicIdentifier() {
    return topicIdentifier;
  }

  /**
   * @return the {@link GraphName} of this {@link Publisher}'s topic
   */
  public GraphName getTopicName() {
    return topicIdentifier.getName();
  }

  @Override
  public String toString() {
    return "PublisherIdentifier<" + nodeIdentifier + ", " + topicIdentifier + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + nodeIdentifier.hashCode();
    result = prime * result + topicIdentifier.hashCode();
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
    PublisherIdentifier other = (PublisherIdentifier) obj;
    if (!nodeIdentifier.equals(other.nodeIdentifier))
      return false;
    if (!topicIdentifier.equals(other.topicIdentifier))
      return false;
    return true;
  }
}
