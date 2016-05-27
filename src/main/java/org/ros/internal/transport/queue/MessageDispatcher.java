package org.ros.internal.transport.queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.CancellableLoop;
import org.ros.concurrent.CircularBlockingDeque;
import org.ros.concurrent.EventDispatcher;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.message.MessageListener;

import java.util.concurrent.ExecutorService;

/**
 * @author jg
 * 
 * @param <T>
 *          the message type
 */
public class MessageDispatcher<T> extends CancellableLoop {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(MessageDispatcher.class);

  private final CircularBlockingDeque<T> lazyMessages;
  private final ListenerGroup<MessageListener<T>> messageListeners;

  /**
   * Ensures that messages are not dispatched twice when adding a listener
   * while latch mode is enabled.
   */
  private final Object mutex;

  private boolean latchMode;
  private T latchedMessage;

  public MessageDispatcher(CircularBlockingDeque<T> lazyMessages,
      ExecutorService executorService) {
    this.lazyMessages = lazyMessages;
    messageListeners = new ListenerGroup<MessageListener<T>>(executorService);
    mutex = new Object();
    latchMode = false;
  }

  /**
   * Adds the specified {@link MessageListener} to the internal
   * {@link ListenerGroup}. If {@link #latchMode} is {@code true}, the
   * {@link #latchedMessage} will be immediately dispatched to the specified
   * {@link MessageListener}.
   * 
   * @see ListenerGroup#add(Object, int)
   */
  public void addListener(MessageListener<T> messageListener, int limit) {
    if (DEBUG) {
      log.info("Adding listener.");
    }
    synchronized (mutex) {
      EventDispatcher<MessageListener<T>> eventDispatcher =
          messageListeners.add(messageListener, limit);
      if (latchMode && latchedMessage != null) {
        eventDispatcher.signal(newSignalRunnable(latchedMessage));
      }
    }
  }

  /**
   * Returns a newly allocated {@link SignalRunnable} for the specified
   * {@link LazyMessage}.
   * 
   * @param lazyMessage
   *          the {@link LazyMessage} to signal {@link MessageListener}s with
   * @return the newly allocated {@link SignalRunnable}
   */
  private SignalRunnable<MessageListener<T>> newSignalRunnable(final T lazyMessage) {
    return new SignalRunnable<MessageListener<T>>() {
      @Override
      public void run(MessageListener<T> messageListener) {
        messageListener.onNewMessage(lazyMessage);
      }
    };
  }

  /**
   * @param enabled
   *          {@code true} if latch mode should be enabled, {@code false}
   *          otherwise
   */
  public void setLatchMode(boolean enabled) {
    latchMode = enabled;
  }

  /**
   * @return {@code true} if latch mode is enabled, {@code false} otherwise
   */
  public boolean getLatchMode() {
    return latchMode;
  }

  @Override
  public void loop() throws InterruptedException {
    T lazyMessage = lazyMessages.takeFirst();
    synchronized (mutex) {
      latchedMessage = lazyMessage;
      if (DEBUG) {
        log.info("Dispatching message: " + latchedMessage);
      }
      messageListeners.signal(newSignalRunnable(latchedMessage));
    }
  }

  @Override
  protected void handleInterruptedException(InterruptedException e) {
    messageListeners.shutdown();
  }
}