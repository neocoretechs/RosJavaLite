package org.ros.internal.node.rpc;

import java.util.List;

/**
 * Marker interface to indicate an RPC endpoint, The default serialization is Java instead
 * of XML for ROSJavaLite
 * @author jg
 *
 */
public interface RpcEndpoint {
	public void setConfig(RpcClientConfigImpl config);


}
