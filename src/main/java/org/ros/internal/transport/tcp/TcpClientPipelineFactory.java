package org.ros.internal.transport.tcp;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 * ChannelInitializer responsible for setting the NamedChannelhandlers into the pipeline of
 * a ChannelHandlerContext.
 * @author jg
 */
public class TcpClientPipelineFactory extends ChannelInitializer {
  public static boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(TcpClientPipelineFactory.class);
  public static final String LENGTH_FIELD_BASED_FRAME_DECODER = "LengthFieldBasedFrameDecoder";
  public static final String LENGTH_FIELD_PREPENDER = "LengthFieldPrepender";
  private List<NamedChannelHandler> namedChannelHandlers;
  /**
   * ChannelGroup has shutdown method and access to ExecutorService
   * @param asynchronousChannelGroup
   * @param namedChannelHandlers
   */
  public TcpClientPipelineFactory(/*Asynchronous*/ChannelGroup asynchronousChannelGroup, List<NamedChannelHandler> namedChannelHandlers) {
    this.namedChannelHandlers = namedChannelHandlers;
    if( DEBUG )
    	log.info("TcpClientPipelineFactory:"+asynchronousChannelGroup+" constructing with "+namedChannelHandlers.size()+" NamedChannelHandler(s).");
  }

  @Override
  protected void initChannel(ChannelHandlerContext ch) {
	  if( DEBUG )
	    	log.info("TcpClientPipelineFactory.initchannel initializing "+namedChannelHandlers.size()+" NamedChannelHandler for ChannelHandlerContext "+ch);
        for (NamedChannelHandler namedChannelHandler : namedChannelHandlers) {
      	  if( DEBUG )
  	    	log.info("TcpClientPipelineFactory.initchannel adding namedChannelHandler "+namedChannelHandler+" to pipeline of ChannelhandlerContext "+ch);
          ch.pipeline().addLast(namedChannelHandler.getName(), namedChannelHandler);
        }
    //ChannelPipeline pipeline = ch.pipeline();
    //pipeline.addLast(LENGTH_FIELD_PREPENDER, new LengthFieldPrepender(4));
    //pipeline.addLast(LENGTH_FIELD_BASED_FRAME_DECODER, new LengthFieldBasedFrameDecoder(
    //    Integer.MAX_VALUE, 0, 4, 0, 4));
  }
}
