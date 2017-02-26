package com.lenzhao.framework.exception;

/**
 *所有的异常都统一封装成这个类
 */
public class RpcException extends RuntimeException {

	public RpcException() {
		super();  
	}

	public RpcException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);  
	}

	public RpcException(String message, Throwable cause) {
		super(message, cause);  
	}

	public RpcException(String message) {
		super(message);  
	}

	public RpcException(Throwable cause) {
		super(cause);  
	}

}
