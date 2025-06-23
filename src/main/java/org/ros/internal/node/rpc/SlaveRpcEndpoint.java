package org.ros.internal.node.rpc;

import java.util.List;

/**
 * Interface for facilitating communications with a master Rpc Endpoint. Returns Lists of Objects representing
 * Master URI, Pid, Subscriptions, Publications, Bus Stats, and issuing shutdown and publisher update messages.
 * {@link RpcEndpoint} {@link SlaveRpcEndpointImpl}
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 *
 */
public interface SlaveRpcEndpoint extends RpcEndpoint {

	List<Object> getMasterUri(String string);

	List<Object> shutdown(String string, String message);

	List<Object> getPid(String string);

	List<Object> getSubscriptions(String string);

	List<Object> getPublications(String string);

	List<Object> publisherUpdate(String string, String string2, Object[] array);

	List<Object> getBusStats(String callerId);

	List<Object> getBusInfo(String callerId);

	List<Object> paramUpdate(String callerId, String key, Object value);

	List<Object> requestTopic(String callerId, String topic, Object[] protocols);

}
