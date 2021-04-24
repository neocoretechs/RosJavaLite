package org.ros.internal.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ros.Assert.assertGraphNameEquals;

import org.junit.Test;
//import org.ros.RosCore;
import org.ros.RosTest;
import org.ros.concurrent.Holder;
import org.ros.exception.RemoteException;
import org.ros.exception.RemoteNotFoundException;
import org.ros.internal.node.client.SlaveClient;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.server.master.MasterServer;
import org.ros.internal.transport.ProtocolDescription;
import org.ros.internal.transport.ProtocolNames;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.topic.CountDownPublisherListener;
import org.ros.node.topic.CountDownSubscriberListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the {@link DefaultNode}.
 * 
 * @author jg
 */
public class DefaultNodeTest extends RosTest {

  void checkHostName(String hostName) {
    assertTrue(!hostName.equals("0.0.0.0"));
    assertTrue(!hostName.equals("0:0:0:0:0:0:0:0"));
  }

  private void checkNodeAddress(final String host) throws InterruptedException {
    final Holder<InetSocketAddress> holder = Holder.newEmpty();
    NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(host,"defaultNode",new InetSocketAddress("127.0.0.1",8090), Thread.currentThread().getContextClassLoader() /*rosCore.getUri()*/);
    nodeMainExecutor.execute(new AbstractNodeMain() {
      @Override
      public GraphName getDefaultNodeName() {
        return GraphName.of("node");
      }

      @Override
      public void onStart(ConnectedNode connectedNode) {
        holder.set(((DefaultNode) connectedNode).getAddress());
      }
    }, nodeConfiguration);

    assertTrue(holder.await(1, TimeUnit.SECONDS));
    assertTrue(holder.get().getPort() > 0);
    assertEquals(holder.get().getHostName(), host);
  }

  @Test
  public void testCreatePublic() throws Exception {
    String host = InetAddress.getLocalHost().getCanonicalHostName();
    checkNodeAddress(host);
  }

  @Test
  public void testCreatePublicWithIpv4() throws InterruptedException {
    String host = "1.2.3.4";
    checkNodeAddress(host);
  }

  @Test
  public void testCreatePublicWithIpv6() throws InterruptedException {
    String host = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
    checkNodeAddress(host);
  }

  @Test
  public void testCreatePrivate() throws InterruptedException {
    checkNodeAddress(nodeConfiguration.getTcpRosAdvertiseAddress().getHost());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRegistration() throws InterruptedException {
    final CountDownPublisherListener<std_msgs.String> publisherListener =
        CountDownPublisherListener.newDefault();
    final CountDownSubscriberListener<std_msgs.String> subscriberListener =
        CountDownSubscriberListener.newDefault();

    NodeMain nodeMain = new AbstractNodeMain() {
      @Override
      public GraphName getDefaultNodeName() {
        return GraphName.of("node");
      }

      @Override
      public void onStart(ConnectedNode connectedNode) {
        Publisher<std_msgs.String> publisher =
            connectedNode.newPublisher("foo", std_msgs.String._TYPE);
        publisher.addListener(publisherListener);
        Subscriber<std_msgs.String> subscriber =
            connectedNode.newSubscriber("foo", std_msgs.String._TYPE);
        subscriber.addSubscriberListener(subscriberListener);
      }
    };

    nodeMainExecutor.execute(nodeMain, nodeConfiguration);

    assertTrue(publisherListener.awaitMasterRegistrationSuccess(1, TimeUnit.SECONDS));
    assertTrue(subscriberListener.awaitMasterRegistrationSuccess(1, TimeUnit.SECONDS));

    // There are now two registered publishers /rosout and /foo.
    List<Object> systemState = rosCore.getMasterServer().getSystemState();
    assertEquals(2, ((List<Object>) systemState.get(MasterServer.SYSTEM_STATE_PUBLISHERS)).size());
    assertEquals(1, ((List<Object>) systemState.get(MasterServer.SYSTEM_STATE_SUBSCRIBERS)).size());

    nodeMainExecutor.shutdownNodeMain(nodeMain);

    assertTrue(publisherListener.awaitShutdown(1, TimeUnit.SECONDS));
    assertTrue(subscriberListener.awaitShutdown(1, TimeUnit.SECONDS));

    systemState = rosCore.getMasterServer().getSystemState();
    assertEquals(0, ((List<Object>) systemState.get(MasterServer.SYSTEM_STATE_PUBLISHERS)).size());
    assertEquals(0, ((List<Object>) systemState.get(MasterServer.SYSTEM_STATE_SUBSCRIBERS)).size());
  }

  @Test
  public void testResolveName() throws InterruptedException {
    final Holder<ConnectedNode> holder = Holder.newEmpty();
    nodeConfiguration.setParentResolver(NameResolver.newFromNamespace("/ns1"));
    nodeMainExecutor.execute(new AbstractNodeMain() {
      @Override
      public GraphName getDefaultNodeName() {
        return GraphName.of("test_resolver");
      }

      @Override
      public void onStart(ConnectedNode connectedNode) {
        holder.set(connectedNode);
      }
    }, nodeConfiguration);

    assertTrue(holder.await(1, TimeUnit.SECONDS));
    ConnectedNode connectedNode = holder.get();

    assertGraphNameEquals("/foo", connectedNode.resolveName("/foo"));
    assertGraphNameEquals("/ns1/foo", connectedNode.resolveName("foo"));
    assertGraphNameEquals("/ns1/test_resolver/foo", connectedNode.resolveName("~foo"));

    Publisher<std_msgs.Int64> pub = connectedNode.newPublisher("pub", std_msgs.Int64._TYPE);
    assertGraphNameEquals("/ns1/pub", pub.getTopicName());
    pub = connectedNode.newPublisher("/pub", std_msgs.Int64._TYPE);
    assertGraphNameEquals("/pub", pub.getTopicName());
    pub = connectedNode.newPublisher("~pub", std_msgs.Int64._TYPE);
    assertGraphNameEquals("/ns1/test_resolver/pub", pub.getTopicName());

    Subscriber<std_msgs.Int64> sub = connectedNode.newSubscriber("sub", std_msgs.Int64._TYPE);
    assertGraphNameEquals("/ns1/sub", sub.getTopicName());
    sub = connectedNode.newSubscriber("/sub", std_msgs.Int64._TYPE);
    assertGraphNameEquals("/sub", sub.getTopicName());
    sub = connectedNode.newSubscriber("~sub", std_msgs.Int64._TYPE);
    assertGraphNameEquals("/ns1/test_resolver/sub", sub.getTopicName());
  }

  @Test
  public void testPublicAddresses() throws InterruptedException, IOException, RemoteException, RemoteNotFoundException {
    //RosCore rosCore = RosCore.newPublic(8090);
    //rosCore.start();
    //assertTrue(rosCore.awaitStart(1, TimeUnit.SECONDS));

    InetSocketAddress masterUri = new InetSocketAddress("172.16.0.101",8090);//rosCore.getUri();
    //checkHostName(masterUri.getHost());

    final Holder<ConnectedNode> holder = Holder.newEmpty();
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(masterUri.getHostName(), "defaultNode", masterUri, Thread.currentThread().getContextClassLoader());
    nodeMainExecutor.execute(new AbstractNodeMain() {
      @Override
      public GraphName getDefaultNodeName() {
        return GraphName.of("test_addresses");
      }

      @Override
      public void onStart(ConnectedNode connectedNode) {
        holder.set(connectedNode);
      };
    }, nodeConfiguration);

    assertTrue(holder.await(1, TimeUnit.SECONDS));

    ConnectedNode connectedNode = holder.get();
    InetSocketAddress nodeUri = connectedNode.getUri();
    //assertTrue(nodeUri..getPort() > 0);
    //checkHostName(nodeUri.getHost());

    CountDownPublisherListener<std_msgs.Int64> publisherListener =
        CountDownPublisherListener.newDefault();
    Publisher<std_msgs.Int64> publisher =
        connectedNode.newPublisher("test_addresses_pub", std_msgs.Int64._TYPE);
    publisher.addListener(publisherListener);
    assertTrue(publisherListener.awaitMasterRegistrationSuccess(1, TimeUnit.SECONDS));

    // Check the TCPROS server address via the XML-RPC API.
    SlaveClient slaveClient = null;
	slaveClient = new SlaveClient(GraphName.of("test_addresses"), nodeUri);
    Response<ProtocolDescription> response =
        slaveClient.requestTopic(GraphName.of("test_addresses_pub"), ProtocolNames.TCP);
    // If null is returned there are no publishers for this topic
    if( response == null ) {
    	ProtocolDescription result = response.getResult();
    	InetSocketAddress tcpRosAddress = result.getAdverstiseAddress().toInetSocketAddress();
    	checkHostName(tcpRosAddress.getHostName());
    } else {
    	System.out.println("There are no publishers for topic "+GraphName.of("test_addresses_pub"));
    }
  }
}
