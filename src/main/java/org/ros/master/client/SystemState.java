package org.ros.master.client;

import java.util.Collection;

/**
 * The state of the ROS graph as understood by the master.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class SystemState {

	/**
	 * All topics known.
	 */
	private final Collection<TopicSystemState> topics;

	public SystemState(Collection<TopicSystemState> topics) {
		this.topics = topics;
	}

	/**
	 * Get all topics in the system state.
	 * 
	 * @return a collection of topics.
	 */
	public Collection<TopicSystemState> getTopics() {
		return topics;
	}
}
