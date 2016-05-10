
package org.ros.internal.node.service;

import io.netty.buffer.ByteBuf;

/**
*/
class ServiceServerResponse {
  
  private ByteBuf message;
  private int errorCode;
  private int messageLength;

  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public void setMessage(ByteBuf buffer) {
    message = buffer;
  }

  public ByteBuf getMessage() {
    return message;
  }

  public void setMessageLength(int messageLength) {
    this.messageLength = messageLength;
  }

  public int getMessageLength() {
    return messageLength;
  }
}
