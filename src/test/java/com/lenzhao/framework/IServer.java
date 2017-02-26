package com.lenzhao.framework;
  
public interface IServer {
	public String getMsg();
	
	public Message echoMsg(String msg);
	
	public Message echoMsg(int msg);
}
