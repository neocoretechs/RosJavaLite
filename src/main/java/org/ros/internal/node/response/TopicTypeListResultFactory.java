package org.ros.internal.node.response;

import java.util.ArrayList;
import java.util.List;

import org.ros.master.client.TopicType;

/**
 * A {@link ResultFactory} to take an object and turn it into a list of
 * {@link TopicType} instances.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class TopicTypeListResultFactory implements
		ResultFactory<List<TopicType>> {

	@Override
	public List<TopicType> newFromValue(Object value) {
		List<TopicType> topics = new ArrayList<TopicType>();

		for (Object pair : (Object[]) value) {
			topics.add(new TopicType((String) ((Object[]) pair)[0],
					(String) ((Object[]) pair)[1]));
		}

		return topics;
	}

}
