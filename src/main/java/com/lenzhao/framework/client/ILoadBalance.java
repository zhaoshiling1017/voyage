package com.lenzhao.framework.client;

import com.lenzhao.framework.connect.IRpcConnection;

/**
 *负载均衡
 */
public interface ILoadBalance {
	 /**
	  * 根据服务名得到服务连接字符串，从多个备选连接串中按照负载均衡选择一个连接串
	  * @param serviceName
	  * @return
	  */
	 String getLoadBalance(String serviceName);
	 
	 /**
	  * 服务支持完毕，归还连接并计数
	  * @param serviceName
	  * @param connStr
	  */
	 void finishLoadBalance(String serviceName, String connStr);
	 
	 /**
	  * 根据连接串得到长连接
	  * @param conn
	  * @return
	  */
	 IRpcConnection getConnection(String conn);
	 

	 /**
	  * 得到当前客户端连接的并发量
	  * @return
	  */
	 int getCurTotalCount();
}
