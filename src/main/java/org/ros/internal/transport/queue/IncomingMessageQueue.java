package org.ros.internal.transport.queue;

import org.ros.concurrent.CircularBlockingDeque;
import org.ros.internal.transport.tcp.NamedChannelHandler;

import org.ros.message.MessageListener;

import java.util.concurrent.ExecutorService;

/**
 * Created in default subscriber to handle the incoming messages.
 * Creates a MessageReceiver and a MessageDispatcher.
 * @author jg
 */
public class IncomingMessageQueue<T> {

  /**
   * The maximum number of incoming messages that will be queued.
   * <p>
   * This limit applies to dispatching {@link LazyMessage}s as they arrive over
   * the network. It is independent of {@link MessageDispatcher} queue
   * capacities specified by
   * {@link IncomingMessageQueue#addListener(MessageListener, int)} which are
   * consumed by user provided {@link MessageListener}s.
   */
  private static final int DEQUE_CAPACITY = 16384;

  private final MessageReceiver<T> messageReceiver;
  private final MessageDispatcher<T> messageDispatcher;

  public IncomingMessageQueue( ExecutorService executorService) {
    CircularBlockingDeque<T> lazyMessages =
        new CircularBlockingDeque<T>(DEQUE_CAPACITY);
    messageReceiver = new MessageReceiver<T>(lazyMessages);
    messageDispatcher = new MessageDispatcher<T>(lazyMessages, executorService);
    executorService.execute(messageDispatcher);
  }

  /**
   * @see MessageDispatcher#setLatchMode(boolean)
   */
  public void setLatchMode(boolean enabled) {
    messageDispatcher.setLatchMode(enabled);
  }

  /**
   * @see MessageDispatcher#getLatchMode()
   */
  public boolean getLatchMode() {
    return messageDispatcher.getLatchMode();
  }

  /**
   * @see MessageDispatcher#addListener(MessageListener, int)
   */
  public void addListener(final MessageListener<T> messageListener, int queueCapacity) {
    messageDispatcher.addListener(messageListener, queueCapacity);
  }

  public void shutdown() {
    messageDispatcher.cancel();
  }

  /**
   * @return a {@link NamedChannelHandler} that will receive messages and add
   *         them to the queue
   */
  public NamedChannelHandler getMessageReceiver() {
    return messageReceiver;
  }
}
