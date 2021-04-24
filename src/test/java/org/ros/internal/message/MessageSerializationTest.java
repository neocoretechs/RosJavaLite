package org.ros.internal.message;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.ros.internal.message.definition.MessageDefinitionReflectionProvider;
import org.ros.internal.system.Utility;
import org.ros.message.Duration;
import org.ros.message.Time;




import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class MessageSerializationTest {

  private MessageDefinitionReflectionProvider messageDefinitionReflectionProvider;
  private DefaultMessageFactory defaultMessageFactory;


  private interface Nested extends Message {
    static final java.lang.String _TYPE = "test/Nested";
    static final java.lang.String _DEFINITION = "std_msgs/String data\n";

    std_msgs.String getData();

    void setData(std_msgs.String value);
  }

  private interface NestedList extends Message {
    static final java.lang.String _TYPE = "test/NestedList";
    static final java.lang.String _DEFINITION = "std_msgs/String[] data\n";

    List<std_msgs.String> getData();

    void setData(List<std_msgs.String> value);
  }

  @Before
  public void before() {
    messageDefinitionReflectionProvider = new MessageDefinitionReflectionProvider(Thread.currentThread().getContextClassLoader());
    messageDefinitionReflectionProvider.add(Nested._TYPE, Nested._DEFINITION);
    messageDefinitionReflectionProvider.add(NestedList._TYPE, NestedList._DEFINITION);
    defaultMessageFactory = new DefaultMessageFactory(messageDefinitionReflectionProvider);
   
  }

  private <T extends Message> void checkSerializeAndDeserialize(T message) {
    ByteBuffer buffer = MessageBuffers.dynamicBuffer();
    Utility.serialize(message, buffer);
    dumpBuffer(buffer);
    assertEquals(message, Utility.deserialize(buffer));
  }

  @Test
  public void testBool() {
    std_msgs.Bool message = defaultMessageFactory.newFromType(std_msgs.Bool._TYPE);
    message.setData(true);
    checkSerializeAndDeserialize(message);
    message.setData(false);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testInt8() {
    std_msgs.Int8 message = defaultMessageFactory.newFromType(std_msgs.Int8._TYPE);
    message.setData((byte) 42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testUint8() {
    std_msgs.UInt8 message = defaultMessageFactory.newFromType(std_msgs.UInt8._TYPE);
    message.setData((byte) 42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testInt16() {
    std_msgs.Int16 message = defaultMessageFactory.newFromType(std_msgs.Int16._TYPE);
    message.setData((short) 42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testUInt16() {
    std_msgs.UInt16 message = defaultMessageFactory.newFromType(std_msgs.UInt16._TYPE);
    message.setData((short) 42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testInt32() {
    std_msgs.Int32 message = defaultMessageFactory.newFromType(std_msgs.Int32._TYPE);
    message.setData(42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testUInt32() {
    std_msgs.UInt32 message = defaultMessageFactory.newFromType(std_msgs.UInt32._TYPE);
    message.setData(42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testInt64() {
    std_msgs.Int64 message = defaultMessageFactory.newFromType(std_msgs.Int64._TYPE);
    message.setData(42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testUInt64() {
    std_msgs.UInt64 message = defaultMessageFactory.newFromType(std_msgs.UInt64._TYPE);
    message.setData(42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testFloat32() {
    std_msgs.Float32 message = defaultMessageFactory.newFromType(std_msgs.Float32._TYPE);
    message.setData(42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testFloat64() {
    std_msgs.Float64 message = defaultMessageFactory.newFromType(std_msgs.Float64._TYPE);
    message.setData(42);
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testString() {
    std_msgs.String message = defaultMessageFactory.newFromType(std_msgs.String._TYPE);
    message.setData("Hello, ROS!");
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testTime() {
    std_msgs.Time message = defaultMessageFactory.newFromType(std_msgs.Time._TYPE);
    message.setData(new Time());
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testDuration() {
    std_msgs.Duration message = defaultMessageFactory.newFromType(std_msgs.Duration._TYPE);
    message.setData(new Duration());
    checkSerializeAndDeserialize(message);
  }

  @Test
  public void testNestedMessage() {
    Nested nestedMessage = defaultMessageFactory.newFromType(Nested._TYPE);
    std_msgs.String stringMessage = defaultMessageFactory.newFromType(std_msgs.String._TYPE);
    stringMessage.setData("Hello, ROS!");
    nestedMessage.setData(stringMessage);
    checkSerializeAndDeserialize(nestedMessage);
  }

  @Test
  public void testNestedMessageList() {
    messageDefinitionReflectionProvider.add(NestedList._TYPE, NestedList._DEFINITION);
    NestedList nestedListMessage = defaultMessageFactory.newFromType(NestedList._TYPE);
    std_msgs.String stringMessageA = defaultMessageFactory.newFromType(std_msgs.String._TYPE);
    stringMessageA.setData("Hello, ROS!");
    std_msgs.String stringMessageB = defaultMessageFactory.newFromType(std_msgs.String._TYPE);
    stringMessageB.setData("Hello, ROS!");
    List<std_msgs.String> s = new ArrayList<std_msgs.String>();
    s.add(stringMessageA);
    s.add(stringMessageB);
    nestedListMessage.setData(s);
    checkSerializeAndDeserialize(nestedListMessage);
  }

  /**
   * Regression test for issue 125.
   */
  @Test
  public void testOdometry() {
    nav_msgs.Odometry message = defaultMessageFactory.newFromType(nav_msgs.Odometry._TYPE);
    checkSerializeAndDeserialize(message);
    ByteBuffer buffer = MessageBuffers.dynamicBuffer();
    Utility.serialize(message, buffer);
    dumpBuffer(buffer);
    // Throw away sequence number.
    buffer.getInt();
    while (buffer.hasRemaining()) {
      byte b = buffer.get();
      assertEquals("All serialized bytes should be 0. Check stdout.", 0, b);
    }
  }

  private void dumpBuffer(ByteBuffer buffer) {
    buffer = buffer.duplicate();
    System.out.printf("Dumping %d readable bytes:\n", buffer.limit());
    int i = 0;
    while (buffer.hasRemaining()) {
      byte b = buffer.get();
      System.out.printf("0x%02x ", b);
      if (++i % 8 == 0) {
        System.out.print("   ");
      }
      if (i % 16 == 0) {
        System.out.print("\n");
      }
    }
    System.out.print("\n\n");
  }
}
