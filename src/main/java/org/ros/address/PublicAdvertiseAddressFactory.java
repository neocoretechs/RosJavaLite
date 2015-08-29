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

package org.ros.address;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.ros.exception.RosRuntimeException;

/**
 * An {@link AdvertiseAddressFactory} which creates public (non-loopback) addresses.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 * @author jg
 */
public class PublicAdvertiseAddressFactory implements AdvertiseAddressFactory {

  private final InetAddress host;

  public PublicAdvertiseAddressFactory() {
    this(InetSocketAddressFactory.newNonLoopback());
  }

  public PublicAdvertiseAddressFactory(InetAddress host) {
	    this.host = host;
  }
  
  public PublicAdvertiseAddressFactory(String host) {
	    try {
			this.host = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			throw new RosRuntimeException("Host name "+host+" cannot be resolved");
		}
}

  public AdvertiseAddress newDefault(int port) {
    return new AdvertiseAddress(host, port);
  }

  @Override
  public AdvertiseAddress newDefault() {
	InetSocketAddress isa = InetSocketAddressFactory.newLoopback();
	return new AdvertiseAddress(isa);
  }
}
