package com.lenzhao.framework.protocol;

import java.io.Serializable;

/**
 *协议响应实体
 */
public class RpcResponse implements Serializable{

	private static final long serialVersionUID = 1L;

	private String requestID;

	private Throwable exception;

	private Object result;

	public RpcResponse() {
		super();
	}

	public RpcResponse(String requestID) {
		super();
		this.requestID = requestID;
	}

	public String getRequestID() {
		return requestID;
	}

	public void setRequestID(String requestID) {
		this.requestID = requestID;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
}
