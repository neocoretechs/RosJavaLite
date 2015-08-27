package org.ros.internal.node.response;

import java.net.InetAddress;

import java.net.UnknownHostException;

import org.ros.exception.RosRuntimeException;

/**
 * @author jg
 */
public class InetAddressResultFactory implements ResultFactory<InetAddress> {

  @Override
  public InetAddress newFromValue(Object value) {
    try {
      return InetAddress.getByName((String) value);
    } catch (UnknownHostException e) {
      throw new RosRuntimeException(e);
    }
  }
}
