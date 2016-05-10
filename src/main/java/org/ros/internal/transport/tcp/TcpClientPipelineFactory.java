package org.ros.internal.transport.tcp;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * @author jg
 */
public class TcpClientPipelineFactory extends ConnectionTrackingChannelPipelineFactory {

  public static final String LENGTH_FIELD_BASED_FRAME_DECODER = "LengthFieldBasedFrameDecoder";
  public static final String LENGTH_FIELD_PREPENDER = "LengthFieldPrepender";

  public TcpClientPipelineFactory(ChannelGroup channelGroup) {
    super(channelGroup);
  }

  @Override
  protected void initChannel(SocketChannel ch) {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(LENGTH_FIELD_PREPENDER, new LengthFieldPrepender(4));
    pipeline.addLast(LENGTH_FIELD_BASED_FRAME_DECODER, new LengthFieldBasedFrameDecoder(
        Integer.MAX_VALUE, 0, 4, 0, 4));
  }
}
