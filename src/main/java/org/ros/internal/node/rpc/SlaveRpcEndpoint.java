package org.ros.internal.node.rpc;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public interface SlaveRpcEndpoint extends RpcEndpoint {

	List<Object> getMasterUri(String string);

	List<Object> shutdown(String string, String message);

	List<Object> getPid(String string);

	List<Object> getSubscriptions(String string);

	List<Object> getPublications(String string);

	List<Object> paramUpdate(String string, String string2, boolean value);

	List<Object> paramUpdate(String string, String string2, char value);

	List<Object> paramUpdate(String string, String string2, int value);

	List<Object> paramUpdate(String string, String string2, double value);

	List<Object> paramUpdate(String string, String string2, String value);

	List<Object> paramUpdate(String string, String string2, List<?> value);

	List<Object> paramUpdate(String string, String string2, Map<?, ?> value);

	List<Object> publisherUpdate(String string, String string2, Object[] array);

	//List<Object> requestTopic(String string, String string2, Object[][] objects);

	List<Object> getBusStats(String callerId);

	List<Object> getBusInfo(String callerId);

	List<Object> paramUpdate(String callerId, String key, byte value);

	List<Object> paramUpdate(String callerId, String key, short value);

	List<Object> paramUpdate(String callerId, String key, Vector<?> value);

	List<Object> requestTopic(String callerId, String topic, Object[] protocols);

}
