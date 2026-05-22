# 外卖拼单系统设计文档

> 版本：v1.0
> 日期：2026-05-22

---

## 目录

1. [系统架构设计](#1-系统架构设计)
2. [核心API接口设计](#2-核心api接口设计)
3. [拼单核心流程](#3-拼单核心流程)
4. [费用分摊算法](#4-费用分摊算法)
5. [数据库设计](#5-数据库设计)
6. [技术选型建议](#6-技术选型建议)
7. [关键难点与解决方案](#7-关键难点与解决方案)

---

## 1. 系统架构设计

### 1.1 微服务划分及职责

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                              │
│                   (Spring Cloud Gateway)                        │
│              统一入口 / 鉴权 / 限流 / 路由                       │
└──────┬──────────┬──────────┬──────────┬─────────────────────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│  用户服务  │ │ 拼单服务  │ │ 订单服务  │ │ 支付服务  │
│  (User)   │ │ (Pool)   │ │ (Order)  │ │ (Payment)│
│           │ │          │ │          │ │          │
│ ·注册登录  │ │ ·创建拼单 │ │ ·生成订单 │ │ ·预收款   │
│ ·信息管理  │ │ ·加入拼单 │ │ ·状态追踪 │ │ ·费用分摊 │
│ ·地址管理  │ │ ·成团检测 │ │ ·商家对接 │ │ ·退款     │
│           │ │ ·解散/超时│ │          │ │ ·明细计算 │
└─────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
      │            │            │            │
      ▼            ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│  MySQL   │ │  MySQL   │ │  MySQL   │ │  MySQL   │
│ user_db  │ │ pool_db  │ │ order_db │ │ pay_db   │
└──────────┘ └──────────┘ └──────────┘ └──────────┘
```

**用户服务（user-service）**

- 用户注册/登录（手机号+验证码，支持微信OAuth）
- 用户基本信息管理（昵称、头像）
- 收货地址管理（CRUD，设默认地址）
- 用户身份鉴权（JWT签发与验证）

**拼单服务（pool-service）**

- 拼单生命周期管理：创建 → 分享 → 加入 → 成团/超时解散
- 成团条件判定引擎（人数/金额/时间，支持组合条件）
- 参与者管理（加入、退出、踢出）
- 拼单商品选择（每人独立选菜）
- 拼单状态机驱动，状态变更事件发布

**订单服务（order-service）**

- 拼单成团后生成正式商家订单
- 订单状态跟踪（待接单→已接单→配送中→已完成→已取消）
- 对接商家/配送系统（预留接口）
- 订单历史查询

**支付服务（payment-service）**

- 拼单预收款（冻结资金）
- 费用分摊计算（按商品明细分摊 + 按比例分摊公共费用）
- 拼单成团后资金结算
- 拼单解散时退款处理
- 对接第三方支付渠道（微信支付/支付宝）

### 1.2 服务间调用关系

```
用户 ──→ API Gateway ──→ 各微服务

内部调用关系（箭头表示"调用"）：

pool-service ──同步调用──→ user-service     （校验用户身份、获取地址）
pool-service ──同步调用──→ payment-service   （成团时发起预收款扣款）
pool-service ──发布事件──→ MQ ──→ order-service    （成团事件 → 生成订单）
pool-service ──发布事件──→ MQ ──→ payment-service  （解散事件 → 退款）

order-service ──发布事件──→ MQ ──→ payment-service  （订单完成 → 结算）
order-service ──同步调用──→ user-service     （获取用户地址用于配送）

调用方式：
  · 同步调用：OpenFeign（强一致性校验场景）
  · 异步事件：RocketMQ/Kafka（状态通知、解耦场景）
```

**事件流总览：**

| 事件 | 生产者 | 消费者 | 说明 |
|------|--------|--------|------|
| pool.formed | pool-service | order-service, payment-service | 拼单成团，触发订单生成+扣款 |
| pool.dissolved | pool-service | payment-service | 拼单解散，触发退款 |
| order.completed | order-service | payment-service | 订单完成，触发资金结算 |
| payment.success | payment-service | pool-service, order-service | 支付成功，更新状态 |
| payment.refund | payment-service | order-service | 退款完成，通知订单服务 |

---

## 2. 核心API接口设计

### 2.1 用户服务（user-service）

#### POST /api/v1/users/register

用户注册

**请求：**

```json
{
  "phone": "13800138000",
  "code": "123456",
  "nickname": "小明"
}
```

**响应（201）：**

```json
{
  "code": 0,
  "data": {
    "userId": "U2026052200001",
    "phone": "138****8000",
    "nickname": "小明",
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

#### POST /api/v1/users/login

用户登录（手机号验证码）

**请求：**

```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

**响应（200）：** 同注册响应格式。

#### GET /api/v1/users/{userId}

获取用户信息（需登录）

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "userId": "U2026052200001",
    "phone": "138****8000",
    "nickname": "小明",
    "avatar": "https://cdn.example.com/avatars/xxx.jpg",
    "defaultAddressId": "ADDR001"
  }
}
```

#### POST /api/v1/users/addresses

添加收货地址

**请求：**

```json
{
  "label": "公司",
  "detail": "北京市海淀区中关村大街1号",
  "contactName": "小明",
  "contactPhone": "13800138000",
  "isDefault": true
}
```

**响应（201）：**

```json
{
  "code": 0,
  "data": {
    "addressId": "ADDR002",
    "label": "公司",
    "detail": "北京市海淀区中关村大街1号"
  }
}
```

#### GET /api/v1/users/{userId}/addresses

获取用户地址列表

**响应（200）：**

```json
{
  "code": 0,
  "data": [
    {
      "addressId": "ADDR001",
      "label": "家",
      "detail": "北京市朝阳区xxx小区3号楼",
      "isDefault": true
    }
  ]
}
```

---

### 2.2 拼单服务（pool-service）

#### POST /api/v1/pools

创建拼单（需登录）

**请求：**

```json
{
  "merchantId": "M10001",
  "merchantName": "张记麻辣烫",
  "creatorAddressId": "ADDR001",
  "formationRule": {
    "minMembers": 3,
    "minAmount": 5000,
    "deadlineMinutes": 30
  },
  "remark": "微辣就好"
}
```

> `formationRule` 中三个条件为"或"关系，满足任一即成团。金额单位为分。

**响应（201）：**

```json
{
  "code": 0,
  "data": {
    "poolId": "P2026052200001",
    "inviteCode": "A3F8K2",
    "status": "FORMING",
    "shareUrl": "https://app.example.com/join?code=A3F8K2",
    "createdAt": "2026-05-22T10:00:00+08:00",
    "expiresAt": "2026-05-22T10:30:00+08:00"
  }
}
```

#### POST /api/v1/pools/{poolId}/join

加入拼单（需登录）

**请求：**

```json
{
  "addressId": "ADDR003",
  "items": [
    {
      "itemId": "MI10001",
      "itemName": "麻辣烫大份",
      "unitPrice": 2500,
      "quantity": 1
    },
    {
      "itemId": "MI10005",
      "itemName": "冰红茶",
      "unitPrice": 500,
      "quantity": 2
    }
  ],
  "remark": "不要香菜"
}
```

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "participantId": "PT2026052200003",
    "poolId": "P2026052200001",
    "currentMembers": 2,
    "currentTotalAmount": 7000,
    "poolStatus": "FORMING"
  }
}
```

#### GET /api/v1/pools/{poolId}

查询拼单详情

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "poolId": "P2026052200001",
    "merchantName": "张记麻辣烫",
    "status": "FORMING",
    "formationRule": {
      "minMembers": 3,
      "minAmount": 5000,
      "deadlineMinutes": 30
    },
    "currentMembers": 2,
    "currentTotalAmount": 7000,
    "createdAt": "2026-05-22T10:00:00+08:00",
    "expiresAt": "2026-05-22T10:30:00+08:00",
    "participants": [
      {
        "userId": "U2026052200001",
        "nickname": "小明",
        "items": [
          { "itemName": "麻辣烫大份", "unitPrice": 2500, "quantity": 1 }
        ],
        "subtotal": 2500,
        "joinedAt": "2026-05-22T10:00:00+08:00"
      },
      {
        "userId": "U2026052200002",
        "nickname": "小红",
        "items": [
          { "itemName": "麻辣烫大份", "unitPrice": 2500, "quantity": 1 },
          { "itemName": "冰红茶", "unitPrice": 500, "quantity": 2 }
        ],
        "subtotal": 4500,
        "joinedAt": "2026-05-22T10:05:00+08:00"
      }
    ]
  }
}
```

#### POST /api/v1/pools/{poolId}/leave

退出拼单（仅在FORMING状态下可退出）

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "poolId": "P2026052200001",
    "currentMembers": 1,
    "poolStatus": "FORMING",
    "refundStatus": "REFUND_INITIATED"
  }
}
```

#### GET /api/v1/pools/my

查询我的拼单列表（需登录，分页）

**Query参数：** `?status=FORMING&page=1&size=10`

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "total": 3,
    "list": [
      {
        "poolId": "P2026052200001",
        "merchantName": "张记麻辣烫",
        "status": "FORMING",
        "role": "CREATOR",
        "mySubtotal": 2500,
        "joinedAt": "2026-05-22T10:00:00+08:00"
      }
    ]
  }
}
```

---

### 2.3 订单服务（order-service）

#### GET /api/v1/orders/{orderId}

查询订单详情

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "orderId": "ORD2026052200001",
    "poolId": "P2026052200001",
    "merchantName": "张记麻辣烫",
    "status": "DELIVERING",
    "totalAmount": 7500,
    "deliveryFee": 500,
    "items": [
      { "itemName": "麻辣烫大份", "quantity": 2, "totalPrice": 5000 },
      { "itemName": "冰红茶", "quantity": 2, "totalPrice": 1000 }
    ],
    "participants": [
      {
        "userId": "U2026052200001",
        "nickname": "小明",
        "foodAmount": 2500,
        "deliveryShare": 167,
        "packagingShare": 100,
        "totalPay": 2767
      },
      {
        "userId": "U2026052200002",
        "nickname": "小红",
        "foodAmount": 3500,
        "deliveryShare": 167,
        "packagingShare": 100,
        "totalPay": 3767
      },
      {
        "userId": "U2026052200003",
        "nickname": "小李",
        "foodAmount": 0,
        "deliveryShare": 166,
        "packagingShare": 100,
        "totalPay": 266
      }
    ],
    "estimatedArrival": "2026-05-22T11:00:00+08:00",
    "createdAt": "2026-05-22T10:30:05+08:00"
  }
}
```

#### GET /api/v1/orders

查询我的订单列表（需登录，分页）

**Query参数：** `?status=COMPLETED&page=1&size=10`

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "total": 15,
    "list": [
      {
        "orderId": "ORD2026052200001",
        "merchantName": "张记麻辣烫",
        "status": "COMPLETED",
        "myPayAmount": 2767,
        "createdAt": "2026-05-22T10:30:05+08:00"
      }
    ]
  }
}
```

#### POST /api/v1/orders/{orderId}/cancel

取消订单（仅限待接单状态）

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "orderId": "ORD2026052200001",
    "status": "CANCELLED",
    "refundStatus": "REFUND_INITIATED"
  }
}
```

---

### 2.4 支付服务（payment-service）

#### POST /api/v1/payments/pool/{poolId}/charge

拼单成团扣款（内部接口，由拼单服务在成团时调用）

**请求：**

```json
{
  "poolId": "P2026052200001",
  "orderId": "ORD2026052200001",
  "charges": [
    {
      "userId": "U2026052200001",
      "foodAmount": 2500,
      "deliveryShare": 167,
      "packagingShare": 100,
      "totalAmount": 2767
    },
    {
      "userId": "U2026052200002",
      "foodAmount": 3500,
      "deliveryShare": 167,
      "packagingShare": 100,
      "totalAmount": 3767
    },
    {
      "userId": "U2026052200003",
      "foodAmount": 0,
      "deliveryShare": 166,
      "packagingShare": 100,
      "totalAmount": 266
    }
  ]
}
```

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "paymentBatchId": "PB2026052200001",
    "poolId": "P2026052200001",
    "totalCharged": 6800,
    "results": [
      { "userId": "U2026052200001", "amount": 2767, "status": "CHARGED" },
      { "userId": "U2026052200002", "amount": 3767, "status": "CHARGED" },
      { "userId": "U2026052200003", "amount": 266, "status": "CHARGED" }
    ]
  }
}
```

#### POST /api/v1/payments/pool/{poolId}/refund

拼单退款（解散或取消时调用）

**请求：**

```json
{
  "poolId": "P2026052200001",
  "reason": "POOL_DISSOLVED_TIMEOUT"
}
```

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "refundBatchId": "RB2026052200001",
    "poolId": "P2026052200001",
    "totalRefunded": 7000,
    "refunds": [
      { "userId": "U2026052200001", "amount": 2500, "status": "REFUNDED" },
      { "userId": "U2026052200002", "amount": 4500, "status": "REFUNDED" }
    ]
  }
}
```

#### GET /api/v1/payments/pool/{poolId}/settlement

查询拼单费用结算明细

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "poolId": "P2026052200001",
    "orderId": "ORD2026052200001",
    "splitMethod": "ITEM_BASED",
    "commonFees": {
      "deliveryFee": 500,
      "packagingFee": 300
    },
    "settlements": [
      {
        "userId": "U2026052200001",
        "nickname": "小明",
        "foodItems": [
          { "itemName": "麻辣烫大份", "price": 2500 }
        ],
        "foodAmount": 2500,
        "deliveryShare": 167,
        "packagingShare": 100,
        "totalAmount": 2767,
        "payStatus": "PAID"
      },
      {
        "userId": "U2026052200002",
        "nickname": "小红",
        "foodItems": [
          { "itemName": "麻辣烫大份", "price": 2500 },
          { "itemName": "冰红茶x2", "price": 1000 }
        ],
        "foodAmount": 3500,
        "deliveryShare": 167,
        "packagingShare": 100,
        "totalAmount": 3767,
        "payStatus": "PAID"
      }
    ]
  }
}
```

#### GET /api/v1/payments/user/{userId}/records

查询用户支付记录（需登录，分页）

**Query参数：** `?page=1&size=10`

**响应（200）：**

```json
{
  "code": 0,
  "data": {
    "total": 20,
    "list": [
      {
        "recordId": "PR2026052200001",
        "poolId": "P2026052200001",
        "type": "CHARGE",
        "amount": 2767,
        "status": "SUCCESS",
        "createdAt": "2026-05-22T10:30:05+08:00"
      }
    ]
  }
}
```

---

## 3. 拼单核心流程

### 3.1 完整时序流程

```
用户A(发起者)       拼单服务          支付服务          订单服务          MQ
    │                │                │                │                │
    │  1.创建拼单     │                │                │                │
    │───────────────>│                │                │                │
    │  返回拼单ID     │                │                │                │
    │<───────────────│                │                │                │
    │                │                │                │                │
    │  2.分享邀请码给B、C                │                │                │
    │                │                │                │                │
用户B │                │                │                │                │
    │  3.加入拼单     │                │                │                │
    │───────────────>│                │                │                │
    │  加入成功       │                │                │                │
    │<───────────────│                │                │                │
    │                │                │                │                │
用户C │                │                │                │                │
    │  4.加入拼单     │                │                │                │
    │───────────────>│                │                │                │
    │                │                │                │                │
    │                │ 5.成团检测      │                │                │
    │                │ ┌────────────┐ │                │                │
    │                │ │ 满足条件？   │ │                │                │
    │                │ │ 人数≥3 ✓   │ │                │                │
    │                │ └────────────┘ │                │                │
    │                │                │                │                │
    │                │ 6.计算费用分摊   │                │                │
    │                │───────────────>│                │                │
    │                │  分摊结果       │                │                │
    │                │<───────────────│                │                │
    │                │                │                │                │
    │                │ 7.锁定拼单      │                │                │
    │                │  状态→FORMED   │                │                │
    │                │                │                │                │
    │  加入成功       │                │                │                │
    │<───────────────│                │                │                │
    │                │                │                │                │
    │                │ 8.发布成团事件──────────────────────────────────>│
    │                │                │                │                │
    │                │                │  9.消费事件：生成订单             │
    │                │                │                │<───────────────│
    │                │                │                │                │
    │                │                │  10.消费事件：扣款               │
    │                │                │<───────────────────────────────│
    │                │                │                │                │
    │                │                │ 11.扣款成功     │                │
    │                │                │───────────────>│                │
    │                │                │                │  12.通知商家    │
    │                │                │                │──────────>商家  │
    │                │                │                │                │
    │  13.通知所有参与人：成团了！                                        │
    │<───────────────│                │                │                │
    │                │                │                │                │
    │                │                │                │  14.商家接单    │
    │                │                │                │<────────────── │
    │                │                │                │                │
    │                │                │                │  15.配送中      │
    │  16.推送订单状态│                │                │                │
    │<───────────────────────────────────────────────────────────────── │
    │                │                │                │                │
    │                │                │                │  17.订单完成    │
    │                │                │  18.结算事件    │                │
    │                │                │<─────────────────────────────── │
    │                │                │                │                │
    │  19.推送完成通知│                │                │                │
    │<───────────────────────────────────────────────────────────────── │
```

### 3.2 状态机

**拼单状态流转：**

```
                    ┌─────────────────────────────┐
                    │                             │
                    ▼                             │
 ┌─────────┐   加入/退出    ┌──────────┐  超时/最后一人退出  ┌──────────┐
 │ CREATED │ ─────────────> │ FORMING  │ ─────────────────> │DISSOLVED │
 └─────────┘  (自动进入)     └────┬─────┘                    └──────────┘
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
                    │ORDERED   │   │DISSOLVED │
                    └──────────┘   │ (退款)   │
                                   └──────────┘
```

**状态说明：**

| 状态 | 含义 | 可执行操作 |
|------|------|-----------|
| CREATED | 已创建，等待第一个人加入 | 加入、解散 |
| FORMING | 拼单中，已有参与者 | 加入、退出、解散 |
| FORMED | 已成团，等待扣款下单 | 无（系统自动处理） |
| ORDERED | 已下单，商家处理中 | 查看订单状态 |
| DISSOLVED | 已解散/取消 | 查看（只读） |

### 3.3 成团条件配置规则

成团条件采用"或"逻辑（满足任一即触发成团）：

```json
{
  "formationRule": {
    "minMembers": 3,
    "minAmount": 5000,
    "deadlineMinutes": 30
  }
}
```

| 条件字段 | 类型 | 说明 | 示例 |
|---------|------|------|------|
| `minMembers` | int | 最少参与人数 | 3人 |
| `minAmount` | int(分) | 最低总金额（仅菜品金额） | 50元 = 5000分 |
| `deadlineMinutes` | int | 拼单有效时长（分钟） | 30分钟 |

**判定逻辑（伪代码）：**

```java
boolean shouldForm(Pool pool) {
    FormationRule rule = pool.getFormationRule();

    boolean memberMet = pool.getCurrentMembers() >= rule.getMinMembers();
    boolean amountMet = pool.getCurrentFoodAmount() >= rule.getMinAmount();
    boolean timeoutMet = LocalDateTime.now().isAfter(pool.getExpiresAt());

    return memberMet || amountMet || timeoutMet;
}
```

**配置约束：**

- `minMembers` 至少为2，最大不超过商家单笔接单上限（如20人）
- `minAmount` 不得低于商家起送价
- `deadlineMinutes` 范围 5~120 分钟，默认30分钟
- 三个条件至少设置一个

### 3.4 拼单超时/解散处理策略

**超时处理：**

```
定时任务（每30秒扫描）
    │
    ├── 查询 FORMING 状态 且 expiresAt < now 的拼单
    │
    └── 对每个超时拼单：
         1. 状态更新为 DISSOLVED（CAS乐观锁）
         2. 发布 pool.dissolved 事件
         3. 支付服务消费事件 → 退款给已参与用户
         4. 通知所有参与者"拼单已解散"
```

**主动退出/解散：**

| 场景 | 触发者 | 条件 | 处理 |
|------|--------|------|------|
| 参与者退出 | 任意参与者 | 状态为FORMING | 移除该参与者，退款，若剩余人数<2则自动解散 |
| 发起者解散 | 发起者 | 状态为FORMING | 全员解散，全部退款 |
| 系统超时 | 定时任务 | 超过deadline | 自动解散，全部退款 |
| 成团后取消 | 发起者 | 状态为FORMED但未接单 | 需所有参与者同意，全额退款 |

**退款策略：**

- FORMING状态退出：按该用户已选菜品金额原路退回
- 超时解散：所有已参与用户全额退款
- 成团后未接单取消：全额退款
- 已接单后取消：与商家协商，扣除已制作部分费用后退款

---

## 4. 费用分摊算法

### 4.1 分摊模型

费用分为两类：

```
┌─────────────────────────────────────────────────┐
│                  用户应付总额                      │
├────────────────────────┬────────────────────────┤
│    菜品费用（个人承担）    │    公共费用（按比例均摊）   │
│                        │                        │
│  · 用户A的菜品 = 25元   │  · 配送费 5元（按人头）   │
│  · 用户B的菜品 = 35元   │  · 包装费 3元（按人头）   │
│  · 用户C的菜品 =  0元   │  · 优惠券（按比例分摊）   │
│                        │                        │
│  各自承担自己的          │  所有人平均分担            │
└────────────────────────┴────────────────────────┘
```

**分摊原则：**

1. **菜品费用**：谁点的谁付，精确到分
2. **配送费**：按参与人数均摊，余数由最后加入的参与者承担
3. **包装费**：按参与人数均摊，余数由最后加入的参与者承担
4. **优惠券**：按菜品金额比例分摊
5. **凑整处理**：每人实付金额向下取整到分，总差额由发起者承担

### 4.2 计算示例

**场景：用户A点了30元，用户B点了20元，配送费5元，包装费3元**

```
菜品总金额 = 30 + 20 = 50元
参与人数 = 2人
配送费 = 5元
包装费 = 3元
公共费用合计 = 5 + 3 = 8元

=== 按人头均摊配送费和包装费 ===
每人配送费 = 5 ÷ 2 = 2.50元
  → 用户A配送费 = 2.50元
  → 用户B配送费 = 2.50元

每人包装费 = 3 ÷ 2 = 1.50元
  → 用户A包装费 = 1.50元
  → 用户B包装费 = 1.50元

=== 最终金额 ===
用户A应付 = 30.00（菜品）+ 2.50（配送）+ 1.50（包装）= 34.00元
用户B应付 = 20.00（菜品）+ 2.50（配送）+ 1.50（包装）= 24.00元

验证：34.00 + 24.00 = 58.00元 = 50.00（菜品）+ 8.00（公共费用）✓
```

**再加一个用户C（没点菜，只想凑单拿优惠）：**

```
菜品总金额 = 30 + 20 + 0 = 50元
参与人数 = 3人

每人配送费 = 5 ÷ 3 = 1.666... → 取整 166分
  余数 = 500 - 166×3 = 2分 → 由最后加入者(C)承担
  → A配送费 = 1.66元, B配送费 = 1.66元, C配送费 = 1.68元

每人包装费 = 3 ÷ 3 = 1.00元

=== 最终金额 ===
用户A应付 = 30.00 + 1.66 + 1.00 = 32.66元
用户B应付 = 20.00 + 1.66 + 1.00 = 22.66元
用户C应付 =  0.00 + 1.68 + 1.00 = 2.68元

验证：32.66 + 22.66 + 2.68 = 58.00元 ✓
```

**优惠券分摊示例（菜品8折优惠，省了10元）：**

```
优惠金额 = 10元
按菜品金额比例分摊：
  A菜品占比 = 30/50 = 60% → A分到优惠 = 10 × 60% = 6.00元
  B菜品占比 = 20/50 = 40% → B分到优惠 = 10 × 40% = 4.00元

用户A实付 = 30.00 - 6.00 + 1.66 + 1.00 = 26.66元
用户B实付 = 20.00 - 4.00 + 1.66 + 1.00 = 18.66元
用户C实付 =  0.00 - 0.00 + 1.68 + 1.00 = 2.68元

验证：26.66 + 18.66 + 2.68 = 48.00元 = 58.00 - 10.00 ✓
```

### 4.3 分摊算法实现

```java
public class FeeSplitCalculator {

    /**
     * 计算拼单费用分摊
     * @param items 各用户选择的菜品
     * @param deliveryFee 配送费（分）
     * @param packagingFee 包装费（分）
     * @param couponDiscount 优惠券减免（分）
     * @return 各用户应付明细
     */
    public List<UserSettlement> calculate(
            List<ParticipantItems> items,
            long deliveryFee,
            long packagingFee,
            long couponDiscount) {

        int memberCount = items.size();
        long totalFoodAmount = items.stream()
                .mapToLong(ParticipantItems::getSubtotal)
                .sum();

        // 1. 按人头均摊配送费
        long[] deliveryShares = splitEvenly(deliveryFee, memberCount);

        // 2. 按人头均摊包装费
        long[] packagingShares = splitEvenly(packagingFee, memberCount);

        // 3. 按菜品金额比例分摊优惠券
        long[] couponShares = splitByRatio(couponDiscount, items);

        // 4. 汇总每人应付
        List<UserSettlement> result = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) {
            long foodAmount = items.get(i).getSubtotal();
            long total = foodAmount - couponShares[i]
                       + deliveryShares[i]
                       + packagingShares[i];
            result.add(UserSettlement.builder()
                    .userId(items.get(i).getUserId())
                    .foodAmount(foodAmount)
                    .deliveryShare(deliveryShares[i])
                    .packagingShare(packagingShares[i])
                    .couponShare(couponShares[i])
                    .totalAmount(Math.max(total, 0))
                    .build());
        }
        return result;
    }

    /**
     * 按人头均分，余数由最后一人承担
     */
    private long[] splitEvenly(long totalAmount, int count) {
        long[] shares = new long[count];
        long each = totalAmount / count;
        long remainder = totalAmount - each * count;
        Arrays.fill(shares, each);
        shares[count - 1] += remainder; // 余数给最后一人
        return shares;
    }

    /**
     * 按菜品金额比例分摊，使用最大余数法避免精度丢失
     */
    private long[] splitByRatio(long totalAmount, List<ParticipantItems> items) {
        long totalFood = items.stream().mapToLong(ParticipantItems::getSubtotal).sum();
        if (totalFood == 0) return new long[items.size()];

        int count = items.size();
        long[] shares = new long[count];
        long allocated = 0;

        for (int i = 0; i < count; i++) {
            // 使用 BigDecimal 精确计算比例
            BigDecimal ratio = BigDecimal.valueOf(items.get(i).getSubtotal())
                    .divide(BigDecimal.valueOf(totalFood), 10, RoundingMode.FLOOR);
            shares[i] = ratio.multiply(BigDecimal.valueOf(totalAmount))
                    .setScale(0, RoundingMode.FLOOR).longValue();
            allocated += shares[i];
        }
        // 余数给金额最大者
        long remainder = totalAmount - allocated;
        if (remainder > 0) {
            int maxIdx = 0;
            for (int i = 1; i < count; i++) {
                if (items.get(i).getSubtotal() > items.get(maxIdx).getSubtotal()) {
                    maxIdx = i;
                }
            }
            shares[maxIdx] += remainder;
        }
        return shares;
    }
}
```

---

## 5. 数据库设计

### 5.1 用户服务（user_db）

#### users — 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | VARCHAR(20) | UNIQUE, NOT NULL | 业务用户ID（如U2026052200001） |
| phone | VARCHAR(20) | UNIQUE, NOT NULL | 手机号 |
| nickname | VARCHAR(50) | NOT NULL | 昵称 |
| avatar_url | VARCHAR(255) | | 头像URL |
| status | TINYINT | NOT NULL DEFAULT 1 | 状态：1-正常 0-禁用 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：** `uk_user_id` (user_id), `uk_phone` (phone)

#### user_addresses — 用户地址表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| address_id | VARCHAR(20) | UNIQUE, NOT NULL | 业务地址ID |
| user_id | VARCHAR(20) | NOT NULL, FK→users.user_id | 用户ID |
| label | VARCHAR(20) | | 标签（家/公司/学校） |
| detail_address | VARCHAR(200) | NOT NULL | 详细地址 |
| contact_name | VARCHAR(20) | NOT NULL | 联系人 |
| contact_phone | VARCHAR(20) | NOT NULL | 联系电话 |
| is_default | TINYINT | NOT NULL DEFAULT 0 | 是否默认地址 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：** `idx_user_id` (user_id), `uk_address_id` (address_id)

---

### 5.2 拼单服务（pool_db）

#### pools — 拼单主表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| pool_id | VARCHAR(20) | UNIQUE, NOT NULL | 业务拼单ID |
| invite_code | VARCHAR(8) | UNIQUE, NOT NULL | 邀请码 |
| creator_id | VARCHAR(20) | NOT NULL | 发起者用户ID |
| merchant_id | VARCHAR(20) | NOT NULL | 商家ID |
| merchant_name | VARCHAR(100) | NOT NULL | 商家名称 |
| status | VARCHAR(15) | NOT NULL | 状态：CREATED/FORMING/FORMED/ORDERED/DISSOLVED |
| min_members | INT | NOT NULL DEFAULT 2 | 成团最少人数 |
| min_amount | BIGINT | NOT NULL DEFAULT 0 | 成团最低金额（分） |
| deadline_minutes | INT | NOT NULL DEFAULT 30 | 拼单有效时长（分钟） |
| current_members | INT | NOT NULL DEFAULT 0 | 当前参与人数 |
| current_food_amount | BIGINT | NOT NULL DEFAULT 0 | 当前菜品总金额（分） |
| order_id | VARCHAR(20) | | 成团后生成的订单ID |
| delivery_fee | BIGINT | DEFAULT 0 | 配送费（分） |
| packaging_fee | BIGINT | DEFAULT 0 | 包装费（分） |
| remark | VARCHAR(500) | | 备注 |
| expires_at | DATETIME | NOT NULL | 过期时间 |
| formed_at | DATETIME | | 成团时间 |
| dissolved_at | DATETIME | | 解散时间 |
| dissolve_reason | VARCHAR(50) | | 解散原因 |
| version | INT | NOT NULL DEFAULT 0 | 乐观锁版本号 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：** `uk_pool_id`, `uk_invite_code`, `idx_creator_id`, `idx_status_expires` (status, expires_at)

#### pool_participants — 拼单参与者表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| participant_id | VARCHAR(20) | UNIQUE, NOT NULL | 参与记录ID |
| pool_id | VARCHAR(20) | NOT NULL, FK→pools.pool_id | 拼单ID |
| user_id | VARCHAR(20) | NOT NULL | 用户ID |
| role | VARCHAR(10) | NOT NULL | 角色：CREATOR/MEMBER |
| address_id | VARCHAR(20) | NOT NULL | 该用户收货地址ID |
| food_amount | BIGINT | NOT NULL DEFAULT 0 | 个人菜品金额（分） |
| status | VARCHAR(10) | NOT NULL DEFAULT 'ACTIVE' | 状态：ACTIVE/LEFT/KICKED |
| remark | VARCHAR(200) | | 个人备注 |
| joined_at | DATETIME | NOT NULL | 加入时间 |
| left_at | DATETIME | | 退出时间 |

**索引：** `uk_participant_id`, `idx_pool_id` (pool_id), `uk_pool_user` (pool_id, user_id) — 同一拼单内用户唯一

#### pool_items — 拼单菜品明细表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| pool_id | VARCHAR(20) | NOT NULL | 拼单ID |
| participant_id | VARCHAR(20) | NOT NULL | 参与者ID |
| user_id | VARCHAR(20) | NOT NULL | 用户ID |
| item_id | VARCHAR(20) | NOT NULL | 菜品ID |
| item_name | VARCHAR(100) | NOT NULL | 菜品名称 |
| unit_price | BIGINT | NOT NULL | 单价（分） |
| quantity | INT | NOT NULL | 数量 |
| total_price | BIGINT | NOT NULL | 小计（分） |
| created_at | DATETIME | NOT NULL | 创建时间 |

**索引：** `idx_pool_id`, `idx_participant_id`

---

### 5.3 订单服务（order_db）

#### orders — 订单主表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_id | VARCHAR(20) | UNIQUE, NOT NULL | 业务订单ID |
| pool_id | VARCHAR(20) | UNIQUE, NOT NULL | 关联拼单ID |
| merchant_id | VARCHAR(20) | NOT NULL | 商家ID |
| merchant_name | VARCHAR(100) | NOT NULL | 商家名称 |
| status | VARCHAR(15) | NOT NULL | PENDING_CONFIRM/CONFIRMED/DELIVERING/COMPLETED/CANCELLED |
| total_food_amount | BIGINT | NOT NULL | 菜品总额（分） |
| delivery_fee | BIGINT | NOT NULL | 配送费（分） |
| packaging_fee | BIGINT | NOT NULL | 包装费（分） |
| coupon_discount | BIGINT | NOT NULL DEFAULT 0 | 优惠减免（分） |
| total_amount | BIGINT | NOT NULL | 订单总额（分） |
| member_count | INT | NOT NULL | 参与人数 |
| delivery_address | VARCHAR(500) | | 配送地址（取发起者地址） |
| estimated_arrival | DATETIME | | 预计送达时间 |
| paid_at | DATETIME | | 支付时间 |
| confirmed_at | DATETIME | | 商家接单时间 |
| completed_at | DATETIME | | 完成时间 |
| cancelled_at | DATETIME | | 取消时间 |
| cancel_reason | VARCHAR(200) | | 取消原因 |
| external_order_id | VARCHAR(50) | | 外部商家/平台订单ID |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：** `uk_order_id`, `uk_pool_id`, `idx_status`, `idx_merchant_id`

#### order_items — 订单菜品汇总表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_id | VARCHAR(20) | NOT NULL | 订单ID |
| item_id | VARCHAR(20) | NOT NULL | 菜品ID |
| item_name | VARCHAR(100) | NOT NULL | 菜品名称 |
| unit_price | BIGINT | NOT NULL | 单价（分） |
| total_quantity | INT | NOT NULL | 总数量 |
| total_price | BIGINT | NOT NULL | 总价（分） |
| created_at | DATETIME | NOT NULL | 创建时间 |

**索引：** `idx_order_id`

#### order_participant_settlements — 订单分摊明细表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_id | VARCHAR(20) | NOT NULL | 订单ID |
| user_id | VARCHAR(20) | NOT NULL | 用户ID |
| food_amount | BIGINT | NOT NULL | 菜品费用（分） |
| delivery_share | BIGINT | NOT NULL | 配送费分摊（分） |
| packaging_share | BIGINT | NOT NULL | 包装费分摊（分） |
| coupon_share | BIGINT | NOT NULL DEFAULT 0 | 优惠券分摊（分） |
| total_amount | BIGINT | NOT NULL | 实付总额（分） |
| pay_status | VARCHAR(10) | NOT NULL | 支付状态：PENDING/PAID/REFUNDED |
| created_at | DATETIME | NOT NULL | 创建时间 |

**索引：** `idx_order_id`, `uk_order_user` (order_id, user_id)

---

### 5.4 支付服务（pay_db）

#### payment_batches — 支付批次表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| batch_id | VARCHAR(20) | UNIQUE, NOT NULL | 批次ID |
| pool_id | VARCHAR(20) | NOT NULL | 拼单ID |
| order_id | VARCHAR(20) | NOT NULL | 订单ID |
| batch_type | VARCHAR(10) | NOT NULL | CHARGE/REFUND |
| total_amount | BIGINT | NOT NULL | 批次总金额（分） |
| status | VARCHAR(15) | NOT NULL | PROCESSING/SUCCESS/PARTIAL_SUCCESS/FAILED |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：** `uk_batch_id`, `idx_pool_id`, `idx_order_id`

#### payment_records — 支付记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| record_id | VARCHAR(20) | UNIQUE, NOT NULL | 记录ID |
| batch_id | VARCHAR(20) | NOT NULL | 批次ID |
| user_id | VARCHAR(20) | NOT NULL | 用户ID |
| pool_id | VARCHAR(20) | NOT NULL | 拼单ID |
| order_id | VARCHAR(20) | | 订单ID |
| type | VARCHAR(10) | NOT NULL | 类型：CHARGE/REFUND |
| amount | BIGINT | NOT NULL | 金额（分） |
| food_amount | BIGINT | NOT NULL DEFAULT 0 | 菜品分摊（分） |
| delivery_share | BIGINT | NOT NULL DEFAULT 0 | 配送费分摊（分） |
| packaging_share | BIGINT | NOT NULL DEFAULT 0 | 包装费分摊（分） |
| coupon_share | BIGINT | NOT NULL DEFAULT 0 | 优惠券分摊（分） |
| pay_channel | VARCHAR(20) | | 支付渠道：WECHAT/ALIPAY/BALANCE |
| channel_trade_no | VARCHAR(64) | | 第三方交易流水号 |
| status | VARCHAR(10) | NOT NULL | PENDING/SUCCESS/FAILED |
| fail_reason | VARCHAR(200) | | 失败原因 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：** `uk_record_id`, `idx_batch_id`, `idx_user_id`, `idx_pool_id`

---

## 6. 技术选型建议

### 6.1 选型总览

| 组件 | 选型 | 备选 | 理由 |
|------|------|------|------|
| 后端框架 | Spring Boot 3.x + Spring Cloud | — | Java生态成熟，微服务支持完善，团队学习成本低 |
| 注册中心 | Nacos 2.x | Eureka, Consul | 同时支持服务注册和配置管理，阿里开源社区活跃，国内使用广泛 |
| 配置中心 | Nacos（与注册中心合一） | Apollo | 与注册中心统一选型减少运维成本，支持灰度发布 |
| 网关 | Spring Cloud Gateway | — | 原生Spring生态，支持动态路由、限流、鉴权 |
| 服务调用 | OpenFeign + Sentinel | — | 声明式HTTP客户端，集成负载均衡和熔断 |
| 消息队列 | RocketMQ 5.x | Kafka | 事务消息支持好（关键），延迟消息用于拼单超时，国内生态强 |
| 数据库 | MySQL 8.0 | PostgreSQL | 事务支持成熟，分库分表方案丰富（ShardingSphere） |
| 缓存 | Redis 7.x (Redisson) | — | 拼单状态缓存、分布式锁（成团判定）、会话管理 |
| 分布式事务 | Seata AT模式 | — | 对业务侵入小，与Spring Cloud生态集成好 |
| 限流熔断 | Sentinel | Hystrix/Sentinel | 阿里开源，与Nacos集成好，支持多维度限流 |
| 链路追踪 | SkyWalking | Zipkin, Jaeger | 无侵入式探针，支持多种中间件追踪 |
| 容器化 | Docker + Kubernetes | — | 微服务标准部署方案 |
| CI/CD | Jenkins + GitLab CI | — | 团队熟悉度高 |

### 6.2 关键选型说明

**为什么选RocketMQ而不是Kafka：**

- 拼单成团扣款需要**事务消息**保证"成团状态更新"和"扣款消息发送"的原子性，RocketMQ原生支持事务消息
- 拼单超时解散需要**延迟/定时消息**，RocketMQ支持丰富的延迟级别
- 外卖场景属于业务消息而非大数据流处理，RocketMQ的语义更贴合

**为什么选Seata AT模式：**

- 成团扣款涉及"拼单服务更新状态 + 支付服务扣款 + 订单服务生成订单"三个服务的数据一致性
- AT模式基于undo log自动补偿，对业务代码侵入最小（只需加`@GlobalTransactional`注解）
- TCC模式虽然性能更好但改造成本高，外卖拼单的并发量可承受AT模式的开销

**为什么选Nacos合一方案：**

- 减少运维组件数量，统一服务发现和配置管理
- 支持配置变更实时推送，成团规则等参数可动态调整

---

## 7. 关键难点与解决方案

### 7.1 成团判定的并发问题

**问题：** 多个用户同时加入拼单，最后一席的竞争可能导致重复成团或漏判。

**场景：** 拼单需要3人，当前2人，用户B和用户C同时调用加入接口。

**解决方案：分布式锁 + 乐观锁双重保障**

```
用户B请求 ──→ 获取Redis分布式锁(pool:P2026xxx) ──→ 持锁
用户C请求 ──→ 获取Redis分布式锁(pool:P2026xxx) ──→ 等待...

持锁后执行：
  1. SELECT * FROM pools WHERE pool_id = ? FOR UPDATE  (数据库行锁)
  2. 检查状态是否为 FORMING
  3. 插入参与者记录
  4. UPDATE pools SET current_members = current_members + 1, version = version + 1
     WHERE pool_id = ? AND version = ?   (CAS乐观锁)
  5. 检查是否满足成团条件
  6. 如满足：UPDATE pools SET status = 'FORMED' WHERE pool_id = ? AND version = ?
  7. 发布成团事件

释放锁
```

**Redis分布式锁实现要点：**

- 锁粒度：每个拼单ID一把锁（`lock:pool:{poolId}`）
- 锁超时：5秒（防死锁）
- 等待超时：3秒（防长时间阻塞）
- 使用Redisson的`RLock`，支持看门狗自动续期

**兜底：定时对账任务**

- 每分钟扫描FORMING状态的拼单
- 校验`current_members`与实际参与者表记录数是否一致
- 校验是否已满足成团条件但因并发问题未触发
- 不一致时修复状态并补发成团事件

### 7.2 拼单解散时的预付款退款

**问题：** 用户已支付但拼单解散，需要可靠退款。

**解决方案：基于消息队列的可靠退款**

```
拼单解散流程：
  1. 拼单服务将状态更新为 DISSOLVED
  2. 发布 pool.dissolved 事件到 MQ（带所有参与者信息）
  3. 支付服务消费事件：
     a. 查询该拼单下所有已扣款的支付记录
     b. 对每笔扣款生成退款申请
     c. 调用第三方支付退款API
     d. 更新退款状态
  4. 如退款失败：
     a. MQ自动重试（最多5次，指数退避）
     b. 超过重试次数 → 进入死信队列 → 告警 → 人工处理
     c. 记录退款失败明细，确保不丢不漏
```

**退款保障机制：**

| 机制 | 说明 |
|------|------|
| MQ持久化 | 消息持久化到磁盘，服务重启不丢失 |
| 消费确认 | 支付服务处理完成后才ACK，失败则重新投递 |
| 幂等设计 | 每笔退款带唯一`refundRequestId`，第三方重复请求不重复退款 |
| 对账补偿 | 每日凌晨与支付渠道对账，发现差异自动修复 |
| 人工兜底 | 超时未处理的退款生成工单，运营人工介入 |

### 7.3 分布式事务（跨服务创建订单+扣款）

**问题：** 成团时需要同时完成：拼单状态变更 → 订单生成 → 用户扣款，三个服务的数据必须一致。

**解决方案：Seata AT模式 + 事务消息兜底**

**主方案 — Seata AT模式：**

```
@GlobalTransactional(name = "pool-form-transaction", rollbackFor = Exception.class)
public void handlePoolFormed(String poolId) {
    // 1. 拼单服务：更新状态为 FORMED
    poolService.markAsFormed(poolId);

    // 2. 订单服务：生成订单
    String orderId = orderService.createOrderFromPool(poolId);

    // 3. 支付服务：对每个参与者扣款
    paymentService.chargeParticipants(poolId, orderId);
}
```

Seata AT模式原理：
- 在每个服务的本地事务前后拦截SQL，自动生成undo log
- 如果任一服务失败，TC（事务协调器）驱动所有参与者回滚
- 全程对业务代码侵入极小

**兜底方案 — 最终一致性：**

即使Seata事务失败，也需要兜底机制保证数据最终一致：

```
Seata事务失败后的处理：
  1. 捕获异常，记录事务失败日志（pool_id, 失败原因, 各步骤完成状态）
  2. 将拼单状态回退为 FORMING（允许用户重新触发成团）
  3. 如订单已生成但扣款失败 → 取消订单
  4. 如部分用户扣款成功 → 执行退款
  5. 告警通知运维人员
```

**降级策略：**

| 故障场景 | 降级处理 |
|---------|---------|
| 用户服务不可用 | 跳过地址校验，使用拼单表中的缓存地址 |
| 支付服务不可用 | 成团状态标记为"待支付"，15分钟后自动重试 |
| 订单服务不可用 | 成团状态保留，消息队列重试生成订单 |
| MQ不可用 | 同步调用降级为本地事件表，定时扫描补偿 |
| 数据库连接池耗尽 | 拒绝新请求，返回"系统繁忙"，已有请求继续处理 |

---

## 附录

### A. 非功能需求

| 指标 | 要求 |
|------|------|
| 可用性 | 99.9%（全年停机 < 8.76小时） |
| API响应时间 | P99 < 500ms |
| 成团判定延迟 | 从最后一个人加入到成团通知 < 3秒 |
| 退款时效 | 拼单解散后24小时内完成退款 |
| 数据持久化 | 核心交易数据RPO=0（零丢失） |

### B. 部署架构

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
  user    pool        order       payment    ...
  ×2      ×3          ×2          ×3
    │        │            │            │
    ▼        ▼            ▼            ▼
  MySQL    MySQL       MySQL       MySQL
  主从     主从        主从        主从

  ┌──────────────────────────────────────┐
  │     Nacos集群(3节点) + RocketMQ集群    │
  │     Redis集群(3主3从) + Seata TC      │
  └──────────────────────────────────────┘
```

### C. API统一响应格式

所有API统一使用以下响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "traceId": "abc123def456"
}
```

| code | 含义 |
|------|------|
| 0 | 成功 |
| 4001 | 参数校验失败 |
| 4003 | 无权限 |
| 4004 | 资源不存在 |
| 4009 | 拼单已满/已成团/已解散 |
| 5000 | 服务内部错误 |
| 5001 | 下游服务调用失败 |
| 5003 | 支付失败 |
