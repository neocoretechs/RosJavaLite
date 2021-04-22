package org.ros.internal.node.response;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class IntegerResultFactory implements ResultFactory<Integer> {
  
  @Override
  public Integer newFromValue(Object value) {
    return (Integer) value;
  }
}