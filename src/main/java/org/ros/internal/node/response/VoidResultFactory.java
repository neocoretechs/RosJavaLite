package org.ros.internal.node.response;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class VoidResultFactory implements ResultFactory<Void> {
  
  @Override
  public Void newFromValue(Object value) {
    return null;
  }
}