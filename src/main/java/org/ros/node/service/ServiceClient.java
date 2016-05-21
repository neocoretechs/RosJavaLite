package org.ros.node.service;

import org.ros.namespace.GraphName;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Provides a connection to a ROS service.
 * 
 * @author jg
 * 
 * @param <T>
 *          the {@link ServiceServer} responds to requests of this type
 * @param <S>
 *          the {@link ServiceServer} returns responses of this type
 */
public interface ServiceClient<T, S> {

  /**
   * Connects to a {@link ServiceServer}.
   * 
   * @param uri
   *          the {@link URI} of the {@link ServiceServer} to connect to
 * @throws Exception 
   */
  void connect(InetSocketAddress uri) throws Exception;

  /**
   * Calls a method on the {@link ServiceServer}.
   * 
   * @param request
   *          the request message
   * @param listener
   *          the {@link ServiceResponseListener} that will handle the response
   *          to this request
   */
  void call(T request, ServiceResponseListener<S> listener);

  /**
   * @return the name of the service this {@link ServiceClient} is connected to
   */
  GraphName getName();

  /**
   * Stops the client (e.g. disconnect a persistent service connection).
   */
  void shutdown();
  
  /**
   * @return a new request message
   */
  T newMessage();
}