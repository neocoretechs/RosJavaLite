package org.ros.internal.transport.tcp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;

//import org.jboss.netty.channel.ChannelDownstreamHandler;
//import org.jboss.netty.channel.ChannelHandler;
//import org.jboss.netty.channel.ChannelUpstreamHandler;

/**
 * upstream / downstream now inbound/outbound at netty v4.x
 * @author jg
 */
public interface NamedChannelHandler extends ChannelInboundHandler {

  /**
   * @return the name of this {@link ChannelHandler}
   */
  String getName();
}
