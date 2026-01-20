# spring-cloud-starter-bus-emqx

用 EMQX (MQTT) 替换 RabbitMQ 作为 Spring Cloud Bus 的消息传输，提供一个开箱即用的 Starter，参考 `spring-cloud-starter-bus-amqp`。

## 功能概述
- 将 `RemoteApplicationEvent` 通过 MQTT Topic 广播到集群中的其他服务。
- 支持应用触发的常见 Bus 事件（如 `RefreshRemoteApplicationEvent`）。
- 自动建立 MQTT 连接，订阅并发布到 `topicPrefix/destination` 路径。
- 与 Spring Boot 3 / Spring Cloud 2023 兼容，使用新式 `AutoConfiguration.imports` 注册自动配置。

## 依赖版本
- Spring Boot: `3.5.x`
- Spring Cloud: `2025.0.x`
- MQTT Client: Eclipse Paho `1.2.x`

## 安装与引入
在业务服务的 `pom.xml` 中引入：
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-emqx</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
确保服务也引入了 Spring Cloud 依赖 BOM（通常在父 `pom` 中已经导入）。

## 配置示例
将原 `spring-cloud-starter-bus-amqp` 的 RabbitMQ 配置替换为 EMQX 的 MQTT 配置：
```yaml
spring:
  application:
    name: demo-service
  cloud:
    bus:
      destination: springCloudBus   # Bus 目的地（类似 exchange/topic 名）
      emqx:
        enabled: true               # 默认开启
        server: tcp://localhost:1883
        client-id: demo-service-bus # 不配置则默认: ${spring.application.name}-bus-${random}
        username: admin             # 如开启认证则填写
        password: public
        topic-prefix: myapp/bus     # 最终发布/订阅的主题为: myapp/bus/springCloudBus
        qos: 1
        clean-session: true
        keep-alive-interval: 30
        connection-timeout: 10
```
> 说明：
> - `server` 支持 `tcp://` 与 `ssl://`，如需 TLS/SSL 需在 EMQX 端配置证书。
> - `topic-prefix` 与 `spring.cloud.bus.destination` 共同决定实际使用的 Topic。
> - `client-id` 默认使用 `${spring.application.name}` + 随机后缀，确保集群内唯一。

## 使用方式
- Actuator 集成：如果应用启用了 `spring-boot-starter-actuator` 且暴露了 `busrefresh` 端点，可以直接触发：
  ```bash
  curl -X POST http://localhost:port/actuator/busrefresh
  ```
  这会在本服务内发布 `RefreshRemoteApplicationEvent`，Starter 会捕获事件并通过 MQTT 广播到其他服务。

- 自定义事件：也可以手动发布任意 `RemoteApplicationEvent` 子类：
  ```java
  @Autowired
  private ApplicationEventPublisher publisher;
  
  // 例如 EnvironmentChangeRemoteApplicationEvent
  publisher.publishEvent(new EnvironmentChangeRemoteApplicationEvent(this, Collections.emptyMap()));
  ```

### 构建并安装 Starter
在仓库根目录执行（将 starter 安装到本地仓库供项目引用）：
```bash
mvn -q -DskipTests install
```

## 与 `spring-cloud-starter-bus-amqp` 的差异
- 传输层从 RabbitMQ (AMQP) 改为 EMQX (MQTT)。
- 使用 MQTT Topic (`topicPrefix/destination`) 替代 Rabbit 的 Exchange/Queue。
- 不依赖 Spring Cloud Stream 的 Rabbit/Kafka Binder；内部直接使用 Eclipse Paho MQTT 客户端桥接事件。

## 设计与实现要点
- 出站桥接：监听本地 `RemoteApplicationEvent`（仅处理 `originService` 属于当前应用的事件），封装为 `BusEnvelope` 并发布到 MQTT。
- 入站桥接：订阅 `topicPrefix/destination/#`，解析负载并反序列化为对应的 `RemoteApplicationEvent`，发布到本地 `ApplicationContext`。
- 自身消息去重：通过 `senderId`（MQTT `clientId`）与线程本地标记避免环路与自发事件重复发布。

## 已知限制
- 高级目的地匹配（例如 `app:**` 模式）未实现，仅支持广播或精确服务名匹配的事件（建议使用广播刷新场景）。
- 未实现 Bus 事件的 Trace/Ack 记录；需要时可扩展。
- 消息有序性/持久化依赖 EMQX 与 MQTT QoS 语义；默认使用 QoS 1。

## 生产建议
- 启用 TLS (`ssl://`) 并在 EMQX 配置证书与访问控制。
- 为不同业务分组选择不同的 `topic-prefix`，避免跨应用串扰。
- 合理规划 `client-id` 与连接数，避免因客户端 ID 冲突导致连接踢出。

## 许可
本项目示例不附带额外许可声明，供学习与参考使用。如需在生产环境使用，请结合自身合规要求与依赖库授权情况。