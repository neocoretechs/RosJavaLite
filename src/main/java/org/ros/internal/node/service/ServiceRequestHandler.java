package org.ros.internal.node.service;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBuffers;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.MessageEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceException;
import org.ros.internal.message.MessageBufferPool;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandler;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.message.MessageFactory;
import org.ros.node.service.ServiceResponseBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;

/**
 * @author jg
 */
class ServiceRequestHandler<T, S> implements ChannelHandler {

  private final ServiceDeclaration serviceDeclaration;
  private final ServiceResponseBuilder<T, S> responseBuilder;
 
  private final MessageFactory messageFactory;
  private final ExecutorService executorService;
  private final MessageBufferPool messageBufferPool;

  public ServiceRequestHandler(ServiceDeclaration serviceDeclaration,
      ServiceResponseBuilder<T, S> responseBuilder, MessageFactory messageFactory,
      ExecutorService executorService) {
    this.serviceDeclaration = serviceDeclaration;
    this.responseBuilder = responseBuilder;
    this.messageFactory = messageFactory;
    this.executorService = executorService;
    messageBufferPool = new MessageBufferPool();
  }

  private void handleRequest(T request, ByteBuffer responseBuffer) throws ServiceException {
    S response = messageFactory.newFromType(serviceDeclaration.getType());
    responseBuilder.build(request, response);
    Utility.serialize(response, responseBuffer);
  }

  private void handleSuccess(final ChannelHandlerContext ctx, ServiceServerResponse response, ByteBuffer responseBuffer) {
    response.setErrorCode(1);
    response.setMessageLength(responseBuffer.limit());
    response.setMessageBytes(responseBuffer.array());
    ByteBuffer resbuf = messageBufferPool.acquire();
    Utility.serialize(response,  resbuf);
    try {
		ctx.write(resbuf.array());
	} catch (IOException e) {
		throw new RosRuntimeException(e);
	}
  }

  private void handleError(final ChannelHandlerContext ctx, ServiceServerResponse response, String message, ByteBuffer responseBuffer) {
    response.setErrorCode(0);
    ByteBuffer encodedMessage = Charset.forName("US-ASCII").encode(message);
    response.setMessageLength(encodedMessage.limit());
    response.setMessage(encodedMessage);
    response.setMessageBytes(encodedMessage.array());
    Utility.serialize(response,  responseBuffer);
    try {
		ctx.write(responseBuffer.array());
	} catch (IOException e) {
		throw new RosRuntimeException(e);
	}
  }

  @Override
  public Object channelRead(final ChannelHandlerContext ctx, Object e) throws Exception {
    // Although the ChannelHandlerContext is explicitly documented as being safe
    // to keep for later use, the MessageEvent is not. So, we make a defensive
    // copy of the buffer.
    final T requestBuffer = ((T) e);
    this.executorService.execute(new Runnable() {
      @Override
      public void run() {
        ServiceServerResponse response = new ServiceServerResponse();
        ByteBuffer responseBuffer = messageBufferPool.acquire();
        boolean success;
        try {
          handleRequest(requestBuffer, responseBuffer);
          success = true;
        } catch (ServiceException ex) {
          handleError(ctx, response, ex.getMessage(), responseBuffer);
          success = false;
        }
        if (success) {
          handleSuccess(ctx, response, responseBuffer);
        }
        messageBufferPool.release(responseBuffer);
      }
    });
    return requestBuffer;
  }

@Override
public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelActive(ChannelHandlerContext ctx) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable msg)
		throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object event)
		throws Exception {
	// TODO Auto-generated method stub
	
}

}
