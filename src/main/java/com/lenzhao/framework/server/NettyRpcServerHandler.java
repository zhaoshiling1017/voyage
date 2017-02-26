package com.lenzhao.framework.server;

import java.lang.reflect.Method;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;

import com.lenzhao.framework.protocol.RpcRequest;
import com.lenzhao.framework.protocol.RpcResponse;
import com.lenzhao.framework.util.ReflectionCache;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *RPC具体处理类
 */
public class NettyRpcServerHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(NettyRpcServerHandler.class);

	private final ChannelGroup channelGroups;

	public NettyRpcServerHandler() {
		this.channelGroups = null;
	}

	public NettyRpcServerHandler(ChannelGroup channelGroups) {
		this.channelGroups = channelGroups;
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		if (null != channelGroups) {
			channelGroups.add(e.getChannel());
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		RpcRequest request = (RpcRequest) ctx.getAttachment();
		if (e.getCause() instanceof ReadTimeoutException) {
			// The connection was OK but there was no traffic for last period.
			logger.warn("Disconnecting due to no inbound traffic");
		} else {
			logger.error("", e);
		}
		e.getChannel().close().awaitUninterruptibly();
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (!(msg instanceof RpcRequest)) {
			logger.error("not RpcRequest received!");
			return;
		}
		RpcRequest request = (RpcRequest) msg;
		ctx.setAttachment(request);

		RpcResponse response = new RpcResponse(request.getRequestID());
		try {
			Object result = handle(request);
			response.setResult(result);
		} catch (Throwable t) {
			logger.error("handle rpc request fail! request:"+request, t);
			response.setException(t);
		}
		e.getChannel().write(response);
	}

	private Object handle(RpcRequest request) throws Throwable {
		String className = request.getClassName();
		Object rpcService = ExtensionLoader.getProxy(className);
		if (null == rpcService) {
			throw new NullPointerException("server interface config is null");
		}
		Method method = ReflectionCache.getMethod(request.getInterfaceName(), request.getMethodName(), request.getParameterTypes());
		Object[] parameters = request.getParameters();
		//invoke
		Object result = method.invoke(rpcService, parameters);
		return result;
	}

}
