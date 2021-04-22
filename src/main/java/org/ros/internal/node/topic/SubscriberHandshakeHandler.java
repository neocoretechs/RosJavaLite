package org.ros.internal.node.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.BaseClientHandshakeHandler;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelPipeline;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.internal.transport.queue.IncomingMessageQueue;
import org.ros.internal.transport.tcp.NamedChannelHandler;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * Performs a handshake with the connected {@link Publisher} and connects the
 * {@link Publisher} to the {@link IncomingMessageQueue} on success.
 * 
 * In the AsynchTcpWorker thread that handles the read for each channel, it strobes the pipeline
 * with the read notification and the handler here takes care of the processing.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 * 
 * @param <T> the {@link Subscriber} may only subscribe to messages of this type
 */
class SubscriberHandshakeHandler<T> extends BaseClientHandshakeHandler {
  private static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(SubscriberHandshakeHandler.class);

  private final IncomingMessageQueue<T> incomingMessageQueue;

  public SubscriberHandshakeHandler(ConnectionHeader outgoingConnectionHeader,
      final IncomingMessageQueue<T> incomingMessageQueue, ExecutorService executorService) {
    super(new SubscriberHandshake(outgoingConnectionHeader), executorService);
    this.incomingMessageQueue = incomingMessageQueue;
    if( DEBUG )
    	log.info("SubscriberHandshakeHandler ctor:"+this+" IncomingMessageQueue:"+incomingMessageQueue+" outgoingConnectionHeader:"+outgoingConnectionHeader);
  }
  /**
   * Triggered from BaseClientHandshakeHandler channelRead
   */
  @Override
  protected void onSuccess(ConnectionHeader incomingConnectionHeader, ChannelHandlerContext ctx) {
	if( DEBUG )
		log.info("SubscriberHandshakeHandler onSuccess for:"+this+" ChannalHandlerContext:"+ctx+" incomingConnectionHeader:"+incomingConnectionHeader);
    ChannelPipeline pipeline = ctx.pipeline();
    pipeline.remove(getName());
    NamedChannelHandler namedChannelHandler = incomingMessageQueue.getMessageReceiver();
    pipeline.addLast(namedChannelHandler.getName(), namedChannelHandler);
    String latching = incomingConnectionHeader.getField(ConnectionHeaderFields.LATCHING);
    if (latching != null && latching.equals("1")) {
      incomingMessageQueue.setLatchMode(true);
    }
    ctx.setReady(true);
  }
  /**
   * Triggered from BaseClientHandshakeHandler
   */
  @Override
  protected void onFailure(String errorMessage, ChannelHandlerContext ctx) throws IOException {
    log.info("Subscriber handshake failed for:"+this+" ChannelHandlerContext:"+ctx+" with error:" + errorMessage);
    ctx.setReady(false);
    ctx.close();
  }

  @Override
  public String getName() {
    return "SubscriberHandshakeHandler";
  }


  public void exceptionCaught(ChannelHandlerContext ctx, Throwable arg1) throws Exception {
	onFailure(arg1.getMessage(), ctx);
	if( DEBUG )
		log.info("SubscriberHandshakeHandler exceptionCaught for:"+this+" ChannelHandlerContext:"+ctx+" error:"+arg1);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
	if( DEBUG )
		log.info("SubscriberHandshakeHandler handlerAdded for:"+this+" ChannelHandlerContext:"+ctx);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
	if( DEBUG )
		log.info("SubscriberHandshakeHandler handlerRemoved for:"+this+" ChannelHandlerContext:"+ctx);
	
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
	if( DEBUG )
		log.info("SubscriberHandshakeHandler userEventTriggered for:"+this+" ChannelHandlerContext"+ctx+" "+event);
  }


}
