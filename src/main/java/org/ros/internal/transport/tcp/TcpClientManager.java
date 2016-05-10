package org.ros.internal.transport.tcp;

//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.group.ChannelGroup;
//import org.jboss.netty.channel.group.DefaultChannelGroup;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.EventExecutor;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TcpClientManager {

  private final ChannelGroup channelGroup;
  private final Collection<TcpClient> tcpClients;
  private final List<NamedChannelHandler> namedChannelHandlers;
  private final Executor executor;

  public TcpClientManager(EventExecutor executor) {
    this.executor = executor;
    channelGroup = new DefaultChannelGroup(executor);
    tcpClients = new ArrayList<TcpClient>();
    namedChannelHandlers = new ArrayList<NamedChannelHandler>();
  }

  public void addNamedChannelHandler(NamedChannelHandler namedChannelHandler) {
    namedChannelHandlers.add(namedChannelHandler);
  }

  public void addAllNamedChannelHandlers(List<NamedChannelHandler> namedChannelHandlers) {
    this.namedChannelHandlers.addAll(namedChannelHandlers);
  }

  /**
   * Connects to a server.
   * <p>
   * This call blocks until the connection is established or fails.
   * 
   * @param connectionName
   *          the name of the new connection
   * @param socketAddress
   *          the {@link SocketAddress} to connect to
   * @return a new {@link TcpClient}
   */
  public TcpClient connect(String connectionName, SocketAddress socketAddress) {
    TcpClient tcpClient = new TcpClient(channelGroup, (EventLoopGroup) executor);
    tcpClient.addAllNamedChannelHandlers(namedChannelHandlers);
    tcpClient.connect(connectionName, socketAddress);
    tcpClients.add(tcpClient);
    return tcpClient;
  }

  /**
   * Sets all {@link TcpClientConnection}s as non-persistent and closes all open
   * {@link Channel}s.
   */
  public void shutdown() {
    channelGroup.close().awaitUninterruptibly();
    tcpClients.clear();
    // We don't call channelFactory.releaseExternalResources() or
    // bootstrap.releaseExternalResources() since the only external resource is
    // the ExecutorService which must remain in the control of the overall
    // application.
  }
}
