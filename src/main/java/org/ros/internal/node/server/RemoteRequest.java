package org.ros.internal.node.server;

import java.io.Serializable;

public class RemoteRequest implements RemoteRequestInterface, Serializable {
	private static final long serialVersionUID = 6897457026081442050L;
	String nodeName;
	String className;
	String session;
	String method;
	Object[] paramArray;
	
	public RemoteRequest() {}
	public RemoteRequest(String classname, String method, Object ... params) {
		className = classname;
		this.method = method;
		paramArray = params;
	}
	
	public void setNode(String node) {
		this.nodeName = node;
	}
	
	public void setSession(String sess) {
		session = sess;
	}
	
	@Override
	public String getNodeName() {
		return nodeName;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getSession() {
		return session;
	}

	@Override
	public String getMethodName() {
		return method;
	}

	@Override
	public Object[] getParamArray() {
		return paramArray;
	}

	@Override
	public Class<?>[] getParams() {
		int i = 0;
		Class<?>[] params = new Class<?>[paramArray.length];
		for( Object param : paramArray) params[i++] = param.getClass();
		return params;
	}
	
	@Override
	public String toString() {
		String params = "";
		for(Object param: paramArray) params += "param:"+param+" -- "+param.getClass()+" | ";
		return "RemoteRequest Class:"+className+" method:"+method+" params:"+params;
	}

}
