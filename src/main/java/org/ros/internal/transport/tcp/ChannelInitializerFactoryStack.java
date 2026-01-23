package org.ros.internal.transport.tcp;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 * This class holds the deque of factories that are ChannelInitializers.
 * The factories implement the ChannelInitializer and contain the initChannel method that is fired 
 * during channel initialization to load the handlers into the pipeline.
 * When a channel is created, the factories are activated and their methods inject the ChannelHandlers
 * into the pipeline for the context of the newly created channel.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 *
 */
public class ChannelInitializerFactoryStack {
	private static boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(ChannelInitializerFactoryStack.class);
	
	private LinkedBlockingDeque<ChannelInitializer> queue = new LinkedBlockingDeque<ChannelInitializer>();
	
	public void addFirst(ChannelInitializer ch) {
		if(DEBUG)
			log.info("Adding First ChannelInitializer:"+ch+" queue size="+queue.size());
		queue.addFirst(ch);
	}
	public void addLast(ChannelInitializer ch) {
		if(DEBUG)
			log.info("Adding Last ChannelInitializer:"+ch+" queue size="+queue.size());
		queue.addLast(ch);
	}
	/**
	 * Given the channelHandlerContext of a freshly constructed channel, run the initializers
	 * that will load the handlers into the pipeline of the context such that the channel may start processing.
	 * @param ctx
	 * @throws Exception
	 */
	public void inject(ChannelHandlerContext ctx) throws Exception {
		if(DEBUG)
			log.info("Injecting ChannelHandlerContext:"+ctx+" queue size="+queue.size());
		Iterator<ChannelInitializer> it = queue.iterator();
		while(it.hasNext()) {
			ChannelInitializer ch = it.next();
			if(DEBUG)
				log.info("ChannelInitializer initChannel for ChannelHandlerContext:"+ctx+" ChannelInitializer:"+ch);
			ch.initChannel(ctx);
		}
	}
	
}
