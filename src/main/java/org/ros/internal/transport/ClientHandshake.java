package org.ros.internal.transport;

/**
 * Encapsulates client-side transport handshake logic.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface ClientHandshake {

  /**
   * @param incommingConnectionHeader
   *          the {@link ConnectionHeader} sent by the server
   * @return {@code true} if the handshake is successful, {@code false}
   *         otherwise
   */
  boolean handshake(ConnectionHeader incommingConnectionHeader);

  /**
   * @return the outgoing {@link ConnectionHeader}
   */
  ConnectionHeader getOutgoingConnectionHeader();

  /**
   * @return the error {@link String} returned by the server if an error occurs,
   *         {@code null} otherwise
   */
  String getErrorMessage();
}
