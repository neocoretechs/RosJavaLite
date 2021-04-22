package org.ros.internal.node.response;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class StringResultFactory implements ResultFactory<String> {
  
  @Override
  public String newFromValue(Object value) {
    return (String) value;
  }
}