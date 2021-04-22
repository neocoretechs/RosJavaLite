package org.ros.internal.node.topic;

import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.namespace.GraphName;

/**
 * The identifier for a topic in a ROS graph.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class TopicIdentifier {
  
  private final GraphName name;
  
  public static TopicIdentifier forName(String name) {
    return new TopicIdentifier(GraphName.of(name));
  }

  public TopicIdentifier(GraphName name) {
    assert(name != null);
    assert(name.isGlobal());
    this.name = name;
  }
  
  public ConnectionHeader toConnectionHeader() {
    ConnectionHeader connectionHeader = new ConnectionHeader();
    connectionHeader.addField(ConnectionHeaderFields.TOPIC, name.toString());
    return connectionHeader;
  }

  public GraphName getName() {
    return name;
  }
  
  @Override
  public String toString() {
    return "TopicIdentifier<" + name + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TopicIdentifier other = (TopicIdentifier) obj;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    return true;
  }
}
