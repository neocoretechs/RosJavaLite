package org.ros.internal.transport.tcp;

import java.io.IOException;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.ros.internal.message.MessageBuffers;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 * This AsynchTCPWorker is spawned for servicing traffic from connected nodes.
 * It functions as a worker for the {@link TcpRosServer} as well as the {@link TcpClient} to handle read traffic.
 * @author jg
 * Copyright (C) NeoCoreTechs 2016
 *
 */
public class AsynchTCPWorker implements Runnable {
	private static final boolean DEBUG = true;
	private static final Log log = LogFactory.getLog(AsynchTCPWorker.class);
	public boolean shouldRun = true;
	private ChannelHandlerContext ctx;
	private Object waitHalt = new Object(); 
	//private MessageBufferPool pool = new MessageBufferPool();
	
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
		//final Object waitFinish = ctx.getChannelCompletionMutex();
			try {
				while(shouldRun) {
					//final ByteBuffer buf = MessageBuffers.dynamicBuffer();//pool.acquire();
					// initiate asynch read
					// If we get a read pending exception, try again
				   	//final ByteBuffer buf = MessageBuffers.dynamicBuffer();//pool.acquire();
					//buf.clear();
					//final CountDownLatch cdl = new CountDownLatch(1);
					//int res = ctx.read(buf);
					// seems like a -1 is generated when channel breaks, so stop
					// this worker on that case
					//if( res == -1) {
					//	shouldRun = false;
					//	if( DEBUG )
					//		log.info("ROS AsynchTCPWorker CHANNEL BREAK, TERMINATING for "+ctx);
					//} else {
					//	buf.flip();
					//	Object reso = Utility.deserialize(buf);
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
					//}
					/*
					ctx.read(buf, new CompletionHandler<Integer, Void>() {
								@Override
								public void completed(Integer arg0, Void arg1) {
									buf.flip();
									Object res = Utility.deserialize(buf);
									//if( res == null ) {
									//	cdl.countDown();
									//	return;
									//}
										
									if( DEBUG )
										log.info("ROS AsynchTCPWorker COMPLETED READ for "+ctx+" buffer:"+buf+" Object:"+res+" Result:"+arg0+","+arg1);
									try {
										ctx.pipeline().fireChannelRead(res);
									} catch (Exception e) {
										if( DEBUG) {
											log.info("Exception out of fireChannelRead",e);
											e.printStackTrace();
										}
									}
									cdl.countDown();
							
								}
								@Override
								public void failed(Throwable arg0, Void arg1) {
									if( DEBUG ){
										log.info("AsynchTcpWorker read op failed:",arg0);
										arg0.printStackTrace();
									}
									try {
										ctx.pipeline().fireExceptionCaught(arg0);
									} catch (Exception e) {
										e.printStackTrace();
									}
									if( arg0 instanceof ClosedChannelException ) {
										shouldRun = false;
									}
									cdl.countDown();
								} 	
					});
					*/
					//cdl.await(); // readpendingexception if we overlap operations
				} // shouldRun
				
			} catch(Exception se) {
					log.error("AsynchTCPWorker terminating due to ",se);
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
