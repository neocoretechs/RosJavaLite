package org.ros.node.service;

import org.ros.internal.node.RegistrantListener;

/**
 * A lifecycle listener for {@link ServiceServer} instances.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 * 
 * @param <T> the {@link ServiceServer} responds to requests of this type
 * @param <S> the {@link ServiceServer} returns responses of this type
 */
public interface ServiceServerListener<T, S> extends RegistrantListener<ServiceServer<T, S>> {

  /**
   * @param serviceServer the {@link ServiceServer} which has been shut down
   */
  void onShutdown(ServiceServer<T, S> serviceServer);
}
