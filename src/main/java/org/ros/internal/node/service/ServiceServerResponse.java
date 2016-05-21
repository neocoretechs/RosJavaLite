package org.ros.internal.node.service;

import java.nio.ByteBuffer;

/**
*/
class ServiceServerResponse {
  
  private ByteBuffer message;
  private int errorCode;
  private int messageLength;

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
