package org.ros.internal.transport.tcp;


/**
 * upstream = inbound, downstream = outbound for netty v4.x
 */
public abstract class AbstractNamedChannelHandler implements NamedChannelHandler {

  @Override
  public String toString() {
    return String.format("NamedChannelHandler<%s, %s>", getName(), super.toString());
  }
}
