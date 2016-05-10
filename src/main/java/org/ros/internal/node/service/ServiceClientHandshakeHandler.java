/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.node.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.channel.MessageEvent;
import org.ros.internal.transport.BaseClientHandshakeHandler;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.tcp.TcpClientPipelineFactory;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.service.ServiceServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * Performs a handshake with the connected {@link ServiceServer}.
 * 
 * @author damonkohler@google.com (Damon Kohler)
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
    ChannelPipeline pipeline = ctx.channel().pipeline();
    pipeline.remove(TcpClientPipelineFactory.LENGTH_FIELD_BASED_FRAME_DECODER);
    pipeline.remove(ServiceClientHandshakeHandler.this);
    pipeline.addLast("ResponseDecoder", new ServiceResponseDecoder<S>());
    pipeline.addLast("ResponseHandler", new ServiceResponseHandler<S>(responseListeners, executorService));
  }

  @Override
  protected void onFailure(String errorMessage, ChannelHandlerContext ctx) {
    log.error("Service client handshake failed: " + errorMessage);
    ctx.channel().close();
  }

  @Override
  public String getName() {
    return "ServiceClientHandshakeHandler";
  }

@Override
public void channelInactive(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelRegistered(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelUnregistered(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void channelWritabilityChanged(ChannelHandlerContext arg0)
		throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void exceptionCaught(ChannelHandlerContext arg0, Throwable arg1)
		throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void userEventTriggered(ChannelHandlerContext arg0, Object arg1)
		throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void handlerAdded(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void handlerRemoved(ChannelHandlerContext arg0) throws Exception {
	// TODO Auto-generated method stub
	
}


}
