package org.ros.internal.transport.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;

import org.ros.internal.transport.ConnectionTrackingHandler;

/**
 */
public class ConnectionTrackingChannelPipelineFactory extends ChannelInitializer<Channel> {

  public static final String CONNECTION_TRACKING_HANDLER = "ConnectionTrackingHandler";

  private final ConnectionTrackingHandler connectionTrackingHandler;
  
  public ConnectionTrackingChannelPipelineFactory(ChannelGroup channelGroup){
    this.connectionTrackingHandler = new ConnectionTrackingHandler(channelGroup);
  }

  @Override
  protected void initChannel(Channel ch) throws Exception {
	ch.pipeline().addLast(CONNECTION_TRACKING_HANDLER, connectionTrackingHandler);	
  }
}
