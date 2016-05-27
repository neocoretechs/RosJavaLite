package org.ros.internal.node.topic;

import org.ros.internal.transport.ConnectionHeader;
import org.ros.master.client.TopicSystemState;
import org.ros.namespace.GraphName;

import java.util.List;

/**
 * Abstract class and Base definition of a {@link TopicSystemState}.
 * Primarily operates on TopicDeclaration supplied in constructor.
 * During handshake, the topic declaration ConnectionHeader and message type are used to set up
 * the class of responses to subscriber which is then stored in the ChannelHandlercontext to filter traffic.
 * Provides master signaling methods.
 * 
 * @author jg
 */
public abstract class DefaultTopicParticipant implements TopicParticipant {

  private final TopicDeclaration topicDeclaration;

  public DefaultTopicParticipant(TopicDeclaration topicDeclaration) {
    this.topicDeclaration = topicDeclaration;
  }

  /**
   * @return the {@link TopicDeclaration} of this {@link TopicParticipant}
   */
  public TopicDeclaration getTopicDeclaration() {
    return topicDeclaration;
  }

  public List<String> getTopicDeclarationAsList() {
    return topicDeclaration.toList();
  }

  @Override
  public GraphName getTopicName() {
    return topicDeclaration.getName();
  }

  @Override
  public String getTopicMessageType() {
    return topicDeclaration.getMessageType();
  }

  /**
   * @return the connection header for the {@link TopicSystemState}
   */
  public ConnectionHeader getTopicDeclarationHeader() {
    return topicDeclaration.toConnectionHeader();
  }

  /**
   * Signal that the {@link TopicSystemState} successfully registered with the master.
   */
  public abstract void signalOnMasterRegistrationSuccess();

  /**
   * Signal that the {@link TopicSystemState} failed to register with the master.
   */
  public abstract void signalOnMasterRegistrationFailure();

  /**
   * Signal that the {@link TopicSystemState} successfully unregistered with the master.
   */
  public abstract void signalOnMasterUnregistrationSuccess();

  /**
   * Signal that the {@link TopicSystemState} failed to unregister with the master.
   */
  public abstract void signalOnMasterUnregistrationFailure();
}
