package org.ros.internal.transport.tcp;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.service.DefaultServiceServer;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.DefaultPublisher;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.internal.node.topic.TopicIdentifier;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.internal.transport.ChannelHandler;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.namespace.GraphName;

/**
 * A {@link ChannelHandler} which will process the TCP server handshake.
 * Once an incoming channel read takes place the handshake handler is removed and the traffic
 * handler is placed in the pipeline
 * 
 * @author jg
 */
public class TcpServerHandshakeHandler implements ChannelHandler {
  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(TcpServerHandshakeHandler.class);
  private final TopicParticipantManager topicParticipantManager;
  private final ServiceManager serviceManager;

  public TcpServerHandshakeHandler(TopicParticipantManager topicParticipantManager, ServiceManager serviceManager) {
    this.topicParticipantManager = topicParticipantManager;
    this.serviceManager = serviceManager;
    if( DEBUG ) {
		  log.info("TcpServerHandshakeHandler ctor:"+topicParticipantManager+" "+serviceManager);
	}
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
	  if(DEBUG)
		  log.info("Channel active for ChannelHandlerContext:"+ctx);
  }
  /**
   * Channel read initiated by pipeline generated message
   * We make the assumption that the inbound object is of type ConnectionHeader
   * when talking to this handler
   */
  @Override
  public Object channelRead(ChannelHandlerContext ctx, Object e) throws Exception {
	if( DEBUG ) {
			  log.info("TcpServerHandshakeHandler channelRead ChannelHandlerContext:"+ctx+" payload:"+e);
	}
	// check for null, possible fault on bad connect
    ConnectionHeader incomingHeader = (ConnectionHeader)e;
    if (incomingHeader.hasField(ConnectionHeaderFields.SERVICE)) {
      handleServiceHandshake(ctx, incomingHeader);
    } else {
      handleSubscriberHandshake(ctx, incomingHeader);
    }
    return e;
  }
  /**
   * Handle the handshake for a service in response to channel read
   * @param ctx
   * @param incomingHeader
   * @throws IOException
   */
  private void handleServiceHandshake(ChannelHandlerContext ctx, ConnectionHeader incomingHeader) throws IOException {
	if( DEBUG ) {
		  log.info("Service handshake ChannelHandlerContext:"+ctx+" header:"+incomingHeader);
	}
    GraphName serviceName = GraphName.of(incomingHeader.getField(ConnectionHeaderFields.SERVICE));
    assert(serviceManager.hasServer(serviceName));
    DefaultServiceServer<?, ?> serviceServer = serviceManager.getServer(serviceName);
    ctx.write(serviceServer.finishHandshake(incomingHeader));
    String probe = incomingHeader.getField(ConnectionHeaderFields.PROBE);
    if (probe != null && probe.equals("1")) {
      ctx.close();
    } else {
      //ctx.pipeline().remove(TcpServerPipelineFactory.LENGTH_FIELD_PREPENDER);
      ctx.pipeline().remove(TcpServerPipelineFactory.HANDSHAKE_HANDLER);
      //pipeline.addLast("ServiceResponseEncoder", new ServiceResponseEncoder());
      ctx.pipeline().addLast("ServiceRequestHandler", serviceServer.newRequestHandler());
    }
  }
  /**
   * Handle the handshake for a typical (not a service) subscriber in response to a channel read.
   * @param ctx
   * @param incomingConnectionHeader
   * @throws InterruptedException
   * @throws Exception
   */
  private void handleSubscriberHandshake(final ChannelHandlerContext ctx, final ConnectionHeader incomingConnectionHeader)
      throws InterruptedException, Exception {
	  if( DEBUG ) {
		  log.info("Subscriber handshake ChannelHandlerContext:"+ctx+" header:"+incomingConnectionHeader);
	  }
    assert(incomingConnectionHeader.hasField(ConnectionHeaderFields.TOPIC)) :
        "Handshake header missing field: " + ConnectionHeaderFields.TOPIC;
    final GraphName topicName =
        GraphName.of(incomingConnectionHeader.getField(ConnectionHeaderFields.TOPIC));
    assert(topicParticipantManager.hasPublisher(topicName)) :
        "No publisher for topic: " + topicName;
    
    final DefaultPublisher<?> publisher = topicParticipantManager.getPublisher(topicName);
    //final ByteBuffer outgoingBuffer = publisher.finishHandshake(incomingConnectionHeader);
    final ConnectionHeader outgoingBuffer = publisher.finishHandshake(incomingConnectionHeader);
    // Write the handshake data back to client
   
    ctx.write(outgoingBuffer);
    
	String nodeName = incomingConnectionHeader.getField(ConnectionHeaderFields.CALLER_ID);
	publisher.addSubscriber(new SubscriberIdentifier(NodeIdentifier.forName(nodeName), new TopicIdentifier(topicName)), ctx);
	if(DEBUG)
		log.info("Current subscribers:"+publisher.getNumberOfSubscribers()+" for publisher "+publisher);
	// Once the handshake is complete, there will be nothing incoming on the
	// channel as we are only queueing outbound traffic to the subscriber, which is done by the OutgoingMessgequeue.
	// So, we remove the handler
	ctx.pipeline().remove(TcpServerPipelineFactory.HANDSHAKE_HANDLER);
	// Set this context ready to receive the message type specified
	synchronized(ctx.getMessageTypes()) {
			    	ctx.getMessageTypes().add(incomingConnectionHeader.getField(ConnectionHeaderFields.TYPE));
	}
	// The handshake is complete and the only task is to set the context ready, which will allow
	// the outbound queue to start sending messages.
	ctx.setReady(true);
			    
	if( DEBUG ) {
		log.info("Subscriber complete for ChannelHandlerContext:"+ctx+" subscribers="+publisher.getNumberOfSubscribers());
	}
	
  }

@Override
public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
	if( DEBUG )
	log.info(" Handler added for ChannelHandlerContext:"+ctx);
	
}

@Override
public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
	if(DEBUG)
	log.info("Handler removed for ChannelHandlerContext:"+ctx);
	
}

@Override
public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	if(DEBUG)
	log.info("Channel inactive for ChannelHandlerContext:"+ctx);
	
}


@Override
public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	if(DEBUG)
	log.info("Channel read complete for ChannelHandlerContext:"+ctx);
	
}

@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable msg)throws Exception {
	log.error("Handshake notified of err! should not occur!");
	
}

@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object event)
		throws Exception {
	if(DEBUG)
	log.info("User event triggered for ChannelHandlerContext:"+ctx+" event:"+event);
	
}


}
