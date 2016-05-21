package org.ros.internal.node.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.channel.MessageEvent;
import org.ros.internal.transport.BaseClientHandshakeHandler;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelPipeline;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.tcp.TcpClientPipelineFactory;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.service.ServiceServer;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * Performs a handshake with the connected {@link ServiceServer}.
 * 
 * @author jg
 * 
 * @param <T>
 *          the connected {@link ServiceServer} responds to requests of this
 *          type
 * @param <S>
 *          the connected {@link ServiceServer} returns responses of this type
 */
class ServiceClientHandshakeHandler<T, S> extends BaseClientHandshakeHandler {

  private static final Log log = LogFactory.getLog(ServiceClientHandshakeHandler.class);
  
  private final Queue<ServiceResponseListener<S>> responseListeners;
  private final ExecutorService executorService;

  public ServiceClientHandshakeHandler(ConnectionHeader outgoingConnectionHeader,
      Queue<ServiceResponseListener<S>> responseListeners, ExecutorService executorService) {
    super(new ServiceClientHandshake(outgoingConnectionHeader), executorService);
    this.responseListeners = responseListeners;
    this.executorService = executorService;
  }

  @Override
  protected void onSuccess(ConnectionHeader incommingConnectionHeader, ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.pipeline();
    pipeline.remove(TcpClientPipelineFactory.LENGTH_FIELD_BASED_FRAME_DECODER);
    pipeline.remove(ServiceClientHandshakeHandler.this);
    //pipeline.addLast("ResponseDecoder", new ServiceResponseDecoder<S>());
    pipeline.addLast("ResponseHandler", new ServiceResponseHandler<S>(responseListeners, executorService));
  }

  @Override
  protected void onFailure(String errorMessage, ChannelHandlerContext ctx) throws IOException {
    log.error("Service client handshake failed: " + errorMessage);
    ctx.close();
  }

  @Override
  public String getName() {
    return "ServiceClientHandshakeHandler";
  }


@Override
public void handlerAdded(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void handlerRemoved(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable th)
		throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object event)
		throws Exception {
	// TODO Auto-generated method stub
	
}


}
