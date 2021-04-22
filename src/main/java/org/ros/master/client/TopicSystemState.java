package org.ros.master.client;

import java.util.Set;

/**
 * Information about a topic.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class TopicSystemState {

	/**
	 * Name of the topic.
	 */
	private final String topicName;

	/**
	 * Node names of all publishers.
	 */
	private final Set<String> publishers;

	/**
	 * Node names of all subscribers.
	 */
	private final Set<String> subscribers;

	public TopicSystemState(String topicName, Set<String> publishers,
			Set<String> subscribers) {
		this.topicName = topicName;
		this.publishers = publishers;
		this.subscribers = subscribers;
	}

	/**
	 * @return the topicName
	 */
	public String getTopicName() {
		return topicName;
	}

	/**
	 * Get the set of all nodes that publish the topic.
	 * 
	 * @return the set of node names
	 */
	public Set<String> getPublishers() {
		return publishers;
	}

	/**
	 * Get the set of all nodes that subscribe to the topic.
	 * 
	 * @return the set of node names
	 */
	public Set<String> getSubscribers() {
		return subscribers;
	}
}
