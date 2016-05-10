package org.ros.internal.transport.queue;

import java.net.SocketAddress;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

import org.ros.concurrent.CircularBlockingDeque;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.MessageEvent;

import org.ros.internal.transport.tcp.AbstractNamedChannelHandler;


/**
 * @author damonkohler@google.com (Damon Kohler)
 * 
 * @param <T>
 *          the message type
 */
public class MessageReceiver<T> extends AbstractNamedChannelHandler {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(MessageReceiver.class);

  private final CircularBlockingDeque<LazyMessage<T>> lazyMessages;


  public MessageReceiver(CircularBlockingDeque<LazyMessage<T>> lazyMessages) {
    this.lazyMessages = lazyMessages;

  }

  @Override
  public String getName() {
    return "IncomingMessageQueueChannelHandler";
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ByteBuf buffer = (ByteBuf) msg;
    if (DEBUG) {
      log.info(String.format("Received %d byte message.", buffer.readableBytes()));
    }
    try {
    	// We have to make a defensive copy of the buffer here because Netty does
    	// not guarantee that the returned ChannelBuffer will not be reused.
    	lazyMessages.addLast(new LazyMessage<T>(buffer.copy()));
    } finally {
      ReferenceCountUtil.release(msg); //V4.x
    }
  }



@Override
public void exceptionCaught(ChannelHandlerContext arg0, Throwable arg1)
		throws Exception {
	// TODO Auto-generated method stub
	
}


@Override
public void handlerAdded(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void handlerRemoved(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelActive(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelInactive(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelReadComplete(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelRegistered(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelUnregistered(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelWritabilityChanged(ChannelHandlerContext arg0)
		throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void userEventTriggered(ChannelHandlerContext arg0, Object arg1)
		throws Exception {
	// TODO Auto-generated method stub
	
}



}