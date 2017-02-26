package com.lenzhao.framework.connect;

import com.lenzhao.framework.protocol.RpcRequest;
import com.lenzhao.framework.protocol.RpcResponse;

/**
 *客户端连接
 */
public interface IRpcConnection {

	/**
	 * 发送请求
	 * @param request
	 * @param async 标示是否异步发送请求
	 * @return
	 * @throws Throwable
	 */
	RpcResponse sendRequest(RpcRequest request, boolean async) throws Throwable;
	
	/**
	 * 初始化连接
	 * @throws Throwable
	 */
	void open() throws Throwable; 
	
	/**
	 * 重新连接
	 * @throws Throwable
	 */
	void connect() throws Throwable;

	/**
	 * 关闭连接
	 * @throws Throwable
	 */
	void close() throws Throwable;

	/**
	 * 连接是否已经关闭
	 * @return
	 */
	boolean isClosed();
	
	/**
	 * 心跳是否正常
	 * @return
	 */
	boolean isConnected();
}
