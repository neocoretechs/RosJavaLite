package org.ros.internal.node.service;

import java.nio.ByteBuffer;
import java.util.List;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;


import org.ros.internal.message.MessageBuffers;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 */
public final class ServiceResponseEncoder {//extends MessageToMessageEncoder<ServiceServerResponse> {

  
  protected void encode(ChannelHandlerContext ctx, ServiceServerResponse msg, List<Object> out) throws Exception {
    if (msg instanceof ServiceServerResponse) {
      ServiceServerResponse response = msg;
      ByteBuffer buffer = MessageBuffers.dynamicBuffer();
      buffer.putInt(response.getErrorCode());
      buffer.putInt(response.getMessageLength());
      buffer.put(response.getMessage());
      //return buffer;
      out.add(buffer);
    } else {
      //return msg;
      out.add(msg);
    }
  }
}
