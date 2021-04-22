package org.ros.internal.node.topic;

import org.ros.internal.message.topic.TopicDescription;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.namespace.GraphName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A topic in a ROS graph.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class TopicDeclaration {

  private final TopicIdentifier topicIdentifier;
  private final TopicDescription topicDescription;

  /**
   * @param header
   *          a {@link Map} of header fields
   * @return a new {@link TopicDeclaration} from the given header
   */
  public static TopicDeclaration newFromHeader(Map<String, String> header) {
    assert(header.containsKey(ConnectionHeaderFields.TOPIC));
    GraphName name = GraphName.of(header.get(ConnectionHeaderFields.TOPIC));
    String type = header.get(ConnectionHeaderFields.TYPE);
    String definition = header.get(ConnectionHeaderFields.MESSAGE_DEFINITION);
    String md5Checksum = header.get(ConnectionHeaderFields.MD5_CHECKSUM);
    TopicDescription topicDescription = new TopicDescription(type, definition, md5Checksum);
    return new TopicDeclaration(new TopicIdentifier(name), topicDescription);
  }

  public static TopicDeclaration newFromTopicName(GraphName topicName,
      TopicDescription topicDescription) {
    return new TopicDeclaration(new TopicIdentifier(topicName), topicDescription);
  }

  public TopicDeclaration(TopicIdentifier topicIdentifier, TopicDescription topicDescription) {
    assert(topicIdentifier != null);
    assert(topicDescription != null);
    this.topicIdentifier = topicIdentifier;
    this.topicDescription = topicDescription;
  }

  public TopicIdentifier getIdentifier() {
    return topicIdentifier;
  }

  public GraphName getName() {
    return topicIdentifier.getName();
  }

  public String getMessageType() {
    return topicDescription.getType();
  }

  public ConnectionHeader toConnectionHeader() {
    ConnectionHeader connectionHeader = new ConnectionHeader();
    connectionHeader.merge(topicIdentifier.toConnectionHeader());
    connectionHeader.addField(ConnectionHeaderFields.TYPE, topicDescription.getType());
    connectionHeader.addField(ConnectionHeaderFields.MESSAGE_DEFINITION,
        topicDescription.getDefinition());
    connectionHeader.addField(ConnectionHeaderFields.MD5_CHECKSUM,
        topicDescription.getMd5Checksum());
    return connectionHeader;
  }

  public List<String> toList() {
    ArrayList<String> l1 = new ArrayList<String>();
    l1.add(getName().toString());
    l1.add(getMessageType());
    return l1;
  }

  @Override
  public String toString() {
    return "Topic<" + topicIdentifier + ", " + topicDescription.toString() + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((topicDescription == null) ? 0 : topicDescription.hashCode());
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
    TopicDeclaration other = (TopicDeclaration) obj;
    if (topicDescription == null) {
      if (other.topicDescription != null)
        return false;
    } else if (!topicDescription.equals(other.topicDescription))
      return false;
    if (topicIdentifier == null) {
      if (other.topicIdentifier != null)
        return false;
    } else if (!topicIdentifier.equals(other.topicIdentifier))
      return false;
    return true;
  }
}
