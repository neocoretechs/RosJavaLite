package org.ros.internal.transport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.internal.message.MessageBuffers;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.tcp.AbstractNamedChannelHandler;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

/**
 * Abstraction of top level ChannelHandler interface.
 * Common functionality for {@link ClientHandshake} handlers.
 * @author jg
 * 
 */
public abstract class BaseClientHandshakeHandler extends AbstractNamedChannelHandler {
  protected static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(BaseClientHandshakeHandler.class);
  private final ClientHandshake clientHandshake;
  private final ListenerGroup<ClientHandshakeListener> clientHandshakeListeners;
  protected final ExecutorService executor;

  public BaseClientHandshakeHandler(ClientHandshake clientHandshake, ExecutorService executorService) {
	this.executor = executorService;
    this.clientHandshake = clientHandshake;
    clientHandshakeListeners = new ListenerGroup<ClientHandshakeListener>(executorService);
  }
  /**
   * Primarily services?
   * @param clientHandshakeListener
   */
  public void addListener(ClientHandshakeListener clientHandshakeListener) {
    clientHandshakeListeners.add(clientHandshakeListener);
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
	//final ByteBuffer bb = MessageBuffers.dynamicBuffer();
	if( DEBUG ) {
		log.info("BaseClientHandshakeHandler channelActive for ChannelHandlerContext:"+ctx+" for:"+this+" Preparing OutgoingConnectionHeader:"+clientHandshake.getOutgoingConnectionHeader());
	}
	
	ctx.write(clientHandshake.getOutgoingConnectionHeader());
	
	if( DEBUG )
		log.info("BaseClientHandshakeHandler channelActive for ChannelHandlerContext "+ctx+" OutgoingConnectionHeader reply to master complete for:"+this);	
	/*
	Utility.serialize(clientHandshake.getOutgoingConnectionHeader(), bb);
	
    ctx.write(bb, new CompletionHandler<Integer, Void>() {
		@Override
		public void completed(Integer arg0, Void arg1) {
			if( DEBUG )
				log.info("BaseClientHandshakeHandler channelActive "+ctx+" reply to master complete with "+arg0+" buffer:"+bb);	
		}
		@Override
		public void failed(Throwable arg0, Void arg1) {
			log.info("BaseClientHandshakeHandler channelActive "+ctx+" reply to master failed with:"+arg0);	
		}
    	
    });
    */
  }
  
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
  	log.info("Channel inactive ChannelHandlerContext:"+ctx+" for:"+this);
  }

  @Override
  public Object channelRead(ChannelHandlerContext ctx, Object buff) throws Exception {
    ConnectionHeader connectionHeader = (ConnectionHeader)buff;
    if (clientHandshake.handshake(connectionHeader)) {
      onSuccess(connectionHeader, ctx);
      signalOnSuccess(connectionHeader);
    } else {
      onFailure(clientHandshake.getErrorMessage(), ctx);
      signalOnFailure(clientHandshake.getErrorMessage());
    }
    return buff;
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext arg0) throws Exception {
  	if( DEBUG )
  		log.info("SubscriberHandshakeHandler.channelReadComplete:"+arg0+" for:"+this);
  	
  }
  /**
   * Called when the {@link ClientHandshake} succeeds and will block the network
   * thread until it returns.
   * <p>
   * This must block in order to allow changes to the pipeline to be made before
   * further messages arrive.
   * 
   * @param incommingConnectionHeader
   * @param ctx
   * @param e
   */
  protected abstract void onSuccess(ConnectionHeader incommingConnectionHeader,ChannelHandlerContext ctx);

  private void signalOnSuccess(final ConnectionHeader incommingConnectionHeader) {
    clientHandshakeListeners.signal(new SignalRunnable<ClientHandshakeListener>() {
      @Override
      public void run(ClientHandshakeListener listener) {
        listener.onSuccess(clientHandshake.getOutgoingConnectionHeader(), incommingConnectionHeader);
      }
    });
  }

  protected abstract void onFailure(String errorMessage, ChannelHandlerContext ctx) throws IOException;

  private void signalOnFailure(final String errorMessage) {
    clientHandshakeListeners.signal(new SignalRunnable<ClientHandshakeListener>() {
      @Override
      public void run(ClientHandshakeListener listener) {
        listener.onFailure(clientHandshake.getOutgoingConnectionHeader(), errorMessage);
      }
    });
  }
}
