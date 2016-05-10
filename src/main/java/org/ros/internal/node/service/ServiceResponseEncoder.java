package org.ros.internal.node.service;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;


import io.netty.handler.codec.MessageToMessageEncoder;

import org.ros.internal.message.MessageBuffers;

/**
 */
public final class ServiceResponseEncoder extends MessageToMessageEncoder<ServiceServerResponse> {

  @Override
  protected void encode(ChannelHandlerContext ctx, ServiceServerResponse msg, List<Object> out) throws Exception {
    if (msg instanceof ServiceServerResponse) {
      ServiceServerResponse response = msg;
      ByteBuf buffer = MessageBuffers.dynamicBuffer();
      buffer.writeByte(response.getErrorCode());
      buffer.writeInt(response.getMessageLength());
      buffer.writeBytes(response.getMessage());
      //return buffer;
      out.add(buffer);
    } else {
      //return msg;
      out.add(msg);
    }
  }
}
