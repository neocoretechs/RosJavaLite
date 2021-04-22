package org.ros.internal.node.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.CancellableLoop;
import org.ros.node.topic.Publisher;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Repeatedly send a message out on a given {@link Publisher}.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class RepeatingPublisher<MessageType> {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(RepeatingPublisher.class);

  private final Publisher<MessageType> publisher;
  private final MessageType message;
  private final int frequency;
  private final RepeatingPublisherLoop runnable;

  /**
   * Executor used to run the {@link RepeatingPublisherLoop}.
   */
  private final ScheduledExecutorService executorService;

  private final class RepeatingPublisherLoop extends CancellableLoop {
    @Override
    public void loop() throws InterruptedException {
      publisher.publish(message);
      if (DEBUG) {
        log.info(String.format("Published message %s to publisher %s ", message, publisher));
      }
      Thread.sleep((long) (1000.0d / frequency));
    }
  }

  /**
   * @param publisher
   * @param message
   * @param frequency
   *          the frequency of publication in Hz
   */
  public RepeatingPublisher(Publisher<MessageType> publisher, MessageType message, int frequency,
      ScheduledExecutorService executorService) {
    this.publisher = publisher;
    this.message = message;
    this.frequency = frequency;
    this.executorService = executorService;
    runnable = new RepeatingPublisherLoop();
  }

  public void start() {
    assert(!runnable.isRunning());
    executorService.execute(runnable);
  }

  public void cancel() {
    assert(runnable.isRunning());
    runnable.cancel();
  }
}
