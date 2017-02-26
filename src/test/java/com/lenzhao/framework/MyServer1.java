package com.lenzhao.framework;

import java.util.Date;

import com.lenzhao.framework.annotation.ServiceAnnotation;
import com.lenzhao.framework.server.RpcServerBootstrap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@ServiceAnnotation
public class MyServer1 implements IServer {
	private static final Log log=LogFactory.getLog(MyServer1.class);
	
	public String getMsg()
	{
		log.info("getMsg echo");
		return "Hello";
	}
	
	public static void main(String[] args) {
		RpcServerBootstrap bootstrap = new RpcServerBootstrap();
		bootstrap.start(8080);
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
