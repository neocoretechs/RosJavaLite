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

package org.ros.internal.node.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.internal.node.client.ParameterClient;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;


/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ParameterServerTest {

  private ParameterClient server;

  @Before
  public void setup() throws IOException {
	  InetSocketAddress testServer = new InetSocketAddress("172.16.0.101", 8090);
	  InetSocketAddress paramServer = new InetSocketAddress("172.16.0.101",8091);
    server = new ParameterClient(new NodeIdentifier(GraphName.of("/foo"), testServer ), testServer);
  }

  @Test
  public void testGetNonExistent() {
    assertEquals(new Object(), server.getParam(GraphName.of("/foo")).getResult());
    assertEquals(new Object(), server.getParam(GraphName.of("/foo/bar")).getResult());
  }

  @Test
  public void testSetAndGetShallow() {
    server.setParam(GraphName.of("/foo"), "bloop");
    assertEquals("bloop", server.getParam(GraphName.of("/foo")).getResult());
  }

  @Test
  public void testSetAndGetDeep() {
    server.setParam(GraphName.of("/foo/bar"), "bloop");
    assertEquals("bloop", server.getParam(GraphName.of("/foo/bar")).getResult());
  }

  @Test
  public void testSetAndGet() {
    server.setParam(GraphName.of("/foo"), "bloop");
    assertEquals("bloop", server.getParam(GraphName.of("/foo")).getResult());
    server.setParam(GraphName.of("/foo/bar"), "bloop");
    assertEquals("bloop", server.getParam(GraphName.of("/foo/bar")).getResult());
    server.setParam(GraphName.of("/foo/bar/baz"), "bloop");
    assertEquals("bloop", server.getParam(GraphName.of("/foo/bar/baz")).getResult());
  }

  @Test
  public void testSetDeepAndGetShallow() {
    server.setParam(GraphName.of("/foo/bar"), "bloop");
    Map<String, Object> expected = new HashMap<String, Object>();
    expected.put("bar", "bloop");
    assertEquals(expected, server.getParam(GraphName.of("/foo")).getResult());
  }

  @Test
  public void testSetOverwritesMap() {
    server.setParam(GraphName.of("/foo/bar"), "bloop");
    assertEquals("bloop", server.getParam(GraphName.of("/foo/bar")).getResult());
    server.setParam(GraphName.of("/foo"), "bloop");
    assertEquals("bloop", server.getParam(GraphName.of("/foo")).getResult());
  }

  @Test
  public void testSetAndGetFloat() {
    GraphName name = GraphName.of("/foo/bar");
    server.setParam(name, new Float(0.42f));
    assertEquals(0.42, (Float) server.getParam(name).getResult(), 0.1);
  }

  @Test
  public void testDeleteShallow() {
    GraphName name = GraphName.of("/foo");
    server.setParam(name, "bloop");
    server.deleteParam(name);
    assertEquals(new Object(), server.getParam(name).getResult());
  }

  @Test
  public void testDeleteDeep() {
    GraphName name = GraphName.of("/foo/bar");
    server.setParam(name, "bloop");
    server.deleteParam(name);
    assertEquals(null, server.getParam(name).getResult());
  }

  @Test
  public void testHas() {
    server.setParam(GraphName.of("/foo/bar/baz"), "bloop");
    assertTrue((Boolean)server.hasParam(GraphName.of("/foo/bar/baz")).getResult());
    assertTrue((Boolean)server.hasParam(GraphName.of("/foo/bar")).getResult());
    assertTrue((Boolean)server.hasParam(GraphName.of("/foo")).getResult());
    assertTrue((Boolean)server.hasParam(GraphName.of("/")).getResult());
  }

  @Test
  public void testGetNames() {
    GraphName name1 = GraphName.of("/foo/bar/baz");
    server.setParam(name1, "bloop");
    GraphName name2 = GraphName.of("/testing");
    server.setParam(name2, "123");
    Collection<GraphName> names = server.getParamNames().getResult();
    assertEquals(2, names.size());
    assertTrue(names.contains(name1));
    assertTrue(names.contains(name2));
  }

}
