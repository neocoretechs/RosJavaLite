package org.ros.internal.node.response;

import org.ros.internal.message.topic.TopicDescription;
import org.ros.internal.node.topic.TopicDeclaration;
import org.ros.namespace.GraphName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link ResultFactory} to take an object and turn it into a list of
 * {@link TopicDeclaration} instances.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class TopicListResultFactory implements ResultFactory<List<TopicDeclaration>> {

  @Override
  public List<TopicDeclaration> newFromValue(Object value) {
    List<TopicDeclaration> descriptions = new ArrayList<TopicDeclaration>();
    List<Object> topics = Arrays.asList((Object[]) value);
    for (Object topic : topics) {
      String name = (String) ((Object[]) topic)[0];
      String type = (String) ((Object[]) topic)[1];
      descriptions.add(TopicDeclaration.newFromTopicName(GraphName.of(name), new TopicDescription(type, null,
          null)));
    }
    return descriptions;
  }
}
