package org.ros.internal.transport.tcp;

//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.group.ChannelGroup;
//import org.jboss.netty.channel.group.DefaultChannelGroup;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelHandlerContextImpl;
import org.ros.internal.transport.ChannelPipeline;
import org.ros.internal.transport.ChannelPipelineImpl;

/**
 * TcpClientManager manages TCP clients which are the subscriber and service clients that communicate with
 * remote peers outside master domain. 
 * It requires the context with the channel group, and executor service constructed.
 * @author jg
 */
public class TcpClientManager {

  private final AsynchronousChannelGroup channelGroup;
  private final Collection<TcpClient> tcpClients;
  private final List<NamedChannelHandler> namedChannelHandlers;
  private final Executor executor;
  private ChannelPipeline pipeline;

  public TcpClientManager(ExecutorService executor) throws IOException {
    this.executor = executor;
    this.channelGroup = AsynchronousChannelGroup.withThreadPool(executor);
    this.tcpClients = new ArrayList<TcpClient>();
    this.namedChannelHandlers = new ArrayList<NamedChannelHandler>();
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
 * @throws IOException 
   */
  public TcpClient connect(String connectionName, SocketAddress socketAddress) throws Exception {
    TcpClient tcpClient = new TcpClient(executor, pipeline, channelGroup);
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
    channelGroup.shutdown();
    tcpClients.clear();
    // We don't call channelFactory.releaseExternalResources() or
    // bootstrap.releaseExternalResources() since the only external resource is
    // the ExecutorService which must remain in the control of the overall
    // application.
  }
}
