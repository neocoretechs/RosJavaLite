package org.ros.internal.transport.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TcpServerPipelineFactory extends ConnectionTrackingChannelPipelineFactory {

  public static final String LENGTH_FIELD_BASED_FRAME_DECODER = "LengthFieldBasedFrameDecoder";
  public static final String LENGTH_FIELD_PREPENDER = "LengthFieldPrepender";
  public static final String HANDSHAKE_HANDLER = "HandshakeHandler";

  private final TopicParticipantManager topicParticipantManager;
  private final ServiceManager serviceManager;

  public TcpServerPipelineFactory(ChannelGroup channelGroup,
      TopicParticipantManager topicParticipantManager, ServiceManager serviceManager) {
    super(channelGroup);
    this.topicParticipantManager = topicParticipantManager;
    this.serviceManager = serviceManager;
  }

  @Override
  protected void initChannel(Channel ch) {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(LENGTH_FIELD_PREPENDER, new LengthFieldPrepender(4));
    pipeline.addLast(LENGTH_FIELD_BASED_FRAME_DECODER, new LengthFieldBasedFrameDecoder(
        Integer.MAX_VALUE, 0, 4, 0, 4));
    pipeline.addLast(HANDSHAKE_HANDLER, new TcpServerHandshakeHandler(topicParticipantManager,
        serviceManager));
  }
}
