package org.ros.internal.node.response;

import java.util.Arrays;
import java.util.List;

import org.ros.namespace.GraphName;
/**
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 *
 */
public class GraphNameListResultFactory implements
		ResultFactory<List<GraphName>> {

	@Override
	public List<GraphName> newFromValue(Object value) {
		List<Object> values = Arrays.asList(value);
		return (List<GraphName>)values.get(0);
	}

}
