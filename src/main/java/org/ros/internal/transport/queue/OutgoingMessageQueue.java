package org.ros.internal.transport.queue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.group.ChannelGroup;
//import org.jboss.netty.channel.group.ChannelGroupFuture;
//import org.jboss.netty.channel.group.ChannelGroupFutureListener;
//import org.jboss.netty.channel.group.DefaultChannelGroup;

import org.ros.concurrent.CancellableLoop;
import org.ros.concurrent.CircularBlockingDeque;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.MessageBufferPool;
import org.ros.internal.message.MessageBuffers;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandlerContext;



/**
 */
public class OutgoingMessageQueue<T> {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(OutgoingMessageQueue.class);

  private static final int DEQUE_CAPACITY = 16;

  private final CircularBlockingDeque<T> deque;
  private final AsynchronousChannelGroup channelGroup;
  private final Writer writer;
  private final MessageBufferPool messageBufferPool;
  private final ByteBuffer latchedBuffer;
  private final Object mutex;

  private boolean latchMode;
  private T latchedMessage;
  
  private List<ChannelHandlerContext> channels;

  private final class Writer extends CancellableLoop {
    @Override
    public void loop() throws InterruptedException {
      T message = deque.takeFirst();
      final ByteBuffer buffer = messageBufferPool.acquire();
      Utility.serialize(message, buffer);
      if (DEBUG  ) {
        log.info(String.format("Writing %d bytes.", buffer.limit()));
      }
      // we have to wait until the write
      // operation is complete before returning the buffer to the pool.
      Iterator<ChannelHandlerContext> it = channels.iterator();
      while(it.hasNext()) {
    	  ChannelHandlerContext ctx = it.next();
    	  ctx.write(buffer, new CompletionHandler<Integer, Void>() {
        @Override
        public void completed(Integer a, Void b) {
          messageBufferPool.release(buffer);
        }
		@Override
		public void failed(Throwable arg0, Void arg1) {
			log.error("Failed write");
			throw new RosRuntimeException(arg0);
		}
      });
      }
    }
  }

  public OutgoingMessageQueue(ExecutorService executorService, List<ChannelHandlerContext> ctxs) throws IOException {
    deque = new CircularBlockingDeque<T>(DEQUE_CAPACITY);
    channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
    writer = new Writer();
    messageBufferPool = new MessageBufferPool();
    latchedBuffer = MessageBuffers.dynamicBuffer();
    mutex = new Object();
    latchMode = false;
    channels = ctxs;
    executorService.execute(writer);
  }

  public void setLatchMode(boolean enabled) {
    latchMode = enabled;
  }

  public boolean getLatchMode() {
    return latchMode;
  }

  /**
   * @param message
   *          the message to add to the queue
   */
  public void add(T message) {
    deque.addLast(message);
    setLatchedMessage(message);
  }

  private void setLatchedMessage(T message) {
    synchronized (mutex) {
      latchedMessage = message;
    }
  }

  /**
   * Stop writing messages and close all outgoing connections.
   */
  public void shutdown() {
    writer.cancel();
    channelGroup.shutdown();
  }


  private void writeLatchedMessage() {
    synchronized (mutex) {
      latchedBuffer.clear();
      Utility.serialize(latchedMessage, latchedBuffer);
      Iterator<ChannelHandlerContext> it = channels.iterator();
      while(it.hasNext()) {
    	  ChannelHandlerContext ctx = it.next();
    	  ctx.write(latchedBuffer);
      }
    }
  }

  /**
   * @return the number of {@link Channel}s which have been added to this queue
   */
  public int getNumberOfChannels() {
    return channels.size();
  }

  public AsynchronousChannelGroup getChannelGroup() {
    return channelGroup;
  }
}
