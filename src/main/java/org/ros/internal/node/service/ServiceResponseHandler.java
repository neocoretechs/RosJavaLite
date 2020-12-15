package org.ros.internal.node.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.MessageEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;
import org.ros.exception.RemoteException;
import org.ros.internal.message.MessageBuffers;
import org.ros.internal.node.response.StatusCode;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandler;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.node.service.ServiceResponseListener;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * A handler for service responses.<p/>
 * The handler revolves around the encoder and the decoder much like the pub/sub model.
 * The encoder and decoder work with the channel handler context and the event model to deliver
 * requests and responses on and off the bus.
 * The service bus looks a lot like the pubsub bus the way it shakes out, essentially you
 * are making a persistent connection to the service server and issuing a repeated set of calls via the 
 * client and getting a response for each call instead of getting a stream of messages via connection to
 * a publisher as a subscriber.<p/>
 * The Encoder for this process looks like this:
 *  protected void encode(ChannelHandlerContext ctx, ServiceServerResponse msg, List<Object> out) throws Exception {
 *   if (msg instanceof ServiceServerResponse) {
 *     ServiceServerResponse response = msg;
 *     ByteBuffer buffer = MessageBuffers.dynamicBuffer();
 *     buffer.putInt(response.getErrorCode());
 *     buffer.putInt(response.getMessageLength());
 *     buffer.put(response.getMessage());
 *     //return buffer;
 *     out.add(buffer);
 *   } else {
 *     //return msg;
 *     out.add(msg);
 *   }
 * }
 * And the decoder looks like this:
 *   protected void decode(int code, ChannelHandlerContext ctx, ByteBuffer buffer, List<Object> rstate) throws Exception {
 *   switch (code) { 
 *     case ERROR_CODE:
 *       response.setErrorCode(buffer.getInt());
 *       //checkpoint(ServiceResponseDecoderState.MESSAGE_LENGTH);
 *     case MESSAGE_LENGTH:
 *       response.setMessageLength(buffer.getInt());
 *      // checkpoint(ServiceResponseDecoderState.MESSAGE);
 *     case MESSAGE:
 *       response.setMessage(buffer);
 *       try {
 *         //return response;
 *         rstate.add(response);
 * 
 * @author jg
 */
class ServiceResponseHandler<ResponseType> implements ChannelHandler {
  private static final Log log = LogFactory.getLog(ServiceResponseHandler.class);
  private final Queue<ServiceResponseListener<ResponseType>> responseListeners;
  private final ExecutorService executorService;

  public ServiceResponseHandler(Queue<ServiceResponseListener<ResponseType>> messageListeners, ExecutorService executorService) {
    this.responseListeners = messageListeners;
    this.executorService = executorService;
  }

	/**
	 * Read the raw byte payload off the bus using the event model, which fires and activates this method.<p/>
	 * Once we turn the raw bytes into a ByteBuffer, we begin to process the integer headers in the payload which
	 * were constructed with the encoder described in the intro.<p/>
	 * 
	 * On an error response the response is encoded into message using 
	 * Charset.forName("US-ASCII").encode(buffer).toString() in {@code ServiceRequestHandler#handleError}
	 * 
	 * The actual error code is 0 inside the decoded 'error code message' for an error, and 1 for success.
	 * 
	 * ServiceServerResponse response is the type set in the decoder for object 'response' and
	 * we are going to get a little stack of them with various states on the List<Object> that
	 * we pass to the decoder, one for each decode method invocation.
	 * The states in each of these ServiceServerResponses represents a response to the code we send down
	 * to indicate the encode action we want, which is the first argument to the 'decode' method:
	 * {@code ServiceResponseDecoder#decode(int, ChannelHandlerContext, ByteBuffer, List)}
	 * I can only surmise the original intent of the MESSAGE_LENGTH state was to convey the incoming payload size for
	 * a UDP transport layer that was never completed by GoolagWillow so thats a TODO UPD transport layer 
	 * @param ctx the channel handler context
	 * @param e the array of bytes read from service server, which is a ByteBuffer with integer headers and an object payload
	 * @return the ServiceServerResponse instance from the decoder stack that we want to deliver, be it error or payload
	 */
  @Override
  public Object channelRead(ChannelHandlerContext ctx, Object e) throws Exception {
	log.info("ServiceResponseHandler channelRead for ChannelHandlerContext:"+ctx+" using Object:"+e);
    final ServiceResponseListener<ResponseType> listener = responseListeners.poll();
    assert(listener != null) : "No listener for incoming service response.";
    final ByteBuffer buffer = ByteBuffer.wrap((byte[]) e);
    final ServiceResponseDecoder decoder = new ServiceResponseDecoder();
    final List<Object> rstate = new ArrayList<Object>();
    ServiceServerResponse response = new ServiceServerResponse();
    executorService.execute(new Runnable() {
      @Override
      public void run() {
    	    try {
				decoder.decode(ServiceResponseDecoderState.ERROR_CODE.ordinal(), ctx, buffer, rstate);
				decoder.decode(ServiceResponseDecoderState.MESSAGE_LENGTH.ordinal(), ctx, buffer, rstate);
				decoder.decode(ServiceResponseDecoderState.MESSAGE.ordinal(), ctx, buffer, rstate);
				ServiceServerResponse sresponse = (ServiceServerResponse) rstate.get(0);
			    if (sresponse.getErrorCode() != ServiceResponseDecoderState.ERROR_CODE.ordinal()) {
					sresponse = (ServiceServerResponse) rstate.get(2);
					// TODO UDP transport?
					//sresponse = (ServiceServerResponse) rstate.get(1);
					//int messageLength = sresponse.getMessageLength();
			        listener.onSuccess((ResponseType) sresponse);
			    } else {
			    	sresponse = (ServiceServerResponse) rstate.get(2);
			        String message = Charset.forName("US-ASCII").decode(sresponse.getMessage()).toString();
			        listener.onFailure(new RemoteException(StatusCode.ERROR, message));
			    }
			    response.setErrorCode(sresponse.getErrorCode());
			    response.setMessageLength(sresponse.getMessageLength());
			    response.setMessage(sresponse.getMessage());
			} catch (Exception e1) {
				log.error("Error:"+e1+" decoding ServiceResponse for context:"+ctx+" using proposed ServiceResponse:"+e);
				e1.printStackTrace();
			}
      }
    });
    return response;
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
