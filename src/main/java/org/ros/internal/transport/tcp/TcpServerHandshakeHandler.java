package org.ros.internal.transport.tcp;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelFuture;
//import org.jboss.netty.channel.ChannelHandler;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.channel.MessageEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;

import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.service.DefaultServiceServer;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.service.ServiceResponseEncoder;
import org.ros.internal.node.topic.DefaultPublisher;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.internal.node.topic.TopicIdentifier;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.namespace.GraphName;

/**
 * A {@link ChannelHandler} which will process the TCP server handshake.
 * 
 * @author jg

 */
public class TcpServerHandshakeHandler extends ChannelDuplexHandler {

  private final TopicParticipantManager topicParticipantManager;
  private final ServiceManager serviceManager;

  public TcpServerHandshakeHandler(TopicParticipantManager topicParticipantManager,
      ServiceManager serviceManager) {
    this.topicParticipantManager = topicParticipantManager;
    this.serviceManager = serviceManager;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object e) throws Exception {
    ByteBuf incomingBuffer = (ByteBuf) e;
    ChannelPipeline pipeline = ctx.channel().pipeline();
    ConnectionHeader incomingHeader = ConnectionHeader.decode(incomingBuffer);
    if (incomingHeader.hasField(ConnectionHeaderFields.SERVICE)) {
      handleServiceHandshake(ctx, e, pipeline, incomingHeader);
    } else {
      handleSubscriberHandshake(ctx, e, pipeline, incomingHeader);
    }
  }

  private void handleServiceHandshake(ChannelHandlerContext ctx, Object e, ChannelPipeline pipeline,
      ConnectionHeader incomingHeader) {
    GraphName serviceName = GraphName.of(incomingHeader.getField(ConnectionHeaderFields.SERVICE));
    assert(serviceManager.hasServer(serviceName));
    DefaultServiceServer<?, ?> serviceServer = serviceManager.getServer(serviceName);
    ctx.channel().write(serviceServer.finishHandshake(incomingHeader));
    String probe = incomingHeader.getField(ConnectionHeaderFields.PROBE);
    if (probe != null && probe.equals("1")) {
      ctx.channel().close();
    } else {
      pipeline.replace(TcpServerPipelineFactory.LENGTH_FIELD_PREPENDER, "ServiceResponseEncoder",
          new ServiceResponseEncoder());
      pipeline.replace(this, "ServiceRequestHandler", serviceServer.newRequestHandler());
    }
  }

  private void handleSubscriberHandshake(ChannelHandlerContext ctx, Object e,
      ChannelPipeline pipeline, ConnectionHeader incomingConnectionHeader)
      throws InterruptedException, Exception {
    assert(incomingConnectionHeader.hasField(ConnectionHeaderFields.TOPIC)) :
        "Handshake header missing field: " + ConnectionHeaderFields.TOPIC;
    GraphName topicName =
        GraphName.of(incomingConnectionHeader.getField(ConnectionHeaderFields.TOPIC));
    assert(topicParticipantManager.hasPublisher(topicName)) :
        "No publisher for topic: " + topicName;
    DefaultPublisher<?> publisher = topicParticipantManager.getPublisher(topicName);
    ByteBuf outgoingBuffer = publisher.finishHandshake(incomingConnectionHeader);
    Channel channel = ctx.channel();
    ChannelFuture future = channel.write(outgoingBuffer).await();
    if (!future.isSuccess()) {
      throw new RosRuntimeException(future.cause());
    }
    String nodeName = incomingConnectionHeader.getField(ConnectionHeaderFields.CALLER_ID);
    publisher.addSubscriber(new SubscriberIdentifier(NodeIdentifier.forName(nodeName),
        new TopicIdentifier(topicName)), channel);

    // Once the handshake is complete, there will be nothing incoming on the
    // channel. So, we replace the handshake handler with a handler which will
    // drop everything.
    pipeline.replace(this, "DiscardHandler", new ChannelInboundHandlerAdapter());
  }
}
