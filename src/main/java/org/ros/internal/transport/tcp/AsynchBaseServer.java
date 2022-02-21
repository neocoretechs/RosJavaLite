package org.ros.internal.transport.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelHandlerContextImpl;

/**
 * Functionally, this class Extends AsynchTCPServer, takes connections and spins the worker thread to handle each
 * incoming connection.<p/>
 * The critical ChannelhandlerContext is created here after the server socket accepts a connection.<p/>
 * After the ChannelHandlerContext is created, an AsynchTcpWorker is constructed using that context.<p/> 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2016,2021
 */
public final class AsynchBaseServer extends AsynchTCPServer {
	private static boolean DEBUG = true;
    private static final Log log = LogFactory.getLog(AsynchBaseServer.class);
	public int WORKBOOTPORT = 0;
	public InetSocketAddress address = null;
	private TcpRosServer tcpserver = null;
	AsynchTCPWorker uworker = null;
	ExecutorService executor;
		
	public AsynchBaseServer(TcpRosServer tcpserver) throws IOException {
		super();
		this.address = tcpserver.getAddress();
		this.tcpserver = tcpserver;
		this.executor = tcpserver.getExecutor();
	}

	public void startServer() throws IOException {
		if( address == null )
			throw new IOException("Server address not defined, can not start Base Server");
		startServer(executor, address);
	}
	
	public void run() {
		while(!shouldStop) {
			try {
				/*Future<Asynchronous*/Socket channel = server.accept();
				if( DEBUG ) {
					log.info("Accept "+channel/*.get()*/);
				}	
				//(/*(AsynchronousSocketChannel)*/channel/*.get()*/).setOption(StandardSocketOptions.SO_RCVBUF, 4096000);
				//(/*(AsynchronousSocketChannel)*/channel/*.get()*/).setOption(StandardSocketOptions.SO_SNDBUF, 4096000);
				//(/*(AsynchronousSocketChannel)*/channel/*.get()*/).setOption(StandardSocketOptions.TCP_NODELAY, true);
				channel.setSendBufferSize(4096000);
				channel.setReceiveBufferSize(4096000);
				//channel.setTcpNoDelay(true);
				ChannelHandlerContext ctx = new ChannelHandlerContextImpl(executor, channel/*.get()*/);
				//if(DEBUG)
				//	log.info("Adding new ChannelHandlerContext to subscribers array, subscribers="+tcpserver.getSubscribers().size()+" "+ctx);
				//tcpserver.getSubscribers().add(ctx);
				// inject the handlers, start handshake
			    // inject calls initChannel on each ChannelInitializer in the factoryStack
				tcpserver.getFactoryStack().inject(ctx);
				// notify handlers that a registration has occured
				//ctx.pipeline().fireChannelRegistered(); 
				// We have to set context as ready AFTER we do the handshake to keep traffic from
				// interfering with handshake
				// notify channel up and ready
				// spin TEMP worker to handle traffic from asynch socket
				// After it gets it the thread terminates and a new handler is inserted to generate outbound traffic
                uworker = new AsynchTCPWorker(ctx);
                // and send it all to executor for running
                ctx.executor().execute(uworker);
				ctx.pipeline().fireChannelActive();
                if( DEBUG ) {
                    	log.info("AsynchBaseServer worker starting for context:"+ctx);
                }   
			} catch(Exception e) {
                    log.error("Asynch Server socket accept exception "+e,e);
            } 
		}
	
	}
	
	public Integer getPort() {
		return WORKBOOTPORT;
	}

	/**
	 * Calls a shutdown of the AsynchTCPWorker, then a shutdown of the AsynchTCPServer superclass.
	 * Invoking TcpRosServer shutdown calls this method before its shutdown is performed.
	 */
	public void shutdown() throws IOException {
		if(uworker != null)
			uworker.shutdown();
		super.shutdown();
	}
	
	public String toString() {
		return "AsynchBaseServer for "+ tcpserver;
	}

}
