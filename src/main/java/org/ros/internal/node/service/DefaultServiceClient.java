package org.ros.internal.node.service;


import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.MessageBufferPool;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ClientHandshakeListener;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.internal.transport.tcp.TcpClient;
import org.ros.internal.transport.tcp.TcpClientManager;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of a {@link ServiceClient}.
 * 
 * @author jg
 */
public class DefaultServiceClient<T, S> implements ServiceClient<T, S> {

  private final class HandshakeLatch implements ClientHandshakeListener {

    private CountDownLatch latch;
    private boolean success;
    private String errorMessage;

    @Override
    public void onSuccess(ConnectionHeader outgoingConnectionHeader,
        ConnectionHeader incomingConnectionHeader) {
      success = true;
      latch.countDown();
    }

    @Override
    public void onFailure(ConnectionHeader outgoingConnectionHeader, String errorMessage) {
      this.errorMessage = errorMessage;
      success = false;
      latch.countDown();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
      latch.await(timeout, unit);
      return success;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public void reset() {
      latch = new CountDownLatch(1);
      success = false;
      errorMessage = null;
    }
  }

  private final ServiceDeclaration serviceDeclaration;
  
  private final MessageFactory messageFactory;
  private final MessageBufferPool messageBufferPool;
  private final Queue<ServiceResponseListener<S>> responseListeners;
  private final ConnectionHeader connectionHeader;
  private final TcpClientManager tcpClientManager;
  private final HandshakeLatch handshakeLatch;

  private TcpClient tcpClient;

  public static <S, T> DefaultServiceClient<S, T> newDefault(GraphName nodeName,
      ServiceDeclaration serviceDeclaration,
      MessageFactory messageFactory,
      ScheduledExecutorService executorService) throws IOException {
    return new DefaultServiceClient<S, T>(nodeName, serviceDeclaration, messageFactory, executorService);
  }

  private DefaultServiceClient(GraphName nodeName, ServiceDeclaration serviceDeclaration,
      MessageFactory messageFactory, ScheduledExecutorService executorService) throws IOException {
    this.serviceDeclaration = serviceDeclaration;
    this.messageFactory = messageFactory;
    messageBufferPool = new MessageBufferPool();
    responseListeners = new LinkedList<ServiceResponseListener<S>>();
    connectionHeader = new ConnectionHeader();
    connectionHeader.addField(ConnectionHeaderFields.CALLER_ID, nodeName.toString());
    // TODO(damonkohler): Support non-persistent connections.
    connectionHeader.addField(ConnectionHeaderFields.PERSISTENT, "1");
    connectionHeader.merge(serviceDeclaration.toConnectionHeader());
    tcpClientManager = TcpClientManager.getInstance(executorService);
    ServiceClientHandshakeHandler<T, S> serviceClientHandshakeHandler =
        new ServiceClientHandshakeHandler<T, S>(connectionHeader, responseListeners, executorService);
    handshakeLatch = new HandshakeLatch();
    serviceClientHandshakeHandler.addListener(handshakeLatch);
    tcpClientManager.addNamedChannelHandler(serviceClientHandshakeHandler);
  }

  @Override
  public void connect(InetSocketAddress inetSocketAddress) throws Exception {
    assert(inetSocketAddress != null) : "Address must be specified.";
    //assert(inetSocketAddress.getScheme().equals("rosrpc")) : "Invalid service URI.";
    assert(tcpClient == null) : "Already connected once.";
    handshakeLatch.reset();
    try {
		tcpClient = tcpClientManager.connect(toString(), inetSocketAddress);
	} catch (IOException e1) {
		throw new RosRuntimeException("TcpClient failed to connect to remote "+toString()+" "+inetSocketAddress, e1);
	}
    try {
      if (!handshakeLatch.await(1, TimeUnit.SECONDS)) {
        throw new RosRuntimeException(handshakeLatch.getErrorMessage());
      }
    } catch (InterruptedException e) {
      throw new RosRuntimeException("Handshake timed out.");
    }
  }

  @Override
  public void shutdown() {
    assert(tcpClient != null) : "Not connected.";
    tcpClientManager.shutdown();
  }

  @Override
  public void call(T request, ServiceResponseListener<S> listener) {
    //ByteBuffer buffer = messageBufferPool.acquire();
    // serialize request to buffer
    //Utility.serialize(request, buffer);
    responseListeners.add(listener);
    try {
		tcpClient.getContext().write(request);
	} catch (IOException e) {
		
		e.printStackTrace();
	}
    //messageBufferPool.release(buffer);
  }

  @Override
  public GraphName getName() {
    return serviceDeclaration.getName();
  }

  @Override
  public String toString() {
    return "ServiceClient<" + serviceDeclaration + ">";
  }

  @Override
  public T newMessage() {
    return messageFactory.newFromType(serviceDeclaration.getType());
  }
}
