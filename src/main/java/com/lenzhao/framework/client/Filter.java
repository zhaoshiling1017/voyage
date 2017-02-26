package com.lenzhao.framework.client;

import com.lenzhao.framework.protocol.RpcResponse;
import com.lenzhao.framework.protocol.RpcRequest;

/**
 *服务调用链过滤器,可以自定义过滤器来实现一些AOP功能
 */

public interface Filter {
	
	/**
	 * 服务调用
	 * @param request
	 * @param loadBlance
	 * @param serviceName
	 * @return
	 * @throws Throwable
	 */
	RpcResponse sendRequest(RpcRequest request, ILoadBalance loadBlance, String serviceName) throws Throwable;

}