package org.ros.time;

import org.ros.Topics;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.DefaultNode;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.node.topic.Subscriber;

import rosgraph_msgs.Clock;

/**
 * A {@link TimeProvider} for use when the ROS graph is configured for
 * simulation.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class ClockTopicTimeProvider implements TimeProvider {

  private final Subscriber<rosgraph_msgs.Clock> subscriber;

  private Object mutex;
  private rosgraph_msgs.Clock clock;

  public ClockTopicTimeProvider(DefaultNode defaultNode) {
	try {
		subscriber = defaultNode.newSubscriber(Topics.CLOCK, rosgraph_msgs.Clock._TYPE);
	} catch(Exception e) { throw new RosRuntimeException(e); }
    mutex = new Object();
    subscriber.addMessageListener(new MessageListener<Clock>() {
      @Override
      public void onNewMessage(Clock message) {
        synchronized (mutex) {
          clock = message;
        }
      }
    });
  }

  public Subscriber<rosgraph_msgs.Clock> getSubscriber() {
    return subscriber;
  }

  @Override
  public Time getCurrentTime() {
    assert(clock != null);
    synchronized (mutex) {
      return new Time(clock.getClock());
    }
  }
}
