package com.lenzhao.framework.protocol;
  
public class MySerializerFactory {
	
	public final static ISerializer getInstance(String mode)
	{
		if("hessian".equals(mode))
		{
			return HessianSerializer.getInstance();
		}
		else if("protobuf".equals(mode))
		{
			return ProtobufSerializer.getInstance();
		}
		else 
		{
			return null;
		}
	}
}
