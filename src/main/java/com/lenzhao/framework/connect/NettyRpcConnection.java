package com.lenzhao.framework.connect;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.lenzhao.framework.common.RpcContext;
import com.lenzhao.framework.protocol.RpcResponse;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.lenzhao.framework.common.Constants;
import com.lenzhao.framework.config.ClientConfig;
import com.lenzhao.framework.exception.RpcException;
import com.lenzhao.framework.protocol.FutureAdapter;
import com.lenzhao.framework.protocol.InvokeFuture;
import com.lenzhao.framework.protocol.RpcRequest;
import com.lenzhao.framework.protocol.RpcRequestEncode;
import com.lenzhao.framework.protocol.RpcResponseDecode;
import com.lenzhao.framework.util.NamedTheadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *netty客户端长连接
 */
public class NettyRpcConnection extends SimpleChannelHandler implements IRpcConnection {

	private static final Logger logger = LoggerFactory.getLogger(NettyRpcConnection.class);
	
	private static final ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(1, new NamedTheadFactory("ConnectionHeart"));

	private volatile long lastConnectedTime = System.currentTimeMillis();
	
	private InetSocketAddress inetAddr;

	private volatile Channel channel;

	//是否已经连接的标示，初始化打开和周期检测时会设置该标示
	private volatile AtomicBoolean connected = new AtomicBoolean(false);
	//客户端配置文件
	private static final ClientConfig clientConfig = ClientConfig.getInstance();
	
	private ClientBootstrap bootstrap = null;
	
	//处理超时事件
//	private Timer timer=null;
	
	private static final ChannelFactory factory = new NioClientSocketChannelFactory(
			Executors.newCachedThreadPool(),
			Executors.newCachedThreadPool(),clientConfig.getMaxThreadCount());
	
	public NettyRpcConnection(String connStr) {
		this.inetAddr = new InetSocketAddress(connStr.split(":")[0],Integer.parseInt(connStr.split(":")[1]));
		initReconnect();
	}

	public NettyRpcConnection(String host, int port) {
		this.inetAddr = new InetSocketAddress(host, port);
		initReconnect();
	}

	public RpcResponse sendRequest(RpcRequest request, boolean async) throws Throwable {
		if (!isConnected() || !channel.isConnected()) {
			throw new RpcException("not connected");
		}
		//如果request已经超时，直接抛弃
		if(System.currentTimeMillis() - request.getAddTime().getTime() > Constants.TIMEOUT_INVOKE_MILLSECOND) {
			logger.error("request timeout exception");
			throw new RpcException("request timeout exception");
		}
		//异步发送请求
		InvokeFuture invokeFuture = new InvokeFuture(channel,request);
		invokeFuture.send();
		if(async) {
			//如果是异步，则封装context
			RpcContext.getContext().setFuture(new FutureAdapter<Object>(invokeFuture));
			return new RpcResponse();
		}
		else {
			//如果是同步，则阻塞调用get方法
			RpcContext.getContext().setFuture(null);
			return invokeFuture.get(Constants.TIMEOUT_INVOKE_MILLSECOND);
		}
	}
	
	/**
	 * 初始化连接
	 */
	public void open() throws Throwable {
		open(true);
	}

	/**
	 * @param connectStatus 心跳检测状态是否正常
	 * @throws Throwable
	 */
	public void open(boolean connectStatus) throws Throwable {
		logger.info("open start,"+getConnStr());
		bootstrap = new ClientBootstrap(factory);
//		timer = new HashedWheelTimer();
		{
			bootstrap.setOption("tcpNoDelay", Boolean.parseBoolean(clientConfig.getTcpNoDelay()));
			bootstrap.setOption("reuseAddress", Boolean.parseBoolean(clientConfig.getReuseAddress()));
			bootstrap.setOption("SO_RCVBUF",1024*128);
			bootstrap.setOption("SO_SNDBUF",1024*128);
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				public ChannelPipeline getPipeline() {
					ChannelPipeline pipeline = Channels.pipeline();
//					int readTimeout = clientConfig.getReadTimeout();
//					if (readTimeout > 0) {
//						pipeline.addLast("timeout", new ReadTimeoutHandler(timer,
//								readTimeout, TimeUnit.MILLISECONDS));
//					}
					pipeline.addLast("encoder", new RpcRequestEncode());
					pipeline.addLast("decoder", new RpcResponseDecode());
					pipeline.addLast("handler", NettyRpcConnection.this);
					return pipeline;
				}
			});
		}
		connected.set(connectStatus);
		logger.info("open finish,"+getConnStr());
	}
	
	public void initReconnect() {
		Runnable connectStatusCheckCommand =  new Runnable() {
			@Override
			public void run() {
				try {
				 if(!isConnected()) {
					 try {
						open(false);
						connect();
						connected.set(true);
					} catch (Throwable e) {
						logger.error("connect open error,conn: {}", getConnStr());
					}
				 }
				 if (isConnected() && isClosed()) {
					 try {
						 connect();
					} catch (Throwable e) {
						logger.error("connect error,conn: {}", getConnStr());
					}
                 }  
				 if(isConnected() && !isClosed()) {
                     lastConnectedTime = System.currentTimeMillis();
                 }
				 if (System.currentTimeMillis() - lastConnectedTime > Constants.TIMEOUT_HEARTBEAT_MILLSECOND) {
                     if (connected.get()) {
                    	 connected.set(false);
                    	 logger.error("connected has loss heartbeat,conn: {}", getConnStr());
                     }
                 }
				}
				catch(Throwable e) {
					logger.error("connectStatusCheckCommand error");
				}
			}
		};
		//1秒后每隔3秒发送一次心跳
		executorService.scheduleAtFixedRate(connectStatusCheckCommand, 1000, 3000, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * 尝试连接
	 */
	public void connect() {
        ChannelFuture future = bootstrap.connect(inetAddr);
        try{
            boolean ret = future.awaitUninterruptibly(Constants.TIMEOUT_CONNECTION_MILLSECOND, TimeUnit.MILLISECONDS);
            if (ret && future.isSuccess()) {
                Channel newChannel = future.getChannel();
                newChannel.setInterestOps(Channel.OP_READ_WRITE);
                try {
                    // 关闭旧的连接
                    Channel oldChannel = NettyRpcConnection.this.channel;
                    if (oldChannel != null) {
                        logger.info("Close old netty channel {} on create new netty channel {}", oldChannel, newChannel);
                        oldChannel.close();
                    }
                } finally {
                    if (!isConnected()) {
                        try {
                            logger.info("Close new netty channel {}, because the client closed.", newChannel);
                            newChannel.close();
                        } finally {
                        	NettyRpcConnection.this.channel = null;
                        }
                    } else {
                    	NettyRpcConnection.this.channel = newChannel;
                    }
                }
            } else if (null != future.getCause()) {
            	logger.error("connect fail", future.getCause());
            	throw new RuntimeException("connect error", future.getCause());
            } else {
            	logger.error("connect fail,connstr: "+this.getConnStr());
            	throw new RuntimeException("connect error");
            }
        }finally{
            if (! isConnected()) {
                future.cancel();
            }
        }
	}

	/**
	 * 客户端接受并处理消息
	 */
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		RpcResponse response = (RpcResponse) e.getMessage();
		InvokeFuture.receive(channel, response);
	}
	

	public void close() throws Throwable {
		connected.set(false);
//		if (null != timer) {
//			timer.stop();
//			timer = null;
//		}
		if (null != channel) {
			channel.close().awaitUninterruptibly();
			channel.getFactory().releaseExternalResources();

			synchronized (channel) {
				channel.notifyAll();
			}
			channel = null;
		}
	}

	public boolean isConnected() {
		return connected.get();
	}

	public boolean isClosed() {
		return (null == channel) || !channel.isConnected() || !channel.isReadable() || !channel.isWritable();
	}

	public String getConnStr() {
		return inetAddr.getHostName()+":"+inetAddr.getPort();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		super.exceptionCaught(ctx, e);
		logger.error("exceptionCaught", e.getCause());
	}

}
