package com.lenzhao.framework.client;

import com.lenzhao.framework.config.ClientConfig;
import com.lenzhao.framework.config.ServiceConfig;
import com.lenzhao.framework.connect.IRpcConnection;
import com.lenzhao.framework.exception.RpcException;
import com.lenzhao.framework.protocol.RpcRequest;
import com.lenzhao.framework.protocol.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *最终执行的Filter，得到socket长连接并发送请求
 */
public class GenericFilter implements Filter {
	
	private static final Logger logger = LoggerFactory.getLogger(GenericFilter.class);
	
	@Override
	public RpcResponse sendRequest(RpcRequest request, ILoadBalance loadBlance, String serviceName) throws Throwable {
		IRpcConnection connection = null;
		RpcResponse response = null;
		String connStr = loadBlance.getLoadBalance(serviceName);
		ClientConfig clientConfig = ClientConfig.getInstance();
		ServiceConfig serviceConfig = clientConfig.getService(serviceName);
		try {
			connection = loadBlance.getConnection(connStr);
			if(connection.isConnected() && connection.isClosed()) {
				connection.connect();
			}
			if(connection.isConnected() && !connection.isClosed()) {
				response = connection.sendRequest(request, serviceConfig.getAsync());
			} else {
				throw new RpcException("send rpc request fail");
			}
			return response;
		} catch(RpcException e) {
			throw e;
		} catch (Throwable t) {
			logger.warn("send rpc request fail! request: " + request, t);
			throw new RpcException(t);
		} finally {
			loadBlance.finishLoadBalance(serviceName, connStr);
		}
	}
	
}
