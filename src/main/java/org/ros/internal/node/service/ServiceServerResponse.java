package org.ros.internal.node.service;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Response object returned from the service server upon completion of the service via client invocation.
 * The instance is sent as a byte payload which contains a bytebuffer with integer headers indicating
 * message size and error status presumably in support of UDP transport layer at some point.<p/>
 * These headers are extracted using a decoder, and then the payload that represents an instance of this object is
 * contained in the remainder of the byte payload and deserialized. The instance is sent on the event handling
 * internal bus, then multiplexed onto the TCP bus back to the client.
 * @author Jonathan Neville Groff (C) NeoCoreTechs 2020
*/
class ServiceServerResponse implements Serializable {
  private static final long serialVersionUID = -5429450419100506243L;
  private ByteBuffer message;
  private int errorCode;
  private int messageLength;

  public ServiceServerResponse() {}
  
  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public void setMessage(ByteBuffer buffer) {
    message = buffer;
  }

  public ByteBuffer getMessage() {
    return message;
  }

  public void setMessageLength(int messageLength) {
    this.messageLength = messageLength;
  }

  public int getMessageLength() {
    return messageLength;
  }
}
