package org.ros.concurrent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link WatchdogTimer} expects to receive a {@link #pulse()} at least once
 * every {@link #period} {@link #unit}s. Once per every period in which a
 * {@link #pulse()} is not received, the provided {@link Runnable} will be
 * executed.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class WatchdogTimer {

  private final ScheduledExecutorService scheduledExecutorService;
  private final long period;
  private final TimeUnit unit;
  private final Runnable runnable;

  private boolean pulsed;
  private ScheduledFuture<?> scheduledFuture;

  public WatchdogTimer(ScheduledExecutorService scheduledExecutorService, long period,
      TimeUnit unit, final Runnable runnable) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.period = period;
    this.unit = unit;
    this.runnable = new Runnable() {
      @Override
      public void run() {
        try {
          if (!pulsed) {
            runnable.run();
          }
        } finally {
          pulsed = false;
        }
      }
    };
    pulsed = false;
  }

  public void start() {
    scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(runnable, period, period, unit);
  }

  public void pulse() {
    pulsed = true;
  }

  public void cancel() {
    scheduledFuture.cancel(true);
  }
}
