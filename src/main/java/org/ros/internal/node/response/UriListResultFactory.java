package org.ros.internal.node.response;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link ResultFactory} to take an object and turn it into a list of InetSocketAddressess.
 * 
 * @author Jonathan Groff (C) NeoCoreTechs 2020
 */
public class UriListResultFactory implements ResultFactory<List<InetSocketAddress>> {

	@Override
	public List<InetSocketAddress> newFromValue(Object value) {
		List<Object> values = Arrays.asList((Object[]) value);
		List<InetSocketAddress> uris = new ArrayList<InetSocketAddress>();
		UriResultFactory factory = new UriResultFactory();
		for (Object uri : values) {
			InetSocketAddress socket = factory.newFromValue(uri);
			if(socket != null)
				uris.add(socket);
		}
		return uris;
	}
}
