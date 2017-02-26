package com.lenzhao.framework.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.lenzhao.framework.protocol.RpcResponse;

import com.lenzhao.framework.protocol.RpcRequest;
import com.lenzhao.framework.util.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *rpc客户端动态代理类，一般为了提高性能会使用javassist、asm等工具直接操作Java字节码
 */
public class RpcClientProxy implements InvocationHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(RpcClientProxy.class);
	
	//根据serviceName得到是哪个服务
	private final String serviceName;
	//要调用的接口
	private final Class<?> interfaceClass; 
	//要调用的具体接口实现
	private final String className;
	//负载均衡策略
	private final ILoadBalance loadBlance = new LeastActiveLoadBalance();
	//proxy缓存
	private final static Map<String,Object> proxyMap = new ConcurrentHashMap<String, Object>();
	//服务调用链过滤器
	private final Filter lastFilter;
	
	/**
	 *初始化客户端动态代理类
	* @param interfaceClass 接口类
	* @param serviceName 服务名
	* @param className   实现名
	 */
	private RpcClientProxy(Class<?> interfaceClass,String serviceName,String className) {
		super();
		this.serviceName = serviceName;
		this.interfaceClass = interfaceClass;
		this.className = className;
		GenericFilter genericFilter = new GenericFilter();
		TimeOutFilter timeOutFilter = new TimeOutFilter(genericFilter);
		//TPS控制
		TpsFilter tpsFilter = new TpsFilter(timeOutFilter);
		lastFilter = tpsFilter;
	}
	
	protected static final String generateRequestID() {
		return Sequence.next() + "";
	}
	
	public static <T> T proxy(Class<T> interfaceClass,String serviceName,String className) throws Throwable {
		if (!interfaceClass.isInterface()) {
			throw new IllegalArgumentException(interfaceClass.getName() + " is not an interface");
		}
		String key = interfaceClass.getName() + "_" + serviceName + "_" + className;
		Object proxy = proxyMap.get(key);
		if(null == proxy) {
			proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] { interfaceClass }, new RpcClientProxy(interfaceClass, serviceName, className));
			proxyMap.put(key,proxy);
			logger.info("proxy generated,serviceName: {}, className: {}", serviceName, className);
		}
		return (T)proxy;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		String interfaceName = interfaceClass.getName();
		List<String> parameterTypes = new LinkedList<String>();
		for (Class<?> parameterType : method.getParameterTypes()) {
			parameterTypes.add(parameterType.getName());
		}
		//requestID用来唯一标识一次请求
		String requestID = generateRequestID();
		final RpcRequest request = new RpcRequest(requestID, interfaceName, className, method.getName(), parameterTypes.toArray(new String[0]), args);
		RpcResponse response =  lastFilter.sendRequest(request, loadBlance, serviceName);
		return response.getResult();
	}
	
}
