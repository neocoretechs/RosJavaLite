package org.ros.node.service;

import org.ros.exception.ServiceException;

/**
 * Builds a service response given a service request.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 * 
 * @param <T>
 *          the {@link ServiceServer} responds to requests of this type
 * @param <S>
 *          the {@link ServiceServer} returns responses of this type
 */
public interface ServiceResponseBuilder<T, S> {

  /**
   * Builds a service response given a service request.
   * 
   * @param request
   *          the received request
   * @param response
   *          the response that will be sent
   * @throws ServiceException
   */
  void build(T request, S response) throws ServiceException;
}
