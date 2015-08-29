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

package org.ros;

import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.internal.node.server.ParameterServer;
import org.ros.internal.node.server.master.MasterServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

// TODO(damonkohler): Add /rosout node.
/**
 * {@link RosCore} is a collection of nodes and programs that are pre-requisites
 * of a ROS-based system. You must have a {@link RosCore}
 * running in order for ROS nodes to communicate.
 * The light weight implementation will not interop with standard ROS
 * nodes, the XML RPC has been eliminated in favor of lightweight java serialization protocol.
 * @see <a href="http://www.ros.org/wiki/roscore">roscore documentation</a>
 * 
 * @author damonkohler@google.com (Damon Kohler)
 * @author jg
 */
public class RosCore {
  private static boolean DEBUG = true;
  private MasterServer masterServer = null;
  private ParameterServer parameterServer = null;

  public static RosCore newPublic(String host, int port) {
    return new RosCore(BindAddress.newPublic(port), new AdvertiseAddress(host, port));
  }

  public static RosCore newPublic(int port) {
    return new RosCore(BindAddress.newPublic(port), AdvertiseAddress.newPublic(port));
  }


  public static RosCore newPrivate() {
	BindAddress ba = BindAddress.newPrivate();
    return new RosCore(ba, AdvertiseAddress.newPrivate());
  }

  private RosCore(BindAddress bindAddress, AdvertiseAddress advertiseAddress) {
	  if(DEBUG)
		  System.out.println("RosCore initialization with bind:"+bindAddress+" advertise:"+advertiseAddress);
    try {
		masterServer = new MasterServer(bindAddress, advertiseAddress);
		parameterServer = new ParameterServer(bindAddress, new AdvertiseAddress(advertiseAddress.getHost(),advertiseAddress.getPort()+1));
	} catch (IOException e) {
		System.out.println("RosCore fault, master server can not be constructed due to "+e);
		e.printStackTrace();
	}
  }

  public void start() {
    masterServer.start();
    parameterServer.start();
  }

  public InetSocketAddress getUri() {
    return masterServer.getUri();
  }

  public void awaitStart() throws InterruptedException {
    masterServer.awaitStart();
    parameterServer.awaitStart();
  }

  public boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
    boolean ms = masterServer.awaitStart(timeout, unit);
    boolean ps = parameterServer.awaitStart(timeout,  unit);
    return ms & ps;
  }

  public void shutdown() {
    try {
    	parameterServer.shutdown();
		masterServer.shutdown();
	} catch (IOException e) {
		System.out.println("Can not shut down master server due to "+e);
		e.printStackTrace();
	}
  }

  public ParameterServer getParameterServer() {
	  return parameterServer;
  }
  
  public MasterServer getMasterServer() {
    return masterServer;
  }
  
  public static void main(String[] args) throws Exception {
	   RosCore rosCore = RosCore.newPublic(8090);
	   rosCore.start();
	   rosCore.awaitStart(1, TimeUnit.SECONDS);
	   System.out.println("RosLite Master started @ address "+rosCore.getUri());
  }
}
