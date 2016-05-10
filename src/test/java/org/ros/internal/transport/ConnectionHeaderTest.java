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

package org.ros.internal.transport;

import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;

import org.junit.Test;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ConnectionHeaderTest {

  @Test
  public void testEncodeAndDecode() {
    ConnectionHeader connectionHeader = new ConnectionHeader();
    connectionHeader.addField("foo", "bar");
    connectionHeader.addField("bloop", "");
    ByteBuf encoded = connectionHeader.encode();
    assertEquals(connectionHeader, ConnectionHeader.decode(encoded));
  }
}
