package org.ros.internal.node.topic;

import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.namespace.GraphName;

import java.net.InetSocketAddress;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class SubscriberIdentifier {

  private final NodeIdentifier nodeIdentifier;
  private final TopicIdentifier topicIdentifier;

  public SubscriberIdentifier(NodeIdentifier nodeIdentifier, TopicIdentifier topicIdentifier) {
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

  public InetSocketAddress getUri() {
    return nodeIdentifier.getUri();
  }

  public TopicIdentifier getTopicIdentifier() {
    return topicIdentifier;
  }

  public GraphName getTopicName() {
    return topicIdentifier.getName();
  }

  @Override
  public String toString() {
    return "SubscriberIdentifier<" + nodeIdentifier + ", " + topicIdentifier + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((nodeIdentifier == null) ? 0 : nodeIdentifier.hashCode());
    result = prime * result + ((topicIdentifier == null) ? 0 : topicIdentifier.hashCode());
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
    SubscriberIdentifier other = (SubscriberIdentifier) obj;
    if (nodeIdentifier == null) {
      if (other.nodeIdentifier != null)
        return false;
    } else { // node id != null
    	if( other.nodeIdentifier == null )
    		return false;
    	if (!nodeIdentifier.equals(other.nodeIdentifier))
    			return false;
    }
    if (topicIdentifier == null) {
      if (other.topicIdentifier != null)
        return false;
    } else { // topicId != null
    	if( other.topicIdentifier == null )
    		return false;
    	if (!topicIdentifier.equals(other.topicIdentifier))
    		return false;
    }
    return true;
  }
}
