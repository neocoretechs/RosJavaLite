package org.ros.internal.node.service;

import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBuffers;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.MessageEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceException;
import org.ros.internal.message.MessageBuffers;
//import org.ros.internal.message.MessageBufferPool;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandler;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.message.MessageFactory;
import org.ros.node.service.ServiceResponseBuilder;

import rosgraph_msgs.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;

/**
 * @author jg
 */
class ServiceRequestHandler<T, S> implements ChannelHandler {
  private static final org.apache.commons.logging.Log log = LogFactory.getLog(ServiceRequestHandler.class);
  private final ServiceDeclaration serviceDeclaration;
  private final ServiceResponseBuilder<T, S> responseBuilder;
 
  private final MessageFactory messageFactory;
  private final ExecutorService executorService;
  //private final ByteBuffer messageBuffer = MessageBuffers.dynamicBuffer();
  //private final MessageBufferPool messageBufferPool;

  public ServiceRequestHandler(ServiceDeclaration serviceDeclaration,
      ServiceResponseBuilder<T, S> responseBuilder, MessageFactory messageFactory,
      ExecutorService executorService) {
    this.serviceDeclaration = serviceDeclaration;
    this.responseBuilder = responseBuilder;
    this.messageFactory = messageFactory;
    this.executorService = executorService;
   // messageBufferPool = new MessageBufferPool();
  }

  private S handleRequest(T request) throws ServiceException {
    S response = messageFactory.newFromType(serviceDeclaration.getType());
    responseBuilder.build(request, response);
    return response;
  }

  private void handleSuccess(final ChannelHandlerContext ctx, S result, ServiceServerResponse response, ByteBuffer responseBuffer) {
    response.setErrorCode(1);
    ByteBuffer resbuf = MessageBuffers.dynamicBuffer(); // allocate for serialized result of service method
    Utility.serialize(result, resbuf);
    byte[] b = new byte[resbuf.limit()];
    resbuf.get(b);
    response.setMessageBytes(b);
    response.setMessageLength(response.getMessageBytes().length);
    responseBuffer.putInt(response.getErrorCode());
    responseBuffer.putInt(response.getMessageLength());
    log.info("Response to be serialized:"+response);
    Utility.serialize(response,  responseBuffer);
    //log.info("ServiceRequestHandler serializing message buffer "+responseBuffer+
    //		" with payload "+response.getMessageBytes().length);
    try {
		ctx.write(responseBuffer.array());
	} catch (IOException e) {
		throw new RosRuntimeException(e);
	}
    MessageBuffers.returnBuffer(resbuf);
  }

  private void handleError(final ChannelHandlerContext ctx, ServiceServerResponse response, String message, ByteBuffer responseBuffer) {
    response.setErrorCode(0);
    ByteBuffer encodedMessage = Charset.forName("US-ASCII").encode(message);
    response.setMessageLength(encodedMessage.limit());
    response.setMessage(encodedMessage);
    response.setMessageBytes(encodedMessage.array());
    responseBuffer.putInt(response.getErrorCode());
    responseBuffer.putInt(response.getMessageLength());
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
        ByteBuffer responseBuffer = MessageBuffers.dynamicBuffer();
        boolean success;
        S result = null;
        try {
          result = handleRequest(requestBuffer);
          success = true;
        } catch (ServiceException ex) {
          handleError(ctx, response, ex.getMessage(), responseBuffer);
          success = false;
        }
        if (success) {
          handleSuccess(ctx, result, response, responseBuffer);
        }
        //messageBufferPool.release(responseBuffer);
        MessageBuffers.returnBuffer(responseBuffer);
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
