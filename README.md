# Voyage
# Overview
---
采用Java实现的基于netty轻量的高性能分布式RPC服务框架。实现了RPC的基本功能，开发者也可以自定义扩展，简单，易用，高效。
# Features
---
* 服务端支持注解配置
* 客户端实现Filter机制，可以自定义Filter
* 基于netty3.x实现，后期会升级至netty4.x，充分利用netty的高性能
* 数据层提供protostuff和hessian的实现，可以自定义扩展ISerializer接口
* 负载均衡算法采用LRU算法，可以自定义扩展ILoadBlance接口
* 客户端支持服务的同步或异步调用
# Protocol
---
magic + body
# Quick Start
---
Add dependencies to pom.
```
<dependency>
    <groupId>com.lenzhao</groupId>
    <artifactId>voyage-framework</artifactId>
    <version>0.0.1</version>
</dependency>
```
1. 定义接口(样例)
```
git clone https://github.com/zhaoshiling1017/VoyageApi.git
npm install
```
2. 服务端开发(样例)
```
git clone https://github.com/zhaoshiling1017/VoyageServer.git
npm install
cd VoyageServer/target
tar -xzvf voyage-server-1.0-SNAPSHOT-assembly.tar.gz
cd voyage-server-1.0-SNAPSHOT
bin/start.sh
```
3. 客户端开发(样例)
```
git clone https://github.com/zhaoshiling1017/VoyageClient.git
npm install
cd VoyageClient/target
tar -xzvf voyage-client-1.0-SNAPSHOT-assembly.tar.gz
cd voyage-client-1.0-SNAPSHOT
bin/start.sh
```
# Documents
---
暂无
# TODOS
* 增加注册中心(zookeeper)
* 增加服务治理管理
* 引入asm、javassit等java字节码工具
* 完善消息传递协议
# License
---
Voyage is released under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

