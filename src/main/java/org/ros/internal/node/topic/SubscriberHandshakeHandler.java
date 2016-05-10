package org.ros.internal.node.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.channel.MessageEvent;
import org.ros.internal.transport.BaseClientHandshakeHandler;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.internal.transport.queue.IncomingMessageQueue;
import org.ros.internal.transport.tcp.NamedChannelHandler;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

/**
 * Performs a handshake with the connected {@link Publisher} and connects the
 * {@link Publisher} to the {@link IncomingMessageQueue} on success.
 * 
 * @author jg
 * 
 * @param <T>
 *          the {@link Subscriber} may only subscribe to messages of this type
 */
class SubscriberHandshakeHandler<T> extends BaseClientHandshakeHandler {
  private static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(SubscriberHandshakeHandler.class);

  private final IncomingMessageQueue<T> incomingMessageQueue;

  public SubscriberHandshakeHandler(ConnectionHeader outgoingConnectionHeader,
      final IncomingMessageQueue<T> incomingMessageQueue, ExecutorService executorService) {
    super(new SubscriberHandshake(outgoingConnectionHeader), executorService);
    this.incomingMessageQueue = incomingMessageQueue;
  }

  @Override
  protected void onSuccess(ConnectionHeader incomingConnectionHeader, ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.channel().pipeline();
    pipeline.remove(SubscriberHandshakeHandler.this);
    NamedChannelHandler namedChannelHandler = incomingMessageQueue.getMessageReceiver();
    pipeline.addLast(namedChannelHandler.getName(), namedChannelHandler);
    String latching = incomingConnectionHeader.getField(ConnectionHeaderFields.LATCHING);
    if (latching != null && latching.equals("1")) {
      incomingMessageQueue.setLatchMode(true);
    }
  }

  @Override
  protected void onFailure(String errorMessage, ChannelHandlerContext ctx) {
    log.error("Subscriber handshake failed: " + errorMessage);
    ctx.channel().close();
  }

  @Override
  public String getName() {
    return "SubscriberHandshakeHandler";
  }

@Override
public void channelInactive(ChannelHandlerContext arg0) throws Exception {
	if( DEBUG )
		System.out.println("SubscriberHandshakeHandler.channelInactive:"+arg0);
	
}


@Override
public void channelRegistered(ChannelHandlerContext arg0) throws Exception {
	if( DEBUG )
		System.out.println("SubscriberHandshakeHandler.channelRegistered:"+arg0);
	
}

@Override
public void channelUnregistered(ChannelHandlerContext arg0) throws Exception {
	if( DEBUG )
		System.out.println("SubscriberHandshakeHandler.channelUnregistered:"+arg0);
	
}

@Override
public void channelWritabilityChanged(ChannelHandlerContext arg0)
		throws Exception {
	if( DEBUG )
		System.out.println("SubscriberHandshakeHandler.channelWritabilityChanged:"+arg0);
	
}

@Override
public void exceptionCaught(ChannelHandlerContext arg0, Throwable arg1)
		throws Exception {
	onFailure(arg1.getMessage(), arg0);
	
}

@Override
public void handlerAdded(ChannelHandlerContext arg0) throws Exception {
	if( DEBUG )
		System.out.println("SubscriberHandshakeHandler.handlerAdded:"+arg0);
	
}

@Override
public void handlerRemoved(ChannelHandlerContext arg0) throws Exception {
	if( DEBUG )
		System.out.println("SubscriberHandshakeHandler.handlerRemoved:"+arg0);
	
}

@Override
public void userEventTriggered(ChannelHandlerContext arg0, Object arg1)
		throws Exception {
	if( DEBUG )
		System.out.println("SubscriberHandshakeHandler.userEventTrigglyPuffed:"+arg0);
	
	
}



}
