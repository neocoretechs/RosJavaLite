package org.ros.internal.node.response;

import java.net.InetSocketAddress;

/**
 * Extract value from array of arrays of objects, 2 elements host and port
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class InetSocketAddressResultFactory implements ResultFactory<InetSocketAddress> {

  @Override
  public InetSocketAddress newFromValue(Object value) {
    for (Object pair : (Object[]) value) {
		return new InetSocketAddress((String) ((Object[]) pair)[0],(int)((Object[]) pair)[1]);
	}
    return null;
  }
}
