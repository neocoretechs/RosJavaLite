
package org.ros.node.parameter;

/**
 * Called when a subscribed parameter value changes.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface ParameterListener {
  
  /**
   * @param value the new parameter value
   */
  void onNewValue(Object value);

}
