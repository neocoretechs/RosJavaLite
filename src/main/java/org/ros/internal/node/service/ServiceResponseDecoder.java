package org.ros.internal.node.service;

import java.nio.ByteBuffer;
import java.util.List;

import org.ros.internal.transport.ChannelHandlerContext;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

/**
 * Decodes service responses.
 * 
 * @author jg
 */
class ServiceResponseDecoder  {

  private final static int ERROR_CODE = 0;// = ServiceResponseDecoderState.ERROR_CODE.ordinal(); 
  private final static int MESSAGE_LENGTH = 1;// = ServiceResponseDecoderState.MESSAGE_LENGTH.ordinal(); 
  private final static int MESSAGE = 2;// = ServiceResponseDecoderState.MESSAGE.ordinal();
  
  private ServiceServerResponse response;

  public ServiceResponseDecoder() {
    reset();
  }

  //@SuppressWarnings("fallthrough")
  //@Override
  protected void decode(int code, ChannelHandlerContext ctx, ByteBuffer buffer, List<Object> rstate) throws Exception {
    switch (code) { 
      case ERROR_CODE:
        response.setErrorCode(buffer.getInt());
        //checkpoint(ServiceResponseDecoderState.MESSAGE_LENGTH);
      case MESSAGE_LENGTH:
        response.setMessageLength(buffer.getInt());
       // checkpoint(ServiceResponseDecoderState.MESSAGE);
      case MESSAGE:
        response.setMessage(buffer);
        try {
          //return response;
          rstate.add(response);
        } finally {
          reset();
        }
      default:
        throw new IllegalStateException();
    }
  }

  private void reset() {
    //checkpoint(ServiceResponseDecoderState.ERROR_CODE);
    response = new ServiceServerResponse();
  }



}
