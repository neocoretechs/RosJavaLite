package org.ros.internal.transport.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.ConnectionTrackingHandler;

/**
 */
public class ConnectionTrackingChannelPipelineFactory extends ChannelInitializer<Channel> {
  public static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(ConnectionTrackingChannelPipelineFactory.class);
  public static final String CONNECTION_TRACKING_HANDLER = "ConnectionTrackingHandler";

  private final ConnectionTrackingHandler connectionTrackingHandler;
  
  public ConnectionTrackingChannelPipelineFactory(ChannelGroup channelGroup){
	super();
    this.connectionTrackingHandler = new ConnectionTrackingHandler(channelGroup);
    if(DEBUG)
    	log.debug("ConnectionTrackingChannelPipelineFactory ctor"+channelGroup);
  }

  @Override
  protected void initChannel(Channel ch) throws Exception {
	ch.pipeline().addLast(CONNECTION_TRACKING_HANDLER, connectionTrackingHandler);
	   if(DEBUG)
	    	log.debug("ConnectionTrackingChannelPipelineFactory initchannel:"+ch);
  }
  
}
