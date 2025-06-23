package org.ros.internal.node.rpc;

/**
 * Marker interface to indicate an RPC endpoint, The default serialization is Java instead
 * of XML for ROSJavaLite. Relies on config from {@link RpcClientConfigImpl} <p>
 * {@link MasterRpcEndpoint} {@link SlaveRpcEndpoint}
 * @author Jonathan Groff copyright (C) NeoCoreTechs 2015,2021
 */
public interface RpcEndpoint {
	public void setConfig(RpcClientConfigImpl config);
	public void shutDown();
}
