package org.ros.internal.transport.tcp;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadPendingException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.message.MessageBufferPool;
import org.ros.internal.message.MessageBuffers;
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
					final ByteBuffer buf = MessageBuffers.dynamicBuffer();//pool.acquire();
					buf.clear();
					// initiate asynch read
					// If we get a read pending exception, try again
					while(true) {
						try {
							ctx.read(buf, new CompletionHandler<Integer, Void>() {
								@Override
								public void completed(Integer arg0, Void arg1) {
									buf.flip();
									if( DEBUG )
										log.info("ROS AsynchTCPWorker COMPLETED READ for "+ctx+" command received:"+buf);
									try {
										ctx.pipeline().fireChannelRead(buf);
									} catch (Exception e) {
										if( DEBUG) {
											log.info("Exception out of fireChannelRead",e);
											e.printStackTrace();
										}
									}
							
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
								} 	
							});
						} catch(ReadPendingException rpe) { continue; }
						break;
					}// while
				} // shouldRun
				ctx.close();
			} catch(Exception se) {
				if( se instanceof SocketException ) {
					log.error("Received SocketException, connection reset..");
				} else {
					log.error("Remote invocation failure ",se);
				}
			} finally {
				try {
					if( DEBUG )
						log.info("<<<<<<<<<< Datasocket closing >>>>>>>>");
					ctx.close();
					ctx.setReady(false);
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
