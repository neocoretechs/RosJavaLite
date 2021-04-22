
package org.ros.internal.node.response;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ros.exception.RosRuntimeException;

/**
 * A {@link ResultFactory} to take an object and turn it into a list of InetAddresss.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class InetAddressListResultFactory implements ResultFactory<List<InetAddress>> {

	@Override
	public List<InetAddress> newFromValue(Object value) {
		/*
		List<Object> values = Arrays.asList(value);
		List<InetAddress> uris = new ArrayList<InetAddress>();
		for (Object uri : values) {
			try {
				uris.add( InetAddress.getByName((String)uri));
			} catch (UnknownHostException e) {
				throw new RosRuntimeException(e);
			}
		}
		return uris;
		*/
		return (List<InetAddress>)value;
	}
}
