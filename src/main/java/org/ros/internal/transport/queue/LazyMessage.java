/*
 * Copyright (C) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.transport.queue;

import io.netty.buffer.ByteBuf;

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

  private final ByteBuf buffer;
  private final Object mutex;

  private T message;

  /**
   * @param buffer
   *          the {@link ChannelBuffer} to be lazily deserialized
   * @param deserializer
   *          the {@link MessageDeserializer} to use
   */
  public LazyMessage(ByteBuf buffer) {
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