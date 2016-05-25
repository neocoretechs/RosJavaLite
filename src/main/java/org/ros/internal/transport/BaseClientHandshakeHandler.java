package org.ros.internal.transport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.internal.transport.tcp.AbstractNamedChannelHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

/**
 * Abstraction of top level ChannelHandler interface
 * Common functionality for {@link ClientHandshake} handlers.
 * 
 */
public abstract class BaseClientHandshakeHandler extends AbstractNamedChannelHandler {
  protected static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(BaseClientHandshakeHandler.class);
  private final ClientHandshake clientHandshake;
  private final ListenerGroup<ClientHandshakeListener> clientHandshakeListeners;

  public BaseClientHandshakeHandler(ClientHandshake clientHandshake, ExecutorService executorService) {
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
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.write(clientHandshake.getOutgoingConnectionHeader().encode());
  }
  
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
  	log.info("Channel inactive"+ctx);
  }

  @Override
  public Object channelRead(ChannelHandlerContext ctx, Object buff) throws Exception {
    ByteBuffer buffer = (ByteBuffer) buff;
    ConnectionHeader connectionHeader = ConnectionHeader.decode(buffer);
    if (clientHandshake.handshake(connectionHeader)) {
      onSuccess(connectionHeader, ctx);
      signalOnSuccess(connectionHeader);
    } else {
      onFailure(clientHandshake.getErrorMessage(), ctx);
      signalOnFailure(clientHandshake.getErrorMessage());
    }
    return buffer;
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext arg0) throws Exception {
  	if( DEBUG )
  		log.info("SubscriberHandshakeHandler.channelReadComplete:"+arg0);
  	
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
