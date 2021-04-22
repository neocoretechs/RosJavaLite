
package org.ros.internal.node.response;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class BooleanResultFactory implements ResultFactory<Boolean> {
  
  @Override
  public Boolean newFromValue(Object value) {
    return (Boolean) value;
  }
}