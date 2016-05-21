package org.ros.internal.node.service;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.MessageEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;
import org.ros.exception.RemoteException;
import org.ros.internal.node.response.StatusCode;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandler;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.node.service.ServiceResponseListener;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * A handler for service responses.
 * 
 * @author jg
 */
class ServiceResponseHandler<ResponseType> implements ChannelHandler {

  private final Queue<ServiceResponseListener<ResponseType>> responseListeners;
  private final ExecutorService executorService;

  public ServiceResponseHandler(Queue<ServiceResponseListener<ResponseType>> messageListeners, ExecutorService executorService) {
    this.responseListeners = messageListeners;
    this.executorService = executorService;
  }

  @Override
  public Object channelRead(ChannelHandlerContext ctx, Object e) throws Exception {
    final ServiceResponseListener<ResponseType> listener = responseListeners.poll();
    assert(listener != null) : "No listener for incoming service response.";
    final ServiceServerResponse response = (ServiceServerResponse) e;
    final ByteBuffer buffer = response.getMessage();
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        if (response.getErrorCode() == 1) {
          listener.onSuccess((ResponseType) Utility.deserialize(buffer));
        } else {
          String message = Charset.forName("US-ASCII").decode(buffer).toString();
          listener.onFailure(new RemoteException(StatusCode.ERROR, message));
        }
      }
    });
    return e;
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
