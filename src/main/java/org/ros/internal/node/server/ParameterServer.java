package org.ros.internal.node.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.client.SlaveClient;
import org.ros.namespace.GraphName;

/**
 * A ROS parameter server.
 * 
 * @author Jonathan Groff (C) NeoCoreTechs 2021
 */
public class ParameterServer extends RpcServer {
  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(ParameterServer.class);

  private final Map<String, Object> tree;
  private final Map<GraphName, List<NodeIdentifier>> subscribers;
  private final GraphName masterName;
  private ServerInvokeMethod invokableMethods;

  public ParameterServer(BindAddress binder, AdvertiseAddress advertiseAddress) throws IOException {
	super(binder, advertiseAddress); 
    tree = new ConcurrentHashMap<String, Object>();
    subscribers = new ConcurrentHashMap<GraphName, List<NodeIdentifier>>();
    masterName = GraphName.of("/master");
    try {
		invokableMethods = new ServerInvokeMethod(this.getClass().getName(), 0);
	} catch (ClassNotFoundException e) {
		throw new RosRuntimeException(e);
	}
  }
  @ServerMethod
  public void subscribe(GraphName name, NodeIdentifier nodeIdentifier) {
	List<NodeIdentifier> subs = subscribers.get(name);
	if( subs == null ) {
		subs = new ArrayList<NodeIdentifier>();
		subscribers.put(name, subs);
	}
	subs.add(nodeIdentifier);
  }

  private Stack<String> getGraphNameParts(GraphName name) {
    Stack<String> parts = new Stack<String>();
    GraphName tip = name;
    while (!tip.isRoot()) {
      parts.add(tip.getBasename().toString());
      tip = tip.getParent();
    }
    //if( DEBUG )
    //	log.info("Returning graph name parts with "+parts.size()+" elements");
    return parts;
  }
  
  @ServerMethod
  @SuppressWarnings("unchecked")
  public Object get(GraphName name) {
    assert(name.isGlobal());
    Stack<String> parts = getGraphNameParts(name);
    Object possibleSubtree = tree;
    while (!parts.empty() && possibleSubtree != null) {
      if (!(possibleSubtree instanceof Map)) {
        return null;
      }
      possibleSubtree = ((Map<String, Object>) possibleSubtree).get(parts.pop());
    }
    return possibleSubtree;
  }

  @SuppressWarnings("unchecked")
  private void setValue(GraphName name, Object value) {
    assert(name.isGlobal());
    Stack<String> parts = getGraphNameParts(name);
    Map<String, Object> subtree = tree;
    while (!parts.empty()) {
      String part = parts.pop();
      //if( DEBUG )
    //	  log.info("setValue subtree part "+part);
      if (parts.empty()) {
        subtree.put(part, value);
      } else if (subtree.containsKey(part) && subtree.get(part) instanceof Map) {
        subtree = (Map<String, Object>) subtree.get(part);
      } else {
        Map<String, Object> newSubtree = new HashMap<String, Object>();
        subtree.put(part, newSubtree);
        subtree = newSubtree;
      }
    }
  }

  private interface Updater {
    void update(SlaveClient client);
  }

  private <T> void update(GraphName name, T value, Updater updater) {
    setValue(name, value);
    synchronized (subscribers) {
     if( subscribers.get(name) != null ) {
      for (NodeIdentifier nodeIdentifier : subscribers.get(name)) {
        try {
          //if( DEBUG )
        	//	log.info("Constructing SlaveClient for update master:"+masterName+" addr:"+nodeIdentifier.getUri());
          SlaveClient client = new SlaveClient(masterName, nodeIdentifier.getUri());
          //if( DEBUG )
        //		log.info("Calling update master:"+masterName+" addr:"+nodeIdentifier.getUri()+" using "+client);
          updater.update(client);
       	  //if( DEBUG )
      	//	log.info("SUCCESS update master:"+masterName+" addr:"+nodeIdentifier.getUri()+" using "+client);
        } catch (Exception e) {
          log.error(e);
        }
      }
     }
    }
  }
  
  @ServerMethod
  public void set(GraphName name, Object value) {
	  final GraphName fname = name;
	  final Object fvalue = value;
    update(fname, fvalue, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(fname, fvalue);
      }
    });
  }

  @ServerMethod
  @SuppressWarnings("unchecked")
  public void delete(GraphName name) {
    assert(name.isGlobal());
    Stack<String> parts = getGraphNameParts(name);
    Map<String, Object> subtree = tree;
    while (!parts.empty() && subtree.containsKey(parts.peek())) {
      String part = parts.pop();
      if (parts.empty()) {
        subtree.remove(part);
      } else {
        subtree = (Map<String, Object>) subtree.get(part);
      }
    }
  }
  
  @ServerMethod
  public Object search(GraphName name) {
    throw new UnsupportedOperationException();
  }
  
  @ServerMethod
  @SuppressWarnings("unchecked")
  public boolean has(GraphName name) {
    assert(name.isGlobal());
    Stack<String> parts = getGraphNameParts(name);
    Map<String, Object> subtree = tree;
    while (!parts.empty() && subtree.containsKey(parts.peek())) {
      String part = parts.pop();
      if (!parts.empty()) {
        subtree = (Map<String, Object>) subtree.get(part);
      }
    }
    return parts.empty();
  }

  @SuppressWarnings("unchecked")
  private List<GraphName> getSubtreeNames(GraphName parent, Map<String, Object> subtree) {
	List<GraphName> names = new ArrayList<GraphName>();
    for (String name : subtree.keySet()) {
      Object possibleSubtree = subtree.get(name);
      if (possibleSubtree instanceof Map) {
        names.addAll(getSubtreeNames(parent.join(GraphName.of(name)),(Map<String, Object>) possibleSubtree));
      } else {
        names.add(parent.join(GraphName.of(name)));
      }
    }
    return names;
  }

  @ServerMethod
  public List<GraphName> getNames() {
    return getSubtreeNames(GraphName.root(), tree);
  }
  
  @ServerMethod
  @Override
  public synchronized Object invokeMethod(RemoteRequestInterface rri) throws Exception {
		return invokableMethods.invokeMethod(rri, this);
  }
}
