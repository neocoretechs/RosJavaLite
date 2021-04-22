
package org.ros.internal.node.response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class StringListResultFactory implements ResultFactory<List<String>> {

  @Override
  public List<String> newFromValue(Object value) {
    List<String> strings = new ArrayList<String>();
    List<Object> objects = Arrays.asList((Object[]) value);
    for (Object topic : objects) {
      strings.add((String) topic);
    }
    return strings;
  }
}