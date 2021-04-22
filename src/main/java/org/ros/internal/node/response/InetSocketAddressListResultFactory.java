
package org.ros.internal.node.response;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link ResultFactory} to take an object and turn it into a list of InetAddresss.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class InetSocketAddressListResultFactory implements ResultFactory<List<InetSocketAddress>> {

	@Override
	public List<InetSocketAddress> newFromValue(Object value) {
		List<Object> values = Arrays.asList(value);
		List<InetSocketAddress> uris = new ArrayList<InetSocketAddress>();
		for (Object uri : values) {
			List<InetSocketAddress> arrayValue = (List<InetSocketAddress>)uri;
			for( InetSocketAddress addr : arrayValue) {
				uris.add(addr);
			}
		}
		return uris;
	}
}
