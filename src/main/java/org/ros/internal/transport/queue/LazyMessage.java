package org.ros.internal.transport.queue;

import java.nio.ByteBuffer;

//import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.internal.system.Utility;

/**
 * Lazily deserializes a message on the first call to {@link #get()} and caches
 * the result.
 * <p>
 * This class is thread-safe.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 * 
 * @param <T>
 *          the message type
 */
public class LazyMessage<T> {

  private final ByteBuffer buffer;
  private final Object mutex;

  private T message;

  /**
   * @param buffer
   *          the buffer to be lazily deserialized
   * @param deserializer
   *          the {@link MessageDeserializer} to use
   */
  public LazyMessage(ByteBuffer buffer) {
    this.buffer = buffer;
    mutex = new Object();
  }


  LazyMessage(T message) {
	mutex = new Object();
	this.buffer = null;
    this.message = message;
  }

  /**
   * @return the deserialized message
   */
  public T get() {
    synchronized (mutex) {
      if (message != null) {
        return message;
      }
      message = (T) Utility.deserialize(buffer);
    }
    return message;
  }
}