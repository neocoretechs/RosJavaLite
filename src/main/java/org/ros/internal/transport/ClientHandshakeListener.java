package org.ros.internal.transport;

/**
 * A listener for handshake events.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface ClientHandshakeListener {

  /**
   * Called when the {@link ClientHandshake} completes successfully.
   * 
   * @param outgoingConnectionHeader
   * @param incomingConnectionHeader
   */
  void onSuccess(ConnectionHeader outgoingConnectionHeader,
      ConnectionHeader incomingConnectionHeader);

  /**
   * Called when the {@link ClientHandshake} fails.
   * 
   * @param outgoingConnectionHeader
   * @param errorMessage
   */
  void onFailure(ConnectionHeader outgoingConnectionHeader, String errorMessage);
}
