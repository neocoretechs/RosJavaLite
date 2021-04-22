package org.ros.master.client;

/**
 * A simple collection of information about a topic.
 *
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class TopicType {
	
	/**
	 * Name of the topic.
	 */
	private final String name;
	
	/**
	 * Message type of the topic.
	 */
	private final String messageType;

	public TopicType(String name, String messageType) {
		this.name = name;
		this.messageType = messageType;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the messageType
	 */
	public String getMessageType() {
		return messageType;
	}
}
