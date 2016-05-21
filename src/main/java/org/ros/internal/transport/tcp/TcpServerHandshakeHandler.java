package org.ros.internal.transport.tcp;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelFuture;
//import org.jboss.netty.channel.ChannelHandler;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.channel.MessageEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.service.DefaultServiceServer;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.service.ServiceResponseEncoder;
import org.ros.internal.node.topic.DefaultPublisher;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.internal.node.topic.TopicIdentifier;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.internal.transport.ChannelHandler;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelPipeline;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.namespace.GraphName;

/**
 * A {@link ChannelHandler} which will process the TCP server handshake.
 * 
 * @author jg

 */
public class TcpServerHandshakeHandler implements ChannelHandler {
  private static final boolean DEBUG = true ;
  private static final Log log = LogFactory.getLog(TcpServerHandshakeHandler.class);
  private final TopicParticipantManager topicParticipantManager;
  private final ServiceManager serviceManager;

  public TcpServerHandshakeHandler(TopicParticipantManager topicParticipantManager,
      ServiceManager serviceManager) {
    this.topicParticipantManager = topicParticipantManager;
    this.serviceManager = serviceManager;
    if( DEBUG ) {
		  log.debug("TcpServerHandshakeHandler ctor:"+topicParticipantManager+" "+serviceManager);
	}
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
	  log.debug("Channel active");
  }
  
  @Override
  public Object channelRead(ChannelHandlerContext ctx, Object e) throws Exception {
	if( DEBUG ) {
			  log.debug("TcpServerHandshakeHandler channelRead:"+e);
	}
    ByteBuffer incomingBuffer =  ByteBuffer.wrap((byte[])e);
    ChannelPipeline pipeline = ctx.pipeline();
    ConnectionHeader incomingHeader = ConnectionHeader.decode(incomingBuffer);
    if (incomingHeader.hasField(ConnectionHeaderFields.SERVICE)) {
      handleServiceHandshake(ctx, e, pipeline, incomingHeader);
    } else {
      handleSubscriberHandshake(ctx, e, incomingHeader);
    }
    return incomingBuffer;
  }

  private void handleServiceHandshake(ChannelHandlerContext ctx, Object e, ChannelPipeline pipeline, ConnectionHeader incomingHeader) throws IOException {
	if( DEBUG ) {
		  log.debug("service handshake:"+e);
	}
    GraphName serviceName = GraphName.of(incomingHeader.getField(ConnectionHeaderFields.SERVICE));
    assert(serviceManager.hasServer(serviceName));
    DefaultServiceServer<?, ?> serviceServer = serviceManager.getServer(serviceName);
    ctx.write(serviceServer.finishHandshake(incomingHeader));
    String probe = incomingHeader.getField(ConnectionHeaderFields.PROBE);
    if (probe != null && probe.equals("1")) {
      ctx.close();
    } else {
      pipeline.remove(TcpServerPipelineFactory.LENGTH_FIELD_PREPENDER);
      pipeline.remove(this);
      //pipeline.addLast("ServiceResponseEncoder", new ServiceResponseEncoder());
      pipeline.addLast("ServiceRequestHandler", serviceServer.newRequestHandler());
    }
  }

  private void handleSubscriberHandshake(ChannelHandlerContext ctx, Object e, ConnectionHeader incomingConnectionHeader)
      throws InterruptedException, Exception {
	  if( DEBUG ) {
		  log.debug("subscriber handshake:"+e);
	  }
    assert(incomingConnectionHeader.hasField(ConnectionHeaderFields.TOPIC)) :
        "Handshake header missing field: " + ConnectionHeaderFields.TOPIC;
    GraphName topicName =
        GraphName.of(incomingConnectionHeader.getField(ConnectionHeaderFields.TOPIC));
    assert(topicParticipantManager.hasPublisher(topicName)) :
        "No publisher for topic: " + topicName;
    DefaultPublisher<?> publisher = topicParticipantManager.getPublisher(topicName);
    ByteBuffer outgoingBuffer = publisher.finishHandshake(incomingConnectionHeader);
    //Channel channel = ctx.channel();
    ctx.write(outgoingBuffer);
    //if (!future.isSuccess()) {
    // throw new RosRuntimeException(future.cause());
    //}
    String nodeName = incomingConnectionHeader.getField(ConnectionHeaderFields.CALLER_ID);
    publisher.addSubscriber(new SubscriberIdentifier(NodeIdentifier.forName(nodeName), new TopicIdentifier(topicName)), ctx);

    // Once the handshake is complete, there will be nothing incoming on the
    // channel. So, we replace the handshake handler with a handler which will
    // drop everything.
    ctx.pipeline().remove(this);
	if( DEBUG ) {
		  log.debug("subscriber complete:"+e);
	}
  }

@Override
public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
	log.debug("Handler added "+ctx);
	
}

@Override
public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
	log.debug("Handler removed "+ctx);
	
}

@Override
public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	log.debug("Channel inactive "+ctx);
	
}


@Override
public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	log.debug("channel read complete "+ctx);
	
}

@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable msg)throws Exception {
	log.error("Handshake notified of err! should not occur!");
	
}

@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object event)
		throws Exception {
	log.debug("User event triggered "+ctx+" "+event);
	
}


}
