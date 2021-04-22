package org.ros.internal.node.topic;

import org.ros.namespace.GraphName;

/**
 * Represents a ROS topic.
 * 
 * @see <a href="http://www.ros.org/wiki/Topics">Topics documentation</a>
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface TopicParticipant {

  /**
   * @return the name of the subscribed topic
   */
  GraphName getTopicName();

  /**
   * @return the message type (e.g. "std_msgs/String")
   */
  String getTopicMessageType();
}