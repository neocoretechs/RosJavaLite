package org.ros.internal.node;

import org.ros.internal.transport.ClientHandshake;
import org.ros.internal.transport.ConnectionHeader;

/**
 * An abstract {@link ClientHandshake} implementation for convenience. 
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public abstract class BaseClientHandshake implements ClientHandshake {

  protected final ConnectionHeader outgoingConnectionHeader;
  
  private String errorMessage;

  public BaseClientHandshake(ConnectionHeader outgoingConnectionHeader) {
    this.outgoingConnectionHeader = outgoingConnectionHeader;
  }

  @Override
  public ConnectionHeader getOutgoingConnectionHeader() {
    return outgoingConnectionHeader;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }
  
  protected void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
