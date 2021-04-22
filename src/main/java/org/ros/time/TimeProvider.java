package org.ros.time;

import org.ros.message.Time;

/**
 * Provide time.
 *
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface TimeProvider {

  /**
   * @return the current time of the system using rostime
   */
  Time getCurrentTime();

}