package org.ros.internal.node.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.system.Process;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Base class for an RPC server.
 * Starts the TCP request processor that maintains master/slave communications at top level registration.
 * Constructs a BaseServer.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public abstract class RpcServer {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(RpcServer.class);

  private final AdvertiseAddress advertiseAddress;
  protected final BindAddress bindAddress;
  private final CountDownLatch startLatch = new CountDownLatch(1);
  private BaseServer server;

  public RpcServer(BindAddress bindAddress, AdvertiseAddress advertiseAddress) throws IOException {
	this.bindAddress = bindAddress;
    this.advertiseAddress = advertiseAddress;
  }

  /**
   * Invoke a method via remote call.
   * @param rri The RemoteRequestInterface passed from remote client
   * @return The Object result of invocation
   * @throws Exception
   */
  public abstract Object invokeMethod(RemoteRequestInterface rri) throws Exception;
  
  /**
   * Start up the remote calling server.
   * 
   * @param instanceClass the class of the remoting server
   * @param instance an instance of the remoting server class
   */
  public void start() {
    try {
      server = new BaseServer(this);
      server.startServer();
    } catch (IOException e) {
      throw new RosRuntimeException(e);
    }
    if (DEBUG) {
      log.info("RpcServer starting and bound to: " + getUri());
    }
    startLatch.countDown();
  }

  /**
   * Shut the remote call server down.
   * @throws IOException 
   */
  public void shutdown() throws IOException {
    server.stopServer();
  }

  public void close(TCPWorker tcpWorker) {
	server.close(tcpWorker);
  }
  /**
   * @return the {@link URI} of the server
   */
  public InetSocketAddress getUri() {
    //return advertiseAddress.toUri("http");
    return advertiseAddress.toInetSocketAddress();
  }

  public InetSocketAddress getAddress() {
    return advertiseAddress.toInetSocketAddress();
  }

  public AdvertiseAddress getAdvertiseAddress() {
    return advertiseAddress;
  }

  public void awaitStart() throws InterruptedException {
    startLatch.await();
  }

  public boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
    return startLatch.await(timeout, unit);
  }

  /**
   * @return PID of node process if available
   * @throws {@link UnsupportedOperationException} otherwise.
   */
  public long getPid() {
    return Process.getPid();
  }
  
  public String toString() {
	  return "RpcServer at address "+advertiseAddress;
  }

}
