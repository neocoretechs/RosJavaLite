package org.ros.internal.node.rpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;

import org.ros.exception.RosRuntimeException;

public class RpcClientConfigImpl implements RpcClientConfig {
	private InetSocketAddress serverURL;
	private int connectionTimeout;
	private int replyTimeout;
	public void setServerURL(URL url) {
		this.serverURL = new InetSocketAddress(url.getHost(), url.getPort());		
	}
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;	
	}
	public void setReplyTimeout(int replyTimeout) {
		this.replyTimeout = replyTimeout;	
	}
	@Override
	public InetSocketAddress getSeverURL() {
		return serverURL;
	}
	@Override
	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	@Override
	public int getReplyTimeout() {
		return replyTimeout;
	}
	public void setServerURL(InetSocketAddress uri) {
		serverURL = uri;
		
	}

}
