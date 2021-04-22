package org.ros.internal.node.response;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class ObjectResultFactory implements ResultFactory<Object> {

  @Override
  public Object newFromValue(Object value) {
    return value;
  }
}