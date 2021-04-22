package org.ros.node.service;

import org.ros.exception.RemoteException;

/**
 * A listener for service responses.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 * 
 * @param <MessageType>
 *          handles messages of this type
 */
public interface ServiceResponseListener<MessageType> {

  /**
   * Called when a service method returns successfully.
   * 
   * @param response
   *          the response message
   */
  void onSuccess(MessageType response);

  /**
   * Called when a service method fails to return successfully.
   * 
   * @param e
   *          the {@link RemoteException} received from the service
   */
  void onFailure(RemoteException e);

}
