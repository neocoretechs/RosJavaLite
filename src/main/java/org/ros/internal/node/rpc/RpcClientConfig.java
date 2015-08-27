package org.ros.internal.node.rpc;


import java.net.InetSocketAddress;
import java.net.URL;

public interface RpcClientConfig {
	public void setServerURL(URL url);
	public void setServerURL(InetSocketAddress url);
	public void setConnectionTimeout(int connectionTimeout);
	public void setReplyTimeout(int replyTimeout);
	public InetSocketAddress getSeverURL();
	public int getConnectionTimeout();
	public int getReplyTimeout();
}
