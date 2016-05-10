package org.ros.internal.transport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelStateEvent;
//import org.jboss.netty.channel.ExceptionEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;
//import org.jboss.netty.channel.group.ChannelGroup;
import org.ros.exception.RosRuntimeException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;

import java.io.IOException;
import java.nio.channels.Channels;

/**
 * Adds new {@link Channels} to the provided {@link ChannelGroup}.
 * 
 */
public class ConnectionTrackingHandler extends ChannelInboundHandlerAdapter {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(ConnectionTrackingHandler.class);

  /**
   * The channel group the connection is to be part of.
   */
  private final ChannelGroup channelGroup;

  public ConnectionTrackingHandler(ChannelGroup channelGroup) {
    this.channelGroup = channelGroup;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    if (DEBUG) {
      log.info("Channel opened: " + ctx.channel());
    }
    channelGroup.add(ctx.channel());
    super.handlerAdded(ctx);
  }

  @Override
  //public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    if (DEBUG) {
      log.info("Channel closed: " + ctx.channel());
    }
    super.handlerRemoved(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
    ctx.channel().close();
    if (e.getCause() instanceof IOException) {
      // NOTE(damonkohler): We ignore exceptions here because they are common
      // (e.g. network failure, connection reset by peer, shutting down, etc.)
      // and should not be fatal. However, in all cases the channel should be
      // closed.
      if (DEBUG) {
        log.error("Channel exception: " + ctx.channel(), e.getCause());
      } else {
        log.error("Channel exception: " + e.getCause());
      }
    } else {
      throw new RosRuntimeException(e.getCause());
    }
  }
}