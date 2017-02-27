package com.lenzhao.framework;

import java.util.Date;

import com.lenzhao.framework.annotation.ServiceAnnotation;
import com.lenzhao.framework.server.RpcServerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceAnnotation
public class MyServer2 implements IServer {

	private static final Logger logger = LoggerFactory.getLogger(MyServer2.class);
	
	public String getMsg()
	{
		logger.info("getMsg echo");
		return "Hello";
	}
	
	public static void main(String[] args) {
		RpcServerBootstrap bootstrap = new RpcServerBootstrap();
		bootstrap.start(9090);
	}

	@Override
	public Message echoMsg(String msg) {
		Message result=new Message();
		result.setMsg(msg);
		result.setData(new Date());
		return result;
	}

	@Override
	public Message echoMsg(int msg) {
		Message result=new Message();
		result.setMsg("int:"+msg);
		result.setData(new Date());
		return result;
	}
}
