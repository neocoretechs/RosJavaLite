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

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBuffers;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.MessageEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;
import org.ros.exception.ServiceException;
import org.ros.internal.message.MessageBufferPool;
import org.ros.internal.system.Utility;
import org.ros.message.MessageFactory;
import org.ros.node.service.ServiceResponseBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
class ServiceRequestHandler<T, S> extends SimpleChannelInboundHandler<T> {

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

  private void handleRequest(ByteBuf requestBuffer, ByteBuf responseBuffer)
      throws ServiceException {
    T request = (T) Utility.deserialize(requestBuffer);
    S response = messageFactory.newFromType(serviceDeclaration.getType());
    responseBuilder.build(request, response);
    Utility.serialize(response, responseBuffer);
  }

  private void handleSuccess(final ChannelHandlerContext ctx, ServiceServerResponse response,
      ByteBuf responseBuffer) {
    response.setErrorCode(1);
    response.setMessageLength(responseBuffer.readableBytes());
    response.setMessage(responseBuffer);
    ctx.channel().write(response);
  }

  private void handleError(final ChannelHandlerContext ctx, ServiceServerResponse response,
      String message) {
    response.setErrorCode(0);
    ByteBuffer encodedMessage = Charset.forName("US-ASCII").encode(message);
    response.setMessageLength(encodedMessage.limit());
    response.setMessage(Unpooled.wrappedBuffer(encodedMessage));
    ctx.channel().write(response);
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, Object e) throws Exception {
    // Although the ChannelHandlerContext is explicitly documented as being safe
    // to keep for later use, the MessageEvent is not. So, we make a defensive
    // copy of the ChannelBuffer.
    final ByteBuf requestBuffer = ((ByteBuf) e).copy();
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        ServiceServerResponse response = new ServiceServerResponse();
        ByteBuf responseBuffer = messageBufferPool.acquire();
        boolean success;
        try {
          handleRequest(requestBuffer, responseBuffer);
          success = true;
        } catch (ServiceException ex) {
          handleError(ctx, response, ex.getMessage());
          success = false;
        }
        if (success) {
          handleSuccess(ctx, response, responseBuffer);
        }
        messageBufferPool.release(responseBuffer);
      }
    });
  
  }

}
