package com.lenzhao.framework.client;

import com.lenzhao.framework.protocol.RpcRequest;
import com.lenzhao.framework.protocol.RpcResponse;

import com.lenzhao.framework.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *监控服务性能，如果超时则输出监控日志
 */
public class TimeOutFilter implements Filter{

	private static final Logger logger = LoggerFactory.getLogger(GenericFilter.class);
	
	private Filter next;
	
	public TimeOutFilter(Filter next) {
		this.next = next;
	}
	@Override
	public RpcResponse sendRequest(RpcRequest request, ILoadBalance loadBlance,String serviceName) throws Throwable {
		long start = System.currentTimeMillis();
		RpcResponse response = next.sendRequest(request, loadBlance, serviceName);
		long spendTime = System.currentTimeMillis() - start;
		if(spendTime > Constants.TIMEOUT_LOG_MILLSECOND) {
			logger.warn("spend time is bigger than "+ Constants.TIMEOUT_LOG_MILLSECOND +",the serviceName is:"+serviceName);
		}
		return response;
	}

}
