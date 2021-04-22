package org.ros.node.service;

import org.ros.namespace.GraphName;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Provides a ROS service.
 * 
 * @see <a href="http://www.ros.org/wiki/Services">Services documentation</a>
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 * 
 * @param <T>
 *          this {@link ServiceServer} responds to requests of this type
 * @param <S>
 *          this {@link ServiceServer} returns responses of this type
 */
public interface ServiceServer<T, S> {

  /**
   * @return the name of the {@link ServiceServer}
   */
  GraphName getName();

  /**
   * @return the {@link URI} for this {@link ServiceServer}
   */
  InetSocketAddress getUri();

  /**
   * Stops the service and unregisters it.
   */
  void shutdown();

  /**
   * Add a {@link ServiceServerListener}.
   * 
   * @param listener
   *          the {@link ServiceServerListener} to add
   */
  void addListener(ServiceServerListener<T, S> listener);
}
