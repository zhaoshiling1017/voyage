package com.lenzhao.framework.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.lenzhao.framework.common.Constants;
import com.lenzhao.framework.config.ServiceConfig;
import com.lenzhao.framework.connect.IRpcConnection;
import com.lenzhao.framework.connect.NettyRpcConnection;

import com.lenzhao.framework.config.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *最少活跃调用数(LRU)，相同活跃数的随机，活跃数指调用前后计数差。
 *使慢的提供者收到更少请求，因为越慢的提供者的调用前后计数差会越大。
 *常见的负载均衡策略有很多，比如：随机，轮循，最少活跃调用等，开发者可以自己扩展实现
 */
public class LeastActiveLoadBalance implements ILoadBalance {

	private static final Logger logger = LoggerFactory.getLogger(LeastActiveLoadBalance.class);
	//当前并发量
	private final AtomicInteger curTotalCount = new AtomicInteger(0);
	
	private final static Random RANDOM_NUM = new Random();
	
	//修改curTotalMap需要加锁
	private final ReadWriteLock lockTotalMap = new ReentrantReadWriteLock();
	
	private final ReadWriteLock lockConnectionMap = new ReentrantReadWriteLock();
	
	//key代表serviceName,value Map中的key代表连接串,value代表该连接的当前并发数
	private final Map<String,Map<String,AtomicInteger>> curTotalMap = new ConcurrentHashMap<String, Map<String,AtomicInteger>>();
	
	//长连接池
	private final Map<String,IRpcConnection> connectionMap = new ConcurrentHashMap<String, IRpcConnection>();
	
	public int getCurTotalCount() {
		return curTotalCount.get();
	}
	
	public IRpcConnection getConnection(String conn) {
		IRpcConnection connection = connectionMap.get(conn);
		if (null == connection) {
			try {
				lockConnectionMap.writeLock().lock();
				connection = connectionMap.get(conn);
				//双重检查，避免重复创建连接
				if(null == connection) {
					try {
						connection = new NettyRpcConnection(conn);
						connection.open();
						connection.connect();
						connectionMap.put(conn, connection);
					} catch(Throwable e) {
						throw new RuntimeException(e);
					}
				}
			} finally {
				lockConnectionMap.writeLock().unlock();
			}
		}
		return connection;
	}
	
	@Override
	public String getLoadBalance(String serviceName) {
		if(null == curTotalMap.get(serviceName)) {
			lockTotalMap.writeLock().lock();
			try {
				if(null == curTotalMap.get(serviceName)) {
					ClientConfig clientConfig = ClientConfig.getInstance();
					ServiceConfig serviceConfig = clientConfig.getService(serviceName);
					Map<String,AtomicInteger> map = new ConcurrentHashMap<String, AtomicInteger>();
					String[] connStrs = serviceConfig.getConnectStr().split(Constants.SERVER_CONNECT_SEPARATOR);
					for(String connStr : connStrs) {
						map.put(connStr,new AtomicInteger(0));
					}
					//service1 => {"127.0.0.1:8080":0, "127.0.0.1:8081":0}
					curTotalMap.put(serviceName, map);
				}
			} finally {
				lockTotalMap.writeLock().unlock();
			}
		}
		//获取最优方案
		String connStr = getMin(curTotalMap.get(serviceName));
		if(null != connStr) {
			curTotalCount.incrementAndGet();
		} else {
			throw new RuntimeException("the service have no alive connection,service: " + serviceName);
		}
		return connStr;
	}

	@Override
	public void finishLoadBalance(String serviceName,String connStr) {
		curTotalCount.decrementAndGet();
		curTotalMap.get(serviceName).get(connStr).decrementAndGet();
	}
	
	/**
	 * 得到存活的并且tps最少的连接
	 * @param map
	 * @return
	 */
	private String getMin(Map<String,AtomicInteger> map) {
		if(map.size() <= 0) {
			return null;
		}
		String result = null;
		TreeMap<Integer,String> sortedMap = new TreeMap<Integer,String>();
		List<String> zeroResults = new ArrayList<String>();
		for(Entry<String,AtomicInteger> entry : map.entrySet()) {
			IRpcConnection connection = connectionMap.get(entry.getKey());
			if(null == connection || (connection != null && connection.isConnected())) {
				int cnt = entry.getValue().get();
				if(cnt == 0) {
					String tmpResult = entry.getKey();
					zeroResults.add(tmpResult);
				}
				else {
					sortedMap.put(entry.getValue().get(), entry.getKey());
				}
			}
		}
		int zsize = zeroResults.size();
		if(zsize > 0) {
			if(zsize == 1) {
				result=zeroResults.get(0);
			} else {
				result = zeroResults.get(RANDOM_NUM.nextInt(zsize));
			}
			return result;
		} else if(sortedMap.size() >= 1) {
			result = sortedMap.firstEntry().getValue();
		} else {
			return null;
		}
		int lessCnt = map.get(result).incrementAndGet();
		int totalCnt = curTotalCount.get();
		//
		if (totalCnt >= Constants.TOTAL_CONCURRENT_ALARAM_NUM) {
			logger.warn("the concurrent connection: {},lessCnt: {},totalCnt: {}", result, lessCnt, totalCnt);
		}
		//127.0.0.1:8080
		return result;
	}
}
