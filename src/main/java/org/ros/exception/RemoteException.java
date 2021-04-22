package org.ros.exception;

import org.ros.internal.node.response.StatusCode;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class RemoteException extends RosRuntimeException {

  private final StatusCode statusCode;

  public RemoteException(StatusCode statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * @return the status code
   */
  public StatusCode getStatusCode() {
    return statusCode;
  }
}
