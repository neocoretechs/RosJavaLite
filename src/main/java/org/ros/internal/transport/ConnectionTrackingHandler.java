package org.ros.internal.transport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelStateEvent;
//import org.jboss.netty.channel.ExceptionEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;
//import org.jboss.netty.channel.group.ChannelGroup;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.transport.tcp.AbstractNamedChannelHandler;

import java.io.IOException;


/**
 * This handler is meant to sit at the top of the stack such that when exceptions are propagated
 * to handlers this one receives notification last and can act upon it or not.
 * @author jg
 * 
 */
public class ConnectionTrackingHandler extends AbstractNamedChannelHandler {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(ConnectionTrackingHandler.class);

  public ConnectionTrackingHandler() {
    if (DEBUG) {
        log.info("ConnectionTrackingHandler ctor");
      }
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    if (DEBUG) {
      log.info("ConnectionTrackingHandler added to channel(Channel open):" + ctx.channel());
    }
  }

  @Override
  //public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    if (DEBUG) {
      log.info("ConnectionTrackingHandler removed(Channel closed):" + ctx.channel());
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
    ctx.channel().close();
    if (e.getCause() instanceof IOException) {
      // (network failure, connection reset by peer, shutting down, etc.)
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

  @Override
  public String getName() {
	return "ConnectionTrackingHandler";
  }

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("Channel active "+ctx);
	}

	@Override
	public Object channelRead(ChannelHandlerContext ctx, Object buff) throws Exception {
		log.info("Channel read "+ctx);
		return buff;
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		log.info("Channel read complete"+ctx);	
	}


	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("Channel inactive"+ ctx);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object event)
			throws Exception {
		
		log.info("User event triggered "+ctx+" "+event);	
	}
}