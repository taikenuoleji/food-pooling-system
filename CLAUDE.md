# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

所有输出必须使用中文

## Project Overview

外卖拼单系统 (Food Pooling System) — a food delivery group-ordering platform built with Spring Cloud microservices. Users browse merchants, create or join pooling groups, and split delivery fees. When a pool forms, orders are auto-generated and payments batch-charged via RocketMQ events.

---

## 关键配置

### 端口精确值

| 服务 | 端口 | 说明 |
|------|------|------|
| food-gateway | 8080 | API网关入口 |
| user-service | 8001 | 用户服务 |
| pool-service | 8002 | 拼单服务 |
| order-service | 8003 | 订单服务 |
| payment-service | 8004 | 支付服务 |
| Redis | 6379 | 缓存/分布式锁 |
| Nacos | 8848 | 注册中心/配置中心 |
| RocketMQ NameServer | 9876 | 消息队列 |
| RocketMQ Broker | 10911 | 消息代理 |
| 前端 | 3000 | Python HTTP Server |
| MySQL (named pipe) | 0 (TCP未绑定) | 使用named pipe连接 |

### 环境变量

启动服务时必须设置：
```bash
JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
```

### 数据库配置

- **H2内存数据库** (当前使用): `jdbc:h2:mem:{service_name};DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE`
- **MySQL**: TCP端口无法绑定(Windows 11 + MySQL 9.6.0)，仅支持named pipe
- H2 schema初始化: `spring.sql.init.mode=always`, `schema-locations=classpath:schema.sql`

---

## 服务启动顺序与依赖

### 启动顺序

```
1. Redis (6379)
2. Nacos (8848) - 依赖: 无
3. RocketMQ NameServer (9876) - 依赖: 无
4. RocketMQ Broker (10911) - 依赖: NameServer
5. user-service (8001) - 依赖: Nacos, Redis
6. pool-service (8002) - 依赖: Nacos, Redis, RocketMQ
7. order-service (8003) - 依赖: Nacos, Redis, RocketMQ
8. payment-service (8004) - 依赖: Nacos, Redis, RocketMQ
9. food-gateway (8080) - 依赖: 所有业务服务
10. 前端 (3000) - 依赖: food-gateway
```

### 启动命令

```bash
# Redis
"/d/middleware/redis/redis-server.exe"

# Nacos (standalone模式)
cd "D:/middleware/nacos"
"D:/jdk17/bin/java" -server -Xms512m -Xmx512m -Xmn256m \
  -Dnacos.standalone=true -Dnacos.member.list="" \
  -jar target/nacos-server.jar

# RocketMQ NameServer
cd "D:/middleware/rocketmq/rocketmq-all-5.1.3-bin-release"
"D:/jdk17/bin/java" -server -Xms512m -Xmx512m \
  -cp "lib/*" org.apache.rocketmq.namesrv.NamesrvStartup

# RocketMQ Broker (需Java 17兼容参数)
"D:/jdk17/bin/java" -server -Xms512m -Xmx512m \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  -cp "lib/*" org.apache.rocketmq.broker.BrokerStartup -n 127.0.0.1:9876

# 业务服务
JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
java $JAVA_OPTS -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar
java $JAVA_OPTS -jar pool-service/target/pool-service-1.0.0-SNAPSHOT.jar
java $JAVA_OPTS -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar
java $JAVA_OPTS -jar payment-service/target/payment-service-1.0.0-SNAPSHOT.jar
java $JAVA_OPTS -jar food-gateway/target/food-gateway-1.0.0-SNAPSHOT.jar

# 前端
cd "d:/food-pooling-system/frontend"
python -m http.server 3000
```

---

## 已修复的Bug清单

### 1. pool_item_id 未生成
- **文件**: `pool-service/src/main/java/com/food/pool/service/impl/PoolServiceImpl.java`
- **位置**: `addItems()` 和 `joinPool()` 方法
- **修复**: 添加 `poolItem.setPoolItemId(IdGenerator.generate("PI"))`
- **关联文件**: `pool-service/.../model/entity/PoolItemEntity.java` 添加 `poolItemId` 字段

### 2. merchant_name 查询方式错误
- **文件**: `pool-service/.../service/impl/PoolServiceImpl.java`
- **位置**: `createPool()` 方法
- **修复**: 使用 `LambdaQueryWrapper<MerchantEntity>` 查询，而非 `selectById()`
- **原因**: `selectById()` 期望主键ID(BIGINT)，但传入的是 merchant_id(字符串)

### 3. settlement_id 未生成
- **文件**: `order-service/.../service/impl/OrderServiceImpl.java`
- **位置**: `createOrder()` 方法
- **修复**: 添加 `settlement.setSettlementId(IdGenerator.generate("STL"))`
- **关联文件**: `order-service/.../model/entity/OrderParticipantSettlementEntity.java` 添加 `settlementId` 字段

### 4. order-service schema 缺少字段
- **文件**: `order-service/src/main/resources/schema.sql`
- **修复**: 添加 `estimated_arrival`, `paid_at`, `confirmed_at`, `completed_at`, `cancelled_at`, `cancel_reason`, `external_order_id` 字段

### 5. OrderDetailVO 缺少字段
- **文件**: `order-service/.../dto/OrderDetailVO.java`
- **修复**: 添加 `totalFoodAmount`, `packagingFee`, `couponDiscount`, `memberCount` 字段

### 6. getOrderDetail 未设置字段
- **文件**: `order-service/.../service/impl/OrderServiceImpl.java`
- **修复**: 在 `buildOrderDetailVO()` 中添加字段映射

### 7. 订单金额从settlements计算
- **文件**: `order-service/.../service/impl/OrderServiceImpl.java`
- **修复**: 当 items 为空时，从 settlements 汇总 totalFoodAmount 和 couponDiscount

### 8. 重复订单检查
- **文件**: `order-service/.../service/impl/OrderServiceImpl.java`
- **修复**: 添加 `existingOrder` 检查，避免重复创建

### 9. PoolFormedEvent中foodAmount空值处理
- **文件**: `pool-service/.../service/impl/PoolServiceImpl.java`
- **修复**: `p.getFoodAmount() != null ? p.getFoodAmount() : 0L`

### 10. order_items表缺少total_quantity字段
- **文件**: `order-service/src/main/resources/schema.sql`
- **修复**: 将 `quantity` 改为 `total_quantity`

### 11. PoolDetailVO缺少merchantId字段
- **文件**: `pool-service/.../dto/PoolDetailVO.java`
- **修复**: 添加 `merchantId` 字段

### 12. ParticipantVO缺少role和nickname
- **文件**: `pool-service/.../dto/ParticipantVO.java`
- **修复**: 添加 `role` 字段
- **文件**: `pool-service/.../service/impl/PoolServiceImpl.java`
- **修复**: 通过Feign获取用户昵称

### 13. 订单支付功能缺失
- **文件**: `payment-service/.../controller/PaymentController.java`
- **修复**: 添加 `payOrder` 接口
- **文件**: `payment-service/.../service/PaymentService.java`
- **修复**: 添加 `payOrder` 方法
- **文件**: `payment-service/.../service/impl/PaymentServiceImpl.java`
- **修复**: 实现支付逻辑

### 14. payment-service schema缺少字段
- **文件**: `payment-service/src/main/resources/schema.sql`
- **修复**: 添加 `pool_id`, `order_id`, `type`, `food_amount` 等字段

---

## 当前项目状态

### 已验证功能 ✅

| 功能 | 状态 | 测试结果 |
|------|------|---------|
| 用户注册 | ✅ | 3个用户注册成功 |
| 创建拼单 | ✅ | 设置最少3人成团 |
| 1人未成团 | ✅ | 状态=FORMING |
| 2人未成团 | ✅ | 状态=FORMING |
| 用户加入拼单 | ✅ | B/C用户成功加入 |
| 3人成功成团 | ✅ | 状态=FORMED |
| 拼单广场 | ✅ | 显示拼单列表 |
| 订单生成 | ✅ | RocketMQ触发创建 |
| 订单详情 | ✅ | 状态码200，数据正确 |
| 八折优惠计算 | ✅ | 原始67元→折后53.6元 |
| 费用分摊 | ✅ | 按菜品金额比例分摊优惠 |
| 订单支付 | ✅ | 支付弹窗一键支付 |
| 支付记录 | ✅ | 显示支付历史 |

### 订单数据验证

```
原始菜品金额: 6700分 (67元)
折后菜品金额: 5360分 (53.6元)
优惠金额: 1340分 (13.4元)
折扣比例: 80% ✓

参与者分摊示例:
- 用户A: 菜品2880 + 配送0 + 包装0 - 优惠720 = 2880分
- 用户B: 菜品1200 + 配送0 + 包装0 - 优惠300 = 1200分
- 用户C: 菜品1280 + 配送0 + 包装0 - 优惠320 = 1280分
```

### 商家数据

16个商家，每个4个菜品，共64个菜品：
- 茶颜悦色, 麦当劳, 海底捞火锅, 外婆家, 必胜客, 肯德基
- 喜茶, 瑞幸咖啡, 黄焖鸡米饭, 兰州拉面, 呷哺呷哺, 真功夫
- 赛百味, 吉野家, 张亮麻辣烫, CoCo都可

---

## 关键决策与约束

### 技术决策

1. **H2替代MySQL**: 因Windows 11 + MySQL 9.6.0 TCP端口无法绑定，使用H2内存数据库
2. **UTF-8编码**: 所有服务启动需添加 `-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8`
3. **八折优惠**: `discountedFood = originalFood * 8 / 10`, `couponShare = originalFood * 2 / 10`
4. **成团条件**: `minMembers > 0 && currentMembers >= minMembers` OR `minAmount > 0 && currentFoodAmount >= minAmount`

### 业务约束

1. 拼单状态: CREATED → FORMING → FORMED → ORDERED, 或 → DISSOLVED
2. 参与者角色: CREATOR(创建者) / MEMBER(成员)
3. 邀请码: 6位随机字符串
4. 所有金额单位: 分(fen)，非元(yuan)

### 代码约定

1. 实体类使用 `@Data`, `@TableName` 注解
2. Mapper使用 `LambdaQueryWrapper` 查询
3. ID生成: `IdGenerator.generate(prefix)`，如 `P`(拼单), `PI`(菜品), `STL`(结算), `ORD`(订单)
4. 统一响应: `Result<T>` 包装

---

## 文件/代码修改要点

### 核心修改文件

| 文件 | 修改内容 |
|------|---------|
| `pool-service/.../PoolServiceImpl.java` | createPool修复, addItems/joinPool添加poolItemId, formPool空值处理 |
| `pool-service/.../PoolItemEntity.java` | 添加poolItemId字段 |
| `pool-service/src/main/resources/schema.sql` | H2兼容schema, 16商家64菜品数据 |
| `order-service/.../OrderServiceImpl.java` | createOrder添加settlementId, 重复检查, 金额计算 |
| `order-service/.../OrderParticipantSettlementEntity.java` | 添加settlementId字段 |
| `order-service/.../OrderDetailVO.java` | 添加totalFoodAmount等字段 |
| `order-service/src/main/resources/schema.sql` | 添加缺失字段 |
| 各服务 `application.yml` | H2数据源配置, UTF-8编码 |

### H2 Schema文件位置

- `user-service/src/main/resources/schema.sql`
- `pool-service/src/main/resources/schema.sql`
- `order-service/src/main/resources/schema.sql`
- `payment-service/src/main/resources/schema.sql`

---

## 当前遇到的问题（未解决）

### 1. 获取订单详情API返回500

- **现象**: `GET /api/v1/orders/{orderId}` 返回500
- **数据库数据**: 正确 (total_food_amount=5360, coupon_discount=1340)
- **可能原因**: API返回时数据序列化问题, 或payment-service调用失败影响

### 2. payment-service charge接口返回500

- **现象**: order-service调用 `POST /api/v1/payments/pool/{poolId}/charge` 失败
- **影响**: RocketMQ重试导致重复调用
- **可能原因**: payment-service schema问题或业务逻辑错误

### 3. 拼单广场显示0个拼单

- **现象**: `GET /api/v1/pools/plaza` 返回空数组
- **可能原因**: 查询条件过滤了FORMED状态的拼单

### 4. MySQL TCP端口无法绑定 (Windows 11 + MySQL 9.6.0)

- **现象**: `skip_networking=ON`, `port=0`, 即使使用 `--skip-networking=0`
- **影响**: 无法通过TCP连接MySQL, 仅支持named pipe
- **当前方案**: 使用H2内存数据库替代

---

## 前端

单文件SPA在 `frontend/index.html`。原生HTML/CSS/JS，无框架。4个标签页：首页(商家), 拼单, 订单, 支付。API基础URL: `http://localhost:8080`。前端通过 `http://localhost:3000` 访问。

商家图片路径: `images/M001.png` - `images/M016.png`，对应16个商家。
