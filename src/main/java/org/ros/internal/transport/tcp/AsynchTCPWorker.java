package org.ros.internal.transport.tcp;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 * This AsynchTCPWorker is spawned for servicing traffic from connected nodes.
 * It functions as a worker for the {@link TcpRosServer} as well as the {@link TcpClient} to handle read traffic.
 * @author jg
 * Copyright (C) NeoCoreTechs 2016
 *
 */
public class AsynchTCPWorker implements Runnable {
	private static final boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(AsynchTCPWorker.class);
	public boolean shouldRun = true;
	private ChannelHandlerContext ctx;
	private Object waitHalt = new Object(); 
	
    public AsynchTCPWorker(ChannelHandlerContext ctx) throws IOException {
    	this.ctx = ctx;
    	if( DEBUG )
    		log.info("AsynchTCPWorker constructed with context:"+ctx);
	}
	
	/**
	 * Client (Slave port) sends data to our master in the following loop
	 */
	@Override
	public void run() {
			try {
				while(shouldRun) {
					Object reso = ctx.read();
					if( DEBUG )
							log.info("ROS AsynchTCPWorker COMPLETED READ for "+ctx+"  Object:"+reso);
					try {
							ctx.pipeline().fireChannelRead(reso);
					} catch (Exception e) {
						if( DEBUG) {
								log.info("Exception out of fireChannelRead",e);
								e.printStackTrace();
						}
						ctx.pipeline().fireExceptionCaught(e);
					}
					ctx.pipeline().fireChannelReadComplete();
				} // shouldRun		
			} catch(Exception se) {
					log.error("AsynchTCPWorker terminating due to ",se);
					try {
						ctx.pipeline().fireExceptionCaught(se);
					} catch (Exception e) {}
			} finally {
				try {
					if( DEBUG )
						log.info("<<<<<<<<<< Datasocket closing >>>>>>>>");
					ctx.setReady(false);
					ctx.close();
				} catch (IOException e) {}
			}
			synchronized(waitHalt) {
				waitHalt.notify();
			}

	}
	
	public void close() {
		synchronized(waitHalt) {
			try {
				shouldRun = false;
				waitHalt.wait();
			} catch (InterruptedException ie) {}
		}
	}

}
