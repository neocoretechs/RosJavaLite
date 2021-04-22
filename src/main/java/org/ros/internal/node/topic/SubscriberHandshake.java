package org.ros.internal.node.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.node.BaseClientHandshake;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;

/**
 * Handshake logic from the subscriber side of a topic connection.
 * The publisher receives the request for fields associated with the topic.
 * The subscriber receives the return from the publisher with the available fields and the
 * MD5 checksum to verify the message and complete the handshake.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class SubscriberHandshake extends BaseClientHandshake {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(SubscriberHandshake.class);

  public SubscriberHandshake(ConnectionHeader outgoingConnectionHeader) {
    super(outgoingConnectionHeader);
    assert(outgoingConnectionHeader.getField(ConnectionHeaderFields.TYPE) != null);
    assert(outgoingConnectionHeader.getField(ConnectionHeaderFields.MD5_CHECKSUM) != null);
  }

  @Override
  public boolean handshake(ConnectionHeader incommingConnectionHeader) {
    if (DEBUG) {
      log.info("Outgoing subscriber connection header: " + outgoingConnectionHeader);
      log.info("Incoming publisher connection header: " + incommingConnectionHeader);
    }
    setErrorMessage(incommingConnectionHeader.getField(ConnectionHeaderFields.ERROR));
    String incomingType = incommingConnectionHeader.getField(ConnectionHeaderFields.TYPE);
    if (incomingType == null) {
      setErrorMessage("Incoming type cannot be null.");
    } else if (!incomingType.equals(outgoingConnectionHeader.getField(ConnectionHeaderFields.TYPE))) {
      setErrorMessage("Message types don't match.");
    }
    String incomingMd5Checksum =
        incommingConnectionHeader.getField(ConnectionHeaderFields.MD5_CHECKSUM);
    if (incomingMd5Checksum == null) {
      setErrorMessage("Incoming MD5 checksum cannot be null.");
    } else if (!incomingMd5Checksum.equals(outgoingConnectionHeader
        .getField(ConnectionHeaderFields.MD5_CHECKSUM))) {
      setErrorMessage("Checksums don't match.");
    }
    return getErrorMessage() == null;
  }
}
