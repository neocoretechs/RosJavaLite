package org.ros.internal.node.response;

import java.util.Arrays;
import java.util.List;

import org.ros.namespace.GraphName;

public class GraphNameListResultFactory implements
		ResultFactory<List<GraphName>> {

	@Override
	public List<GraphName> newFromValue(Object value) {
		List<Object> values = Arrays.asList(value);
		return (List<GraphName>)values.get(0);
	}

}
