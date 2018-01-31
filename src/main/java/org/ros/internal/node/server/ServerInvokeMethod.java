package org.ros.internal.node.server;

import java.lang.reflect.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* The remote call mechanism depends on Java reflection to provide access to methods that can be
* remotely invoked via serializable arguments and method name. By designating the reflected classes at startup
* in the server module, remote calls have access to reflected methods. 
* This class handles reflection of the user requests to call designated methods in the server side classes,
* It starts by populating a table of those methods, and at runtime, creates a method call transport for client,
* and provides for server-side invocation of those methods.
* Option to skip leading arguments for whatever reason is provided.
* @author Groff Copyright (C) NeoCoreTechs 1998-2000, 2015, 2017
*/
public final class ServerInvokeMethod {
	private static final boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(ServerInvokeMethod.class);
    protected int skipArgs;
    int skipArgIndex;
    private Method[] methods;
    private MethodNamesAndParams pkmnap = new MethodNamesAndParams();

    public MethodNamesAndParams getMethodNamesAndParams() { return pkmnap; }
    /**
    * This constructor populates this object with reflected methods from the
    * designated class.  Reflect hierarchy in reverse (to get proper
    * overload) and look for methods
    * @param tclass The class name we are targeting
    * @param tskipArgs > 0 if we want to skip first args.
    */
    public ServerInvokeMethod(String tclass, int tskipArgs) throws ClassNotFoundException {      
                pkmnap.classClass = Class.forName(tclass);
                //pkmnap.classClass = theClassLoader.loadClass(tclass, true);
                pkmnap.className = pkmnap.classClass.getName();
                skipArgs = tskipArgs;
                skipArgIndex = skipArgs;
                Method m[];
                m = pkmnap.classClass.getMethods();
                for(int i = m.length-1; i >= 0 ; i--) {
                        //if( m[i].getName().startsWith("Reflect_") ) {
                                pkmnap.methodNames.add(m[i].getName()/*.substring(8)*/);
                                log.info("Method :"+m[i].getName()/*.substring(8)*/);
                        //}
                }
                // create arrays
                methods = new Method[pkmnap.methodNames.size()];
                pkmnap.methodParams = new Class[pkmnap.methodNames.size()][];
                pkmnap.methodSigs = new String[pkmnap.methodNames.size()];
                pkmnap.returnTypes = new Class[pkmnap.methodNames.size()];
                int methCnt = 0;
                //
                for(int i = m.length-1; i >= 0 ; i--) {
                        //if( m[i].getName().startsWith("Reflect_") ) {
                                pkmnap.methodParams[methCnt] = m[i].getParameterTypes();
                                pkmnap.methodSigs[methCnt] = m[i].toString();
                                pkmnap.returnTypes[methCnt] = m[i].getReturnType();
                                if( pkmnap.returnTypes[methCnt] == void.class ) 
                                	pkmnap.returnTypes[methCnt] = Void.class;
                                //int ind1 = pkmnap.methodSigs[methCnt].indexOf("Reflect_");
                                //pkmnap.methodSigs[methCnt] = pkmnap.methodSigs[methCnt].substring(0,ind1)+pkmnap.methodSigs[methCnt].substring(ind1+8);
                                if( skipArgs > 0) {
                                   try {
                                        int ind1 = pkmnap.methodSigs[methCnt].indexOf("(");
                                        int ind2 = pkmnap.methodSigs[methCnt].indexOf(",",ind1);
                                        ind2 = pkmnap.methodSigs[methCnt].indexOf(",",ind2+1);
                                        ind2 = pkmnap.methodSigs[methCnt].indexOf(",",ind2+1);
                                        pkmnap.methodSigs[methCnt] = pkmnap.methodSigs[methCnt].substring(0,ind1+1)+pkmnap.methodSigs[methCnt].substring(ind2+1);
                                   } catch(StringIndexOutOfBoundsException sioobe) {
                                        log.error("The method "+pkmnap.methodSigs[methCnt]+" contains too few arguments (first "+skipArgIndex+" skipped)");
                                   }
                                }
                                methods[methCnt++] = m[i];
                       // }
                }
       }
    
       /**
    	 * Call invocation for static methods in target class
    	 * @param tmc
    	 * @return
    	 * @throws Exception
       */
       public Object invokeMethod(RemoteRequestInterface tmc) throws Exception {
    		return invokeMethod(tmc, null);
       }
       /**
       * For an incoming RemoteRequestInterface implementor of a concrete composition, verify and invoke the proper
       * method.  We assume there is a table of class names and method params properly populated. Compare the
       * method signatures in the RemoteRequest to the table, if a match, invoke the method on localObject.
       * If skipArgs, a class variable, is not zero, the first number of skipArgs parameters in the method
       * described by RemoteRequest will not be compared against table entry, but the values will be passed into
       * the method upon invocation.
       * @return Object of result of method invocation
       */
       public Object invokeMethod(RemoteRequestInterface tmc, Object localObject) throws Exception {
                //NoSuchMethodException, InvocationTargetException, IllegalAccessException  {               
                String targetMethod = tmc.getMethodName();
                int methodIndex = pkmnap.methodNames.indexOf(targetMethod);
                String whyNotFound = "No such method";
                while( methodIndex != -1 && methodIndex < pkmnap.methodNames.size()) {
                        Class[] params = tmc.getParams();
                        //
                        //
                        if (DEBUG) {
                        	for(int iparm1 = 0; iparm1 < params.length ; iparm1++) {        
                                log.info("Calling param: "+params[iparm1]);
                        	}
                        	for(int iparm2 = skipArgIndex ; iparm2 < pkmnap.methodParams[methodIndex].length; iparm2++) {
                                log.info("Method param: "+pkmnap.methodParams[methodIndex][iparm2]);
                        	}
                        }
                        //
                        //
                        if( params.length == pkmnap.methodParams[methodIndex].length-skipArgIndex ) {
                                boolean found = true;
                                // if skipArgs, don't compare first 2
                                for(int paramIndex = 0 ; paramIndex < params.length; paramIndex++) {
                                        // can we cast it?
                                        if( params[paramIndex] != null && !pkmnap.methodParams[methodIndex][paramIndex+skipArgIndex].isAssignableFrom(params[paramIndex]) ) {
                                                found = false;
                                                whyNotFound = "Parameters do not match";
                                                break;
                                        }
                                }
                                if( found ) {
                                        if( skipArgs > 0) {
                                                Object o1[] = tmc.getParamArray(); 
                                                return methods[methodIndex].invoke( localObject, o1 );
                                        } 
                                        // invoke it for return
                                        return methods[methodIndex].invoke( localObject, tmc.getParamArray() );
                               }
                        } else
                               // tag for later if we find nothing matching
                               whyNotFound = "Wrong number of parameters";
                        methodIndex = pkmnap.methodNames.indexOf(targetMethod,methodIndex+1);
                }
                throw new NoSuchMethodException("Method "+targetMethod+" not found in "+pkmnap.className+" "+whyNotFound);
        }

}

