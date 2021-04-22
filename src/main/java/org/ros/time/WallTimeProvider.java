package org.ros.time;

import org.ros.message.Time;

/**
 * Provides wallclock (actual) time.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class WallTimeProvider implements TimeProvider {

  @Override
  public Time getCurrentTime() {
    return Time.fromMillis(System.currentTimeMillis());
  }

}
