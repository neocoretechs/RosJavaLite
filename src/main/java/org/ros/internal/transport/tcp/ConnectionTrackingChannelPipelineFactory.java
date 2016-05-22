package org.ros.internal.transport.tcp;

import java.nio.channels.AsynchronousChannelGroup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ConnectionTrackingHandler;

/**
 */
public class ConnectionTrackingChannelPipelineFactory extends ChannelInitializer {
  public static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(ConnectionTrackingChannelPipelineFactory.class);
  public static final String CONNECTION_TRACKING_HANDLER = "ConnectionTrackingHandler";

  private final ConnectionTrackingHandler connectionTrackingHandler;
  
  public ConnectionTrackingChannelPipelineFactory(AsynchronousChannelGroup channelGroup){
    this.connectionTrackingHandler = new ConnectionTrackingHandler();
    if(DEBUG)
    	log.info("ConnectionTrackingChannelPipelineFactory ctor"+channelGroup);
  }

  @Override
  protected void initChannel(ChannelHandlerContext ch) throws Exception {
	ch.pipeline().addLast(CONNECTION_TRACKING_HANDLER, connectionTrackingHandler);
	if(DEBUG)
	    	log.info("ConnectionTrackingChannelPipelineFactory initchannel:"+ch);
  }

  
}
