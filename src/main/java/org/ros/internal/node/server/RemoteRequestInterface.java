package org.ros.internal.node.server;

/**
 * Instead of the XML-RPC calls we are going to box up a lightweight serializable version
 * of this class to call a remotely reflected class and method.
 * @author jg
 *
 */
public interface RemoteRequestInterface {
	public String getNodeName();
	
	public String getClassName();

	public String getSession();

	public String getMethodName();

	public Object[] getParamArray();

	/**
	 * @return An array of Class objects for the parameters of the remote method
	 */
	public Class<?>[] getParams();

}