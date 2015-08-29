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

package org.ros.internal.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.client.SlaveClient;
import org.ros.internal.node.parameter.ParameterManager;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.server.SlaveServer;
import org.ros.internal.node.server.master.MasterServer;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.namespace.GraphName;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MasterSlaveIntegrationTest {

  private MasterServer masterServer;
  private MasterClient masterClient;
  private SlaveServer slaveServer;
  private SlaveClient slaveClient;
  private ScheduledExecutorService executorService;

  @Before
  public void setUp() throws IOException {
    executorService = Executors.newScheduledThreadPool(10);
    try {
		masterServer = new MasterServer(BindAddress.newPrivate(), AdvertiseAddress.newPrivate());
	} catch (IOException e) {
		e.printStackTrace();
	}
    masterServer.start();
	masterClient = new MasterClient(masterServer.getUri(), 60000, 60000);

    TopicParticipantManager topicParticipantManager = new TopicParticipantManager();
    ServiceManager serviceManager = new ServiceManager();
    ParameterManager parameterManager = new ParameterManager(executorService);
    try {
		slaveServer =
		    new SlaveServer(GraphName.of("/foo"), BindAddress.newPrivate(),
		        AdvertiseAddress.newPrivate(), BindAddress.newPrivate(), AdvertiseAddress.newPrivate(),
		        masterClient, topicParticipantManager, serviceManager, parameterManager,
		        executorService);
	} catch (IOException e) {
		e.printStackTrace();
	}
    slaveServer.start();
    try {
		slaveClient = new SlaveClient(GraphName.of("/bar"), slaveServer.getUri());
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }

  @After
  public void tearDown() {
    try {
		masterServer.shutdown();
	} catch (IOException e) {
		e.printStackTrace();
	}
    executorService.shutdown();
  }

  @Test
  public void testGetMasterUri() {
    Response<URI> response = slaveClient.getMasterUri();
    assertEquals(masterServer.getUri(), response.getResult());
  }

  @Test
  public void testGetPid() {
    Response<Integer> response = slaveClient.getPid();
    assertTrue(response.getResult().toString().compareTo("0") > 0);
  }
}
