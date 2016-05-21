package org.ros.internal.transport.tcp;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 * 
 */
public abstract class AbstractNamedChannelHandler implements NamedChannelHandler {
	private static final boolean DEBUG = true;
	private static final Log log = LogFactory.getLog(AbstractNamedChannelHandler.class);
	
	@Override
	public String toString() {
		return String.format("NamedChannelHandler<%s, %s>", getName(), super.toString());
	}

  public abstract void channelActive(ChannelHandlerContext ctx) throws Exception;

  public abstract Object channelRead(ChannelHandlerContext ctx, Object buff) throws Exception;

  public abstract void channelReadComplete(ChannelHandlerContext arg0) throws Exception;
	
  public abstract void channelInactive(ChannelHandlerContext ctx) throws Exception;
  
  public abstract void handlerAdded(ChannelHandlerContext ctx) throws Exception;

  public abstract void handlerRemoved(ChannelHandlerContext ctx) throws Exception;
  
  public abstract void exceptionCaught(ChannelHandlerContext ctx, Throwable th) throws Exception;

}
