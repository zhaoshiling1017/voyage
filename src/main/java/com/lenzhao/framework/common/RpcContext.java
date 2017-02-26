package com.lenzhao.framework.common;

import java.util.concurrent.Future;

/**
 *上下文对象
 */
public class RpcContext {
	
	private static final ThreadLocal<RpcContext> LOCAL = new ThreadLocal<RpcContext>() {
		@Override
		protected RpcContext initialValue() {
			return new RpcContext();
		}
	};
	
	public static RpcContext getContext() {
	    return LOCAL.get();
	}
	
	private Future<?> future = null;

	public <T> Future<T> getFuture() {
		return (Future<T>)future;
	}

	public void setFuture(Future<?> future) {
		this.future = future;
	}
}
