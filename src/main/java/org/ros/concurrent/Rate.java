
package org.ros.concurrent;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface Rate {

  /**
   * Sleeps until the configured rate is achieved.
   */
  void sleep();
}
