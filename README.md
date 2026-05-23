# 🍔 外卖拼单系统 (Food Pooling System)

> 基于 Spring Cloud 微服务架构的外卖拼单平台，支持多人拼单、智能成团、费用分摊、订单追踪等功能。

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-blue.svg)](https://spring.io/projects/spring-cloud)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

---

## 📑 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [项目结构](#项目结构)
- [环境依赖](#环境依赖)
- [本地安装与运行](#本地安装与运行)
- [API 接口说明](#api-接口说明)
- [拼单业务流程](#拼单业务流程)
- [费用分摊算法](#费用分摊算法)
- [部署指南](#部署指南)
- [贡献指南](#贡献指南)
- [测试报告](#测试报告)
- [许可证](#许可证)

---

## 项目简介

**外卖拼单系统**是一个面向多人协作点餐场景的微服务平台。用户可以创建拼单、邀请好友加入、共同下单，系统自动完成成团判定、费用分摊和订单生成。解决了同事、同学在同一商家点外卖时各自下单的痛点，通过拼单降低人均配送费用。

**使用场景：**
- 办公室同事一起点午餐，分摊配送费
- 学生宿舍集体点外卖，凑满减优惠
- 朋友聚会拼单，统一配送省事省心

---

## 核心功能

| 功能模块 | 说明 |
|---------|------|
| **用户管理** | 手机号注册/登录（JWT 鉴权）、个人信息管理、收货地址 CRUD |
| **商家浏览** | 分类筛选、商家搜索、菜品浏览、购物车管理 |
| **拼单管理** | 创建拼单、分享邀请码、加入/退出拼单、成团条件自定义 |
| **智能成团** | 支持按人数/金额/时间三维度组合条件，满足任一即自动成团 |
| **订单管理** | 成团后自动生成订单，状态跟踪（待接单→配送中→已完成） |
| **费用分摊** | 菜品各自承担 + 配送费/包装费按人頭均摊 + 优惠券按比例分摊 |
| **支付结算** | 成团后对参与者自动扣款，解散/取消时自动退款 |
| **超时处理** | 定时扫描超时拼单，自动解散并退款 |

---

## 技术栈

### 后端

| 组件 | 技术选型 | 版本 |
|------|---------|------|
| 开发语言 | Java | 17 |
| 基础框架 | Spring Boot | 3.2.5 |
| 微服务框架 | Spring Cloud | 2023.0.1 |
| 微服务治理 | Spring Cloud Alibaba | 2023.0.1.0 |
| 注册中心 & 配置中心 | Nacos | 2.3.0 |
| API 网关 | Spring Cloud Gateway | — |
| 服务调用 | OpenFeign + Sentinel | — |
| 消息队列 | Apache RocketMQ | 5.1.3 |
| ORM 框架 | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.2 |
| 缓存 & 分布式锁 | Redis + Redisson | 7.x / 3.27.2 |
| JWT 认证 | jjwt | 0.12.5 |
| 工具库 | Hutool | 5.8.26 |
| Lombok | Lombok | 1.18.32 |

### 前端

| 组件 | 技术选型 |
|------|---------|
| 技术栈 | 原生 HTML5 + CSS3 + JavaScript (SPA) |
| UI 风格 | 美团/饿了么风格移动端界面 |

### 基础设施

| 组件 | 说明 |
|------|------|
| Docker Compose | 一键启动 Redis / Nacos / RocketMQ |
| MySQL | 四个独立数据库 (user_db, pool_db, order_db, pay_db) |

---

## 系统架构

### 微服务架构图

```
                    ┌─────────────────────────────────────────┐
                    │           Nginx / CDN                   │
                    └──────────────────┬──────────────────────┘
                                       │
                    ┌──────────────────┴──────────────────────┐
                    │          API Gateway (Gateway)           │
                    │      统一入口 / JWT鉴权 / 路由 / 限流      │
                    └────┬──────────┬──────────┬──────────────┘
                         │          │          │
          ┌──────────────┼──────────┼──────────┼──────────────┐
          │              │          │          │              │
          ▼              ▼          ▼          ▼              ▼
   ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
   │ user-service│ │pool-service│ │order-service│ │payment-svc │
   │            │ │            │ │            │ │            │
   │ ·注册/登录  │ │ ·创建/加入  │ │ ·订单生成   │ │ ·预收/扣款  │
   │ ·地址管理   │ │ ·成团判定  │ │ ·状态追踪   │ │ ·费用分摊   │
   │ ·用户信息   │ │ ·超时解散  │ │ ·商家对接   │ │ ·退款处理   │
   └──────┬─────┘ └──────┬─────┘ └──────┬─────┘ └──────┬─────┘
          │              │              │              │
          ▼              ▼              ▼              ▼
   ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
   │   MySQL    │ │   MySQL    │ │   MySQL    │ │   MySQL    │
   │  user_db   │ │  pool_db   │ │  order_db  │ │   pay_db   │
   └────────────┘ └────────────┘ └────────────┘ └────────────┘

   ┌─────────────────────────────────────────────────────────┐
   │   Nacos 集群 | RocketMQ 集群 | Redis (Redisson)          │
   └─────────────────────────────────────────────────────────┘
```

### 服务间调用关系

```
同步调用 (OpenFeign)：
  pool-service  ──→ user-service    (身份校验、获取地址)
  pool-service  ──→ payment-service (成团时发起扣款)
  order-service ──→ user-service    (获取配送地址)

异步事件 (RocketMQ)：
  pool-service  ──→ "pool.formed"     ──→ order-service  (生成订单)
                 ──→ "pool.formed"     ──→ payment-svc    (批量扣款)
                 ──→ "pool.dissolved"  ──→ payment-svc    (批量退款)
  order-service ──→ "order.completed"  ──→ payment-svc    (资金结算)
```

### 事件流

| 事件 | 生产者 | 消费者 | 说明 |
|------|--------|--------|------|
| `pool.formed` | pool-service | order-service, payment-service | 拼单成团，触发订单生成和扣款 |
| `pool.dissolved` | pool-service | payment-service | 拼单解散，触发退款 |
| `order.completed` | order-service | payment-service | 订单完成，触发资金结算 |
| `payment.success` | payment-service | pool-service, order-service | 支付成功，更新状态 |
| `payment.refund` | payment-service | order-service | 退款完成，通知订单服务 |

---

## 项目结构

```
food-pooling-system/
├── food-common/                    # 公共模块
│   └── src/main/java/com/food/common/
│       ├── constants/              # 业务常量定义
│       ├── dto/                    # 通用 DTO (SettlementDTO, UserInfoDTO)
│       ├── event/                  # 事件模型 (成团/解散/订单取消事件)
│       ├── exception/              # 自定义业务异常
│       ├── result/                 # 统一响应体 (Result<T>)
│       └── utils/                  # 工具类 (IdGenerator, JwtUtil)
│
├── food-gateway/                   # API 网关模块
│   └── src/main/java/com/food/gateway/
│       ├── GatewayApplication.java # 网关启动类
│       └── filter/                 # JWT 鉴权过滤器 + CORS 配置
│
├── user-service/                   # 用户服务 (端口 8081)
│   ├── controller/                 # UserController, UserAddressController
│   ├── service/                    # UserService, UserAddressService
│   ├── mapper/                     # MyBatis-Plus Mapper 接口
│   ├── model/entity/               # UserEntity, UserAddressEntity
│   └── dto/                        # LoginRequest, RegisterRequest, UserVO, AddressRequest
│
├── pool-service/                   # 拼单服务 (端口 8082)
│   ├── controller/                 # PoolController, MerchantController
│   ├── service/                    # PoolService (核心业务逻辑)
│   ├── mapper/                     # PoolMapper, ParticipantMapper, ItemMapper, MerchantMapper
│   ├── model/                      # Entity + DTO
│   └── resources/sql/              # 商家/菜品初始化 SQL
│
├── order-service/                  # 订单服务 (端口 8083)
│   ├── controller/                 # OrderController
│   ├── service/                    # OrderService
│   ├── mapper/                     # OrderMapper, OrderItemMapper, SettlementMapper
│   └── model/entity/               # OrderEntity, OrderItemEntity, SettlementEntity
│
├── payment-service/                # 支付服务 (端口 8084)
│   ├── controller/                 # PaymentController
│   ├── service/                    # PaymentService (扣款/退款/费用分摊)
│   ├── mapper/                     # PaymentBatchMapper, PaymentRecordMapper
│   └── model/entity/               # PaymentBatchEntity, PaymentRecordEntity
│
├── frontend/                       # 前端 SPA (纯静态)
│   └── index.html                  # 单页应用 (美团风格 UI)
│
├── sql/                            # 数据库初始化脚本
│   ├── user_db.sql                 # 用户库建表
│   ├── pool_db.sql                 # 拼单库建表
│   ├── order_db.sql                # 订单库建表
│   └── pay_db.sql                  # 支付库建表
│
├── docker-compose.yml              # 基础设施容器编排
├── pom.xml                         # Maven 父 POM
├── design.md                       # 系统设计文档
├── 测试计划.md                     # 测试计划
├── 测试用例.md                     # 测试用例
└── 测试报告.md                     # 测试报告
```

---

## 环境依赖

### 必需环境

| 软件 | 最低版本 | 说明 |
|------|---------|------|
| JDK | 17+ | 开发与运行环境 |
| Maven | 3.8+ | 项目构建 |
| MySQL | 8.0+ | 数据持久化 |
| Redis | 7.0+ | 缓存与分布式锁 |
| Nacos | 2.3.0+ | 服务注册与配置中心 |
| RocketMQ | 5.1.3+ | 消息队列（事务消息、延迟消息） |
| Docker | 20.10+ | 容器化部署基础设施 |
| Docker Compose | 2.0+ | 编排中间件容器 |

### 快速启动基础设施

```bash
# 使用 Docker Compose 一键启动 Redis + Nacos + RocketMQ
docker-compose up -d
```

启动后将可访问：
- Nacos 控制台：http://localhost:8848/nacos （用户名/密码：nacos/nacos）
- Redis：localhost:6379
- RocketMQ NameServer：localhost:9876

---

## 本地安装与运行

### 1. 克隆项目

```bash
git clone https://github.com/taikenuoleji/food-pooling-system.git
cd food-pooling-system
```

### 2. 启动基础设施

```bash
docker-compose up -d
```

### 3. 初始化数据库

依次执行 `sql/` 目录下的四个 SQL 脚本，创建数据库和表：

```bash
mysql -u root -p < sql/user_db.sql
mysql -u root -p < sql/pool_db.sql
mysql -u root -p < sql/order_db.sql
mysql -u root -p < sql/pay_db.sql
```

### 4. 初始化商家数据

执行商家和菜品的初始化 SQL：

```bash
mysql -u root -p pool_db < pool-service/src/main/resources/sql/init_merchants.sql
```

### 5. 配置 Nacos

启动 Nacos 后，访问 http://localhost:8848/nacos ，在配置管理中创建各服务的配置文件，或确保各服务的 `bootstrap.yml` 中配置正确。

> 各服务的 `application.yml` 和 `bootstrap.yml` 已预置了本地默认配置，开箱即用。

### 6. 构建项目

```bash
mvn clean package -DskipTests
```

### 7. 启动服务

按以下顺序启动各服务：

```bash
# 1. 网关
cd food-gateway
mvn spring-boot:run

# 2. 各业务服务（开新终端）
cd user-service && mvn spring-boot:run
cd pool-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
```

### 8. 访问前端

打开浏览器访问 `frontend/index.html`，或通过网关访问 `http://localhost:8080`。

### 9. 验证服务

```bash
# 检查 Nacos 注册中心是否注册成功
curl http://localhost:8848/nacos/v1/ns/service/list

# 测试用户注册接口
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","code":"123456","nickname":"测试用户"}'
```

---

## API 接口说明

所有接口统一前缀 `/api/v1/`，统一响应格式：

```json
{
  "code": 0,          // 0-成功, 4001-参数错误, 4003-无权限, 4004-不存在, 5000-服务错误
  "message": "success",
  "data": {},
  "traceId": "abc123def456"
}
```

### 用户服务 (User Service)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/v1/users/register` | 用户注册 | 无 |
| POST | `/api/v1/users/login` | 用户登录 | 无 |
| GET | `/api/v1/users/{userId}` | 获取用户信息 | 需要 |
| POST | `/api/v1/users/addresses` | 添加收货地址 | 需要 |
| GET | `/api/v1/users/{userId}/addresses` | 查询地址列表 | 需要 |

### 拼单服务 (Pool Service)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/v1/pools` | 创建拼单 | 需要 |
| POST | `/api/v1/pools/{poolId}/join` | 加入拼单 | 需要 |
| GET | `/api/v1/pools/{poolId}` | 查询拼单详情 | 无 |
| POST | `/api/v1/pools/{poolId}/leave` | 退出拼单 | 需要 |
| GET | `/api/v1/pools/my` | 我的拼单列表 | 需要 |
| POST | `/api/v1/pools/{poolId}/items` | 添加菜品到拼单 | 需要 |
| GET | `/api/v1/merchants` | 商家列表 | 无 |
| GET | `/api/v1/merchants/{merchantId}` | 商家详情 | 无 |
| GET | `/api/v1/merchants/{merchantId}/items` | 菜品列表 | 无 |

### 订单服务 (Order Service)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/orders/{orderId}` | 查询订单详情 | 需要 |
| GET | `/api/v1/orders` | 我的订单列表 | 需要 |
| POST | `/api/v1/orders/{orderId}/cancel` | 取消订单 | 需要 |

### 支付服务 (Payment Service)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/v1/payments/pool/{poolId}/charge` | 拼单成团扣款 | 内部 |
| POST | `/api/v1/payments/pool/{poolId}/refund` | 拼单退款 | 内部 |
| GET | `/api/v1/payments/pool/{poolId}/settlement` | 费用结算明细 | 需要 |
| GET | `/api/v1/payments/user/{userId}/records` | 用户支付记录 | 需要 |

> 详细请求/响应示例请参见 [design.md](./design.md)

---

## 拼单业务流程

### 状态流程图

```
┌─────────┐   加入/退出    ┌──────────┐  超时/最后一人退出  ┌──────────┐
│ CREATED │ ─────────────> │ FORMING  │ ─────────────────> │DISSOLVED │
└─────────┘                └────┬─────┘                    └──────────┘
                                │
                                │ 满足成团条件
                                ▼
                           ┌──────────┐
                           │  FORMED  │
                           └────┬─────┘
                                │
                        ┌───────┴───────┐
                        │               │
                   扣款成功          扣款失败
                        │               │
                        ▼               ▼
                  ┌──────────┐   ┌──────────┐
                  │ ORDERED  │   │DISSOLVED │
                  └──────────┘   │ (退款)   │
                                 └──────────┘
```

### 完整时序流程

```
用户A(发起者)        拼单服务           支付服务          订单服务         RocketMQ
    │                  │                 │                │                │
    │ 1.创建拼单        │                 │                │                │
    │─────────────────>│                 │                │                │
    │<─────────────────│                 │                │                │
    │ 2.分享邀请码      │                 │                │                │
    │                  │                 │                │                │
用户B,C               │                 │                │                │
    │ 3.加入拼单        │                 │                │                │
    │─────────────────>│                 │                │                │
    │                  │ 4.成团检测       │                │                │
    │                  │ 人数/金额/时间   │                │                │
    │                  │                 │                │                │
    │                  │ 5.计算费用分摊    │                │                │
    │                  │────────────────>│                │                │
    │                  │<────────────────│                │                │
    │                  │                 │                │                │
    │                  │ 6.状态→FORMED    │                │                │
    │                  │ 7.发布成团事件 ────────────────────────────────────>│
    │                  │                 │                │                │
    │                  │                 │ 8.生成订单<─────────────────────│
    │                  │                 │ 9.批量扣款<──────────────────────│
    │                  │                 │                │                │
    │ 10.通知成团       │                 │                │                │
    │<─────────────────│                 │                │                │
```

### 成团条件配置

成团条件采用**"或"逻辑**，满足任一即触发成团：

```json
{
  "formationRule": {
    "minMembers": 3,         // 最少参与人数（≥2）
    "minAmount": 5000,       // 最低菜品金额（分，≥商家起送价）
    "deadlineMinutes": 30    // 拼单有效时长（分钟，5~120）
  }
}
```

---

## 费用分摊算法

### 分摊模型

```
┌─────────────────────────────────────────────────────────┐
│                     用户应付总额                           │
├─────────────────────────────┬───────────────────────────┤
│   菜品费用（个人承担）          │  公共费用（按人数均摊）       │
│                             │                           │
│  · 谁点的谁付                 │  · 配送费：按参与人数均摊      │
│  · 精确到分                   │  · 包装费：按参与人数均摊      │
│                             │  · 优惠券：按菜品金额比例分摊   │
│                             │  · 余数处理：由最后加入者承担    │
└─────────────────────────────┴───────────────────────────┘
```

### 计算示例

| 用户 | 菜品金额 | 配送分摊 | 包装分摊 | 应付总额 |
|------|---------|---------|---------|---------|
| 小明 | ¥25.00 | ¥2.50 | ¥1.50 | ¥29.00 |
| 小红 | ¥35.00 | ¥2.50 | ¥1.50 | ¥39.00 |

**验证：** ¥29.00 + ¥39.00 = ¥68.00 = ¥60.00(菜品) + ¥5.00(配送) + ¥3.00(包装) ✓

> 详细算法实现和优惠券分摊示例请参见 [design.md](./design.md#4-费用分摊算法)

---

## 部署指南

### Docker Compose 部署 (推荐)

1. **为每个服务编写 Dockerfile**
   
   ```dockerfile
   FROM openjdk:17-jdk-slim
   COPY target/*.jar app.jar
   ENTRYPOINT ["java", "-jar", "/app.jar"]
   ```

2. **扩展 docker-compose.yml**

   在现有 `docker-compose.yml` 中添加各服务定义：

   ```yaml
   user-service:
     build: ./user-service
     ports:
       - "8081:8081"
     depends_on:
       - nacos
       - redis
       - mysql
     environment:
       - NACOS_SERVER_ADDR=nacos:8848
   ```

3. **完整启动**

   ```bash
   docker-compose up -d --build
   ```

### 生产环境部署建议

```
                    ┌─────────────┐
                    │   CDN/WAF   │
                    └──────┬──────┘
                           │
                    ┌──────┴──────┐
                    │   Nginx LB  │
                    │  (2节点HA)  │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ Gateway  │ │ Gateway  │ │ Gateway  │
        │  实例1   │ │  实例2   │ │  实例3   │
        └────┬─────┘ └────┬─────┘ └────┬─────┘
             │            │            │
    ┌────────┼────────────┼────────────┼────────┐
    ▼        ▼            ▼            ▼        ▼
  user    pool         order       payment     ...
  ×2      ×3           ×2          ×3

  ┌──────────────────────────────────────────────────┐
  │     Nacos集群(3节点) + RocketMQ集群 + Redis集群     │
  │     MySQL主从 + Seata TC + SkyWalking             │
  └──────────────────────────────────────────────────┘
```

**非功能需求指标：**

| 指标 | 要求 |
|------|------|
| 可用性 | 99.9% |
| API 响应时间 | P99 < 500ms |
| 成团判定延迟 | < 3 秒 |
| 数据持久化 | RPO = 0（零丢失） |

---

## 数据库设计

系统采用**数据库垂直拆分**，每个微服务拥有独立数据库：

| 数据库 | 所属服务 | 核心表 |
|--------|---------|--------|
| `user_db` | user-service | users, user_addresses |
| `pool_db` | pool-service | pools, pool_participants, pool_items, merchants, merchant_items |
| `order_db` | order-service | orders, order_items, order_participant_settlements |
| `pay_db` | payment-service | payment_batches, payment_records |

> 完整表结构请参见 [design.md](./design.md#5-数据库设计) 或 `sql/` 目录下的建表脚本。

---

## 贡献指南

### 分支策略

```
main          ← 生产分支，保持稳定
  └── develop ← 开发分支，日常开发合并
       └── feature/xxx    ← 功能分支
       └── bugfix/xxx     ← 修复分支
       └── hotfix/xxx     ← 紧急修复分支
```

### 贡献流程

1. **Fork** 本项目到你的 GitHub 账号
2. 从 `develop` 分支创建功能分支：`git checkout -b feature/your-feature`
3. 编写代码和测试，确保通过所有现有测试
4. 提交代码：`git commit -m "feat: 添加XXX功能"`
5. 推送到远端：`git push origin feature/your-feature`
6. 提交 **Pull Request** 到 `develop` 分支

### Commit 规范

本项目推荐使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
feat:     新功能
fix:      错误修复
docs:     文档更新
style:    代码格式调整
refactor: 代码重构
test:     测试相关
chore:    构建/工具链变更
```

### 代码规范

- 使用 Lombok 简化 POJO（`@Data`, `@Builder`, `@AllArgsConstructor`）
- Controller 层统一使用 `Result<T>` 返回
- Service 层通过 `BusinessException` 抛出业务异常
- 数据库操作使用 MyBatis-Plus 的 `LambdaQueryWrapper` / `LambdaUpdateWrapper`
- 并发场景使用 Redisson 分布式锁 + CAS 乐观锁

---

## 测试报告

### 测试概况

| 项目 | 内容 |
|------|------|
| 被测系统 | Food Pooling System v1.0 |
| 测试环境 | JDK 17, MySQL 9.6.0, Redis, Nacos 2.3.0, RocketMQ 5.1.3 |
| 测试范围 | 24 个 API 端点 + 前端 7 个页面 + 4 个端到端集成流程 |
| 总用例数 | 73 |
| 通过数 | 61 |
| 通过率 | **83.6%** |

### 各模块通过率

| 模块 | 用例数 | 通过率 |
|------|--------|--------|
| 用户模块 | 8 | 87.5% |
| 商家模块 | 7 | 85.7% |
| 拼单模块 | 18 | 77.8% |
| 订单模块 | 5 | 60.0% |
| 支付模块 | 5 | 80.0% |
| 网关鉴权 | 6 | 100% |
| 前端功能 | 20 | 90.0% |
| 集成测试 | 4 | 75.0% |

### 已知问题（需上线前修复）

| 优先级 | 缺陷 | 说明 |
|--------|------|------|
| 🔴 P0 | `cancelOrder` 无权限校验 | 用户可取消他人订单 |
| 🔴 P0 | `refund` 接口可重复退款 | 缺少幂等性保护 |
| 🔴 P0 | `login` 无密码/验证码校验 | 仅凭手机号即可登录 |
| 🟡 P1 | `leavePool` 缺少并发保护 | 高并发下数据不一致 |
| 🟡 P1 | 缺少全局异常处理器 | 异常时返回原始错误页 |

> 完整测试报告请参见 [测试报告.md](./测试报告.md)

---

## 许可证

本项目采用 [MIT License](LICENSE) 开源。

---

## 相关文档

- [系统设计文档](./design.md) — 完整架构设计、API 接口、数据库、算法详解
- [测试计划](./测试计划.md) — 测试范围与策略
- [测试用例](./测试用例.md) — 73 个测试用例详情
- [测试报告](./测试报告.md) — 缺陷分析与修复建议

---

<p align="center">
  <b>Food Pooling System</b> — 让拼单更简单 🍕
</p>
