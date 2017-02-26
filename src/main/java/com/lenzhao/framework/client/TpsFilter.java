package com.lenzhao.framework.client;

import com.lenzhao.framework.protocol.RpcResponse;
import com.lenzhao.framework.common.Constants;
import com.lenzhao.framework.exception.RpcException;
import com.lenzhao.framework.protocol.RpcRequest;

/**
 *控制客户端调用服务的最大并发量，超过最大并发量直接抛异常
 */
public class TpsFilter implements Filter {
	
	private Filter next;
	
	public TpsFilter(Filter next) {
		this.next = next;
	}

	@Override
	public RpcResponse sendRequest(RpcRequest request, ILoadBalance loadBlance,String serviceName) throws Throwable {
		int maxConcurrentNum = Constants.CLIENT_CONCURRENT_NUM;
		if(loadBlance.getCurTotalCount() > maxConcurrentNum) {
			throw new RpcException("total invoke is bigger than " + maxConcurrentNum);
		} else {
			return next.sendRequest(request, loadBlance, serviceName);
		}
	}
}
