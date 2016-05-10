package org.ros.internal.transport;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelStateEvent;
//import org.jboss.netty.channel.MessageEvent;

import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.internal.transport.tcp.AbstractNamedChannelHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

/**
 * Common functionality for {@link ClientHandshake} handlers.
 * 
 */
public abstract class BaseClientHandshakeHandler extends AbstractNamedChannelHandler {
  private static boolean DEBUG = true;
  private final ClientHandshake clientHandshake;
  private final ListenerGroup<ClientHandshakeListener> clientHandshakeListeners;

  public BaseClientHandshakeHandler(ClientHandshake clientHandshake, ExecutorService executorService) {
    this.clientHandshake = clientHandshake;
    clientHandshakeListeners = new ListenerGroup<ClientHandshakeListener>(executorService);
  }

  public void addListener(ClientHandshakeListener clientHandshakeListener) {
    clientHandshakeListeners.add(clientHandshakeListener);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.channel().write(clientHandshake.getOutgoingConnectionHeader().encode());
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object buff) throws Exception {
    ByteBuf buffer = (ByteBuf) buff;
    ConnectionHeader connectionHeader = ConnectionHeader.decode(buffer);
    if (clientHandshake.handshake(connectionHeader)) {
      onSuccess(connectionHeader, ctx);
      signalOnSuccess(connectionHeader);
    } else {
      onFailure(clientHandshake.getErrorMessage(), ctx);
      signalOnFailure(clientHandshake.getErrorMessage());
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext arg0) throws Exception {
  	if( DEBUG )
  		System.out.println("SubscriberHandshakeHandler.channelReadComplete:"+arg0);
  	
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
  protected abstract void onSuccess(ConnectionHeader incommingConnectionHeader,
      ChannelHandlerContext ctx);

  private void signalOnSuccess(final ConnectionHeader incommingConnectionHeader) {
    clientHandshakeListeners.signal(new SignalRunnable<ClientHandshakeListener>() {
      @Override
      public void run(ClientHandshakeListener listener) {
        listener.onSuccess(clientHandshake.getOutgoingConnectionHeader(), incommingConnectionHeader);
      }
    });
  }

  protected abstract void onFailure(String errorMessage, ChannelHandlerContext ctx);

  private void signalOnFailure(final String errorMessage) {
    clientHandshakeListeners.signal(new SignalRunnable<ClientHandshakeListener>() {
      @Override
      public void run(ClientHandshakeListener listener) {
        listener.onFailure(clientHandshake.getOutgoingConnectionHeader(), errorMessage);
      }
    });
  }
}
