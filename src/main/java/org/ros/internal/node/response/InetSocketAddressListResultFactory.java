/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.node.response;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A {@link ResultFactory} to take an object and turn it into a list of InetAddresss.
 * 
 * @author jg
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
