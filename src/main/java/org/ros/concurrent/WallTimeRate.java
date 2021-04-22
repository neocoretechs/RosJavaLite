package org.ros.concurrent;


/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class WallTimeRate implements Rate {

  private final long delay;

  private long time;

  public WallTimeRate(int hz) {
    delay = 1000 / hz;
    time = 0;
  }

  @Override
  public void sleep() {
    long delta = System.currentTimeMillis() - time;
    while (delta < delay) {
      try {
        Thread.sleep(delay - delta);
      } catch (InterruptedException e) {
        break;
      }
      delta = System.currentTimeMillis() - time;
    }
    time = System.currentTimeMillis();
  }
}
