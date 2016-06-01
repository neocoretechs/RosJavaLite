package org.ros.internal.transport.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

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
		  log.info("TcpServerHandshakeHandler ctor:"+topicParticipantManager+" "+serviceManager);
	}
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
	  log.info("Channel active");
  }
  
  @Override
  public Object channelRead(ChannelHandlerContext ctx, Object e) throws Exception {
	if( DEBUG ) {
			  log.info("TcpServerHandshakeHandler channelRead:"+e);
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

  private void handleServiceHandshake(ChannelHandlerContext ctx, ConnectionHeader incomingHeader) throws IOException {
	if( DEBUG ) {
		  log.info("service handshake:"+ctx+" header:"+incomingHeader);
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

  private void handleSubscriberHandshake(final ChannelHandlerContext ctx, final ConnectionHeader incomingConnectionHeader)
      throws InterruptedException, Exception {
	  if( DEBUG ) {
		  log.info("subscriber handshake:"+ctx+" header:"+incomingConnectionHeader);
	  }
    assert(incomingConnectionHeader.hasField(ConnectionHeaderFields.TOPIC)) :
        "Handshake header missing field: " + ConnectionHeaderFields.TOPIC;
    final GraphName topicName =
        GraphName.of(incomingConnectionHeader.getField(ConnectionHeaderFields.TOPIC));
    assert(topicParticipantManager.hasPublisher(topicName)) :
        "No publisher for topic: " + topicName;
    final DefaultPublisher<?> publisher = topicParticipantManager.getPublisher(topicName);
    final ByteBuffer outgoingBuffer = publisher.finishHandshake(incomingConnectionHeader);
    // Write the handshake data back to client and upon completion set this channel
    // ready for write queue
    ctx.write(outgoingBuffer, new CompletionHandler<Integer, Void>() {
		@Override
		public void completed(Integer arg0, Void arg1) {
			   String nodeName = incomingConnectionHeader.getField(ConnectionHeaderFields.CALLER_ID);
			   publisher.addSubscriber(new SubscriberIdentifier(NodeIdentifier.forName(nodeName), new TopicIdentifier(topicName)), ctx);
			    // Once the handshake is complete, there will be nothing incoming on the
			    // channel as we are only queueing outbound traffic to the subscriber, which is done by the OutgoingMessgequeue.
			    // So, we replace the handshake handler with a handler which will
			    // drop everything.
			    ctx.pipeline().remove(TcpServerPipelineFactory.HANDSHAKE_HANDLER);
			    // Set this context ready to receive the message type specified
			    synchronized(ctx.getMessageTypes()) {
			    	ctx.getMessageTypes().add(incomingConnectionHeader.getField(ConnectionHeaderFields.TYPE));
			    }
				// set as ready for channel write loop in OutogingMessageQueue
				ctx.setReady(true);
				if( DEBUG ) {
					  log.info("subscriber complete:"+outgoingBuffer);
				}
		}
		@Override
		public void failed(Throwable arg0, Void arg1) {
			log.info("Failed to perform handshake for:"+ctx);
		} 
    });

 
  }

@Override
public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
	log.info("Handler added "+ctx);
	
}

@Override
public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
	log.info("Handler removed "+ctx);
	
}

@Override
public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	log.info("Channel inactive "+ctx);
	
}


@Override
public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	log.info("channel read complete "+ctx);
	
}

@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable msg)throws Exception {
	log.error("Handshake notified of err! should not occur!");
	
}

@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object event)
		throws Exception {
	log.info("User event triggered "+ctx+" "+event);
	
}


}
