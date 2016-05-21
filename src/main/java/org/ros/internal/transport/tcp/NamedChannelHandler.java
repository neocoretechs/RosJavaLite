package org.ros.internal.transport.tcp;

import org.ros.internal.transport.ChannelHandler;


//import org.jboss.netty.channel.ChannelDownstreamHandler;
//import org.jboss.netty.channel.ChannelHandler;
//import org.jboss.netty.channel.ChannelUpstreamHandler;

/**
 * @author jg
 */
public interface NamedChannelHandler extends ChannelHandler {

  /**
   * @return the name of this {@link ChannelHandler}
   */
  String getName();
}
