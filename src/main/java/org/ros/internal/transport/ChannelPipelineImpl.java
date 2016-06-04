package org.ros.internal.transport;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.ros.exception.RosRuntimeException;

/**
 * Implementation of the ChannelPipeline interface to process the requests via the pluggable ChannelHandlers
 * named in the queue. The pipeline is per ChannelHandlerContext, per channel as it represents state in the form of the
 * presence or absence of handlers such as the handshake handler, which disappears after initial handshake.
 * @author jg
 *
 */
public class ChannelPipelineImpl implements ChannelPipeline {
	
	LinkedBlockingDeque<Entry<String, ChannelHandler>> queue = new LinkedBlockingDeque<Entry<String, ChannelHandler>>();
	private ChannelHandlerContext ctx;
	
	public ChannelPipelineImpl(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}
	//public ChannelPipelineImpl() {}
	
	public void setContext(ChannelHandlerContext ctx) { this.ctx = ctx; }
	

	@Override
	public Iterator<Entry<String, ChannelHandler>> iterator() {
		return queue.iterator();
	}

	@Override
	public ChannelPipeline addFirst(String name, ChannelHandler handler) {
		queue.addFirst(new AbstractMap.SimpleEntry<String, ChannelHandler>(name, handler));
		try {
			queue.peekFirst().getValue().handlerAdded(ctx);
		} catch (Exception e) {
			throw new RosRuntimeException(e);
		}
		return this;
	}


	@Override
	public ChannelPipeline addLast(String name, ChannelHandler handler) {
		queue.addLast(new AbstractMap.SimpleEntry<String, ChannelHandler>(name, handler));
		try {
			queue.peekLast().getValue().handlerAdded(ctx);
		} catch (Exception e) {
			throw new RosRuntimeException(e);
		}
		return this;
	}


	/**
	 * Remove and fire event handlerRemoved to handler after removed one
	 */
	@Override
	public ChannelPipeline remove(ChannelHandler handler) {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		boolean found = false;
		while(it.hasNext()) {
			Map.Entry<String,ChannelHandler> me = it.next();
			if( !found && me.getValue().equals(handler) ) {
				it.remove();
				found = true;
				continue;
			}
			if( found ) {
				try {
						me.getValue().handlerRemoved(ctx);
						break;
				} catch (Exception e) {
					throw new RosRuntimeException(e);
				}
			}
		}
		return this;
	}

	@Override
	public ChannelHandler remove(String name) {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		ChannelHandler val = null;
		boolean found = false;
		while(it.hasNext()) {
			Map.Entry<String,ChannelHandler> me = it.next();
			if( !found && me.getKey().equals(name) ) {
				val = me.getValue();
				it.remove();
				found = true;
				continue;
			}
			if( found ) {
				try {
						me.getValue().handlerRemoved(ctx);
						break;
				} catch (Exception e) {
					throw new RosRuntimeException(e);
				}
			}
		}
		return val;
	}


	@Override
	public ChannelHandler removeFirst() {
		Map.Entry<String,ChannelHandler> me = queue.removeFirst();
		if( me == null )  return null;
		if( queue.isEmpty())
			return me.getValue();
		try {
			queue.peekFirst().getValue().handlerRemoved(ctx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return me.getValue();
	}

	@Override
	public ChannelHandler removeLast() {
		Map.Entry<String,ChannelHandler> me = queue.removeLast();
		if( me == null )  return null;
		return me.getValue();
	}

	
	@Override
	public ChannelHandler first() {
		Map.Entry<String,ChannelHandler> me = queue.getFirst();
		if(me == null) return null;
		return me.getValue();
	}


	@Override
	public ChannelHandler last() {
		Map.Entry<String,ChannelHandler> me = queue.getLast();
		if(me == null) return null;
		return me.getValue();
	}


	@Override
	public ChannelHandler get(String name) {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			if( me.getKey().equals(name) ) {
				break;
			}
		}
		if( me == null)
			return null;
		return me.getValue();
	}


	@Override
	public List<String> names() {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		ArrayList<String> ar = new ArrayList<String>();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			ar.add(me.getKey());
		}
		return ar;
	}

	@Override
	public Map<String, ChannelHandler> toMap() {
		ConcurrentHashMap<String, ChannelHandler> map = new ConcurrentHashMap<String, ChannelHandler>();
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			map.put(me.getKey(), me.getValue());
		}
		return map;
	}

	@Override
	public ChannelPipeline fireChannelRegistered() throws Exception {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			me.getValue().handlerAdded(ctx);
		}
		return this;
	}

	@Override
	public ChannelPipeline fireChannelUnregistered() throws Exception {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			me.getValue().handlerRemoved(ctx);
		}
		return this;
	}

	@Override
	public ChannelPipeline fireChannelActive() throws Exception {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			me.getValue().channelActive(ctx);
		}
		return this;
	}

	@Override
	public ChannelPipeline fireChannelInactive() throws Exception {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			me.getValue().channelInactive(ctx);
		}
		return this;
	}
	/**
	 * In this case we traverse the queue backwards and notify from bottom up
	 */
	@Override
	public ChannelPipeline fireExceptionCaught(Throwable cause) throws Exception {
		Iterator<Map.Entry<String,ChannelHandler>> it = queue.descendingIterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			me.getValue().exceptionCaught(ctx, cause);
		}
		return this;
	}

	@Override
	public ChannelPipeline fireUserEventTriggered(Object event) throws Exception {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			me.getValue().userEventTriggered(ctx, event);
		}
		return this;
	}

	@Override
	public ChannelPipeline fireChannelRead(Object msg) throws Exception {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			me.getValue().channelRead(ctx, msg);
		}
		return this;
	}

	@Override
	public ChannelPipeline fireChannelReadComplete() throws Exception {
		Iterator<Map.Entry<String,ChannelHandler>> it = iterator();
		Map.Entry<String,ChannelHandler> me = null;
		while(it.hasNext()) {
			me = it.next();
			me.getValue().channelReadComplete(ctx);
		}
		return this;
	}


}
