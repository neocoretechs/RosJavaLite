package org.ros.node;

import org.ros.namespace.GraphName;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

/**
 * Encapsulates a {@link Node} with its associated program logic. An instance
 * is created that encapsulates a {@link SlaveServer} within an instance of
 * {@link NodeConfiguration}.
 * <p>
 * {@link NodeMain} is the one required {@link NodeListener} for {@link Node}
 * creation. {@link NodeListener#onStart(ConnectedNode)} should be used to set up your
 * program's {@link Publisher}s, {@link Subscriber}s, etc.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021
 */
public interface NodeMain extends NodeListener {

  /**
   * @return the name of the {@link Node} that will be used if a name was not specified in the {@link Node}'s associated {@link NodeConfiguration}
   */
  public GraphName getDefaultNodeName();
 
}
