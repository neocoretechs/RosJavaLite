package org.ros.internal.transport.tcp;

import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.Channel;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 * @author jg
 */
public class TcpClientPipelineFactory extends ConnectionTrackingChannelPipelineFactory {
  public static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(TcpClientPipelineFactory.class);
  public static final String LENGTH_FIELD_BASED_FRAME_DECODER = "LengthFieldBasedFrameDecoder";
  public static final String LENGTH_FIELD_PREPENDER = "LengthFieldPrepender";
  private List<NamedChannelHandler> namedChannelHandlers;

  public TcpClientPipelineFactory(/*Asynchronous*/ChannelGroup asynchronousChannelGroup, List<NamedChannelHandler> namedChannelHandlers) {
    super(asynchronousChannelGroup);
    this.namedChannelHandlers = namedChannelHandlers;
    if( DEBUG )
    	log.info("TcpClientPipelineFactory:"+asynchronousChannelGroup);
  }

  @Override
  protected void initChannel(ChannelHandlerContext ch) {
	  if( DEBUG )
	    	log.info("TcpClientPipelineFactory.initchannel:"+ch);
        for (NamedChannelHandler namedChannelHandler : namedChannelHandlers) 
          ch.pipeline().addLast(namedChannelHandler.getName(), namedChannelHandler);
    //ChannelPipeline pipeline = ch.pipeline();
    //pipeline.addLast(LENGTH_FIELD_PREPENDER, new LengthFieldPrepender(4));
    //pipeline.addLast(LENGTH_FIELD_BASED_FRAME_DECODER, new LengthFieldBasedFrameDecoder(
    //    Integer.MAX_VALUE, 0, 4, 0, 4));
  }
}
