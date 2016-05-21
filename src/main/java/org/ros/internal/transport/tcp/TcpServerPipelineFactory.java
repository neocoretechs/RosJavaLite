package org.ros.internal.transport.tcp;

import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.Channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelPipeline;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TcpServerPipelineFactory extends ConnectionTrackingChannelPipelineFactory {
  public static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(TcpServerPipelineFactory.class);
  public static final String LENGTH_FIELD_BASED_FRAME_DECODER = "LengthFieldBasedFrameDecoder";
  public static final String LENGTH_FIELD_PREPENDER = "LengthFieldPrepender";
  public static final String HANDSHAKE_HANDLER = "HandshakeHandler";

  private final TopicParticipantManager topicParticipantManager;
  private final ServiceManager serviceManager;

  public TcpServerPipelineFactory(AsynchronousChannelGroup channelGroup,
      TopicParticipantManager topicParticipantManager, ServiceManager serviceManager) {
    super(channelGroup);
    if( DEBUG )
    	log.debug("TcpServerPipeLineFactory ctor:"+channelGroup+" "+topicParticipantManager+" "+serviceManager);
    this.topicParticipantManager = topicParticipantManager;
    this.serviceManager = serviceManager;
  }

  @Override
  protected void initChannel(ChannelHandlerContext ch) throws Exception {
	if( DEBUG )
		log.info("TcpServerPipelineFactory initChannel:"+ch);
    ChannelPipeline pipeline = ch.pipeline();
    //pipeline.addLast(LENGTH_FIELD_PREPENDER, new LengthFieldPrepender(4));
    //pipeline.addLast(LENGTH_FIELD_BASED_FRAME_DECODER, new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
    pipeline.addLast(HANDSHAKE_HANDLER, new TcpServerHandshakeHandler(topicParticipantManager,serviceManager));
  }
}
