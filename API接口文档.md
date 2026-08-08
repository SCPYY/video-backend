# 海外短剧+影游独立站 — API 接口文档 (v1.0)

---

## 通用说明

### 基础URL

| 环境 | 地址 |
|:---|:---|
| 开发环境 | `http://localhost:8080` |
| 生产环境 | `https://your-domain.com` |

### 统一响应格式

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { },
  "timestamp": 1704067200000
}
```

### 状态码

| code | 含义 |
|:---|:---|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 / Token 失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 业务冲突（如重复购买） |
| 500 | 服务器内部错误 |

### 认证方式

- 认证接口需在请求头携带：`Authorization: Bearer <accessToken>`
- AccessToken 有效期 **2小时**，过期后使用 RefreshToken 刷新
- RefreshToken 有效期 **7天**

---

## 接口清单总览

| # | 模块 | Method | Endpoint | 认证 | 状态 |
|:---|:---|:---|:---|:---|:---|
| 1 | 用户 | POST | `/api/v1/auth/register` | 否 | ✅ 已开发 |
| 2 | 用户 | POST | `/api/v1/auth/login` | 否 | ✅ 已开发 |
| 3 | 用户 | POST | `/api/v1/auth/refresh` | 是 | ✅ 已开发 |
| 4 | 用户 | POST | `/api/v1/auth/logout` | 是 | ✅ 已开发 |
| 5 | 用户 | GET | `/api/v1/user/profile` | 是 | ✅ 已开发 |
| 6 | 用户 | PUT | `/api/v1/user/profile` | 是 | ✅ 已开发 |
| 7 | 内容 | GET | `/api/v1/contents` | 否 | ⬜ 待开发 |
| 8 | 内容 | GET | `/api/v1/contents/hot` | 否 | ⬜ 待开发 |
| 9 | 内容 | GET | `/api/v1/contents/{id}` | 否 | ⬜ 待开发 |
| 10 | 内容 | GET | `/api/v1/contents/{id}/episodes` | 否 | ⬜ 待开发 |
| 11 | 内容 | GET | `/api/v1/episodes/{id}/play` | 是 | ⬜ 待开发 |
| 12 | 商品 | GET | `/api/v1/products` | 否 | ⬜ 待开发 |
| 13 | 商品 | GET | `/api/v1/products/{id}` | 否 | ⬜ 待开发 |
| 14 | 订单 | POST | `/api/v1/orders` | 是 | ⬜ 待开发 |
| 15 | 订单 | GET | `/api/v1/orders` | 是 | ⬜ 待开发 |
| 16 | 订单 | GET | `/api/v1/orders/{id}` | 是 | ⬜ 待开发 |
| 17 | 订单 | POST | `/api/v1/orders/{id}/cancel` | 是 | ⬜ 待开发 |
| 18 | 支付 | POST | `/api/v1/payment/webhook/{gateway}` | 否(签名) | ⬜ 待开发 |
| 19 | 支付 | GET | `/api/v1/payment/status/{orderNo}` | 是 | ⬜ 待开发 |
| 20 | 权益 | GET | `/api/v1/entitlements/check` | 是 | ⬜ 待开发 |
| 21 | 权益 | GET | `/api/v1/entitlements/list` | 是 | ⬜ 待开发 |
| 22 | 评论 | POST | `/api/v1/comments` | 是 | ⬜ 待开发 |
| 23 | 评论 | GET | `/api/v1/comments` | 否 | ⬜ 待开发 |
| 24 | 评论 | GET | `/api/v1/comments/{id}/replies` | 否 | ⬜ 待开发 |
| 25 | 评论 | PUT | `/api/v1/comments/{id}` | 是 | ⬜ 待开发 |
| 26 | 评论 | DELETE | `/api/v1/comments/{id}` | 是 | ⬜ 待开发 |
| 27 | 评论 | POST | `/api/v1/comments/{id}/like` | 是 | ⬜ 待开发 |
| 28 | 评论 | POST | `/api/v1/comments/{id}/dislike` | 是 | ⬜ 待开发 |
| 29 | 弹幕 | POST | `/api/v1/danmaku` | 是 | ⬜ 待开发 |
| 30 | 弹幕 | GET | `/api/v1/danmaku` | 否 | ⬜ 待开发 |
| 31 | 弹幕 | POST | `/api/v1/danmaku/{id}/like` | 是 | ⬜ 待开发 |
| 32 | 弹幕 | DELETE | `/api/v1/danmaku/{id}` | 是 | ⬜ 待开发 |
| 33 | 管理-上传 | POST | `/admin/api/v1/upload/image` | 是(ADMIN/EDITOR) | ⬜ 待开发 |
| 34 | 管理-上传 | POST | `/admin/api/v1/upload/video` | 是(ADMIN/EDITOR) | ⬜ 待开发 |
| 35 | 管理-剧集 | POST | `/admin/api/v1/episodes/batch` | 是(ADMIN/EDITOR) | ⬜ 待开发 |
| 36 | 管理-剧集 | PUT | `/admin/api/v1/episodes/sort` | 是(ADMIN/EDITOR) | ⬜ 待开发 |

---

## 一、用户模块 (✅ 已开发)

### 1.1 用户注册

```
POST /api/v1/auth/register
```

**Request Body:**
```json
{
  "username": "testuser",
  "password": "123456",
  "email": "test@example.com"
}
```

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| username | String | 是 | 用户名，3-64位 |
| password | String | 是 | 密码，6-32位 |
| email | String | 否 | 邮箱 |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "timestamp": 1704067200000
}
```

**Error:**
| code | msg |
|:---|:---|
| 20002 | 用户名已存在 |
| 20003 | 邮箱已注册 |

---

### 1.2 用户登录

```
POST /api/v1/auth/login
```

**Request Body:**
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "expiresIn": 7200000,
    "tokenType": "Bearer"
  }
}
```

| 字段 | 说明 |
|:---|:---|
| accessToken | 访问令牌（2h有效） |
| refreshToken | 刷新令牌（7d有效） |
| expiresIn | 过期时间（毫秒） |

**Error:**
| code | msg |
|:---|:---|
| 20001 | 用户不存在 |
| 20004 | 密码错误 |
| 20005 | 账号已被禁用 |
| 20008 | 登录尝试次数过多 |

---

### 1.3 刷新Token

```
POST /api/v1/auth/refresh?refreshToken={refreshToken}
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "expiresIn": 7200000,
    "tokenType": "Bearer"
  }
}
```

---

### 1.4 退出登录

```
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "timestamp": 1704067200000
}
```

---

### 1.5 获取个人信息

```
GET /api/v1/user/profile
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "phone": null,
    "nickname": null,
    "avatarUrl": null,
    "status": 0,
    "lastLoginTime": "2026-08-07T18:07:19",
    "createdAt": "2026-08-07T18:07:15"
  }
}
```

---

### 1.6 更新个人信息

```
PUT /api/v1/user/profile
Authorization: Bearer <accessToken>
```

**Request Body:**
```json
{
  "nickname": "新昵称",
  "phone": "13800138000",
  "avatarUrl": "https://cdn.example.com/avatars/1.png"
}
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "timestamp": 1704067200000
}
```

---

## 二、内容模块 (⬜ 待开发)

### 2.1 内容列表

```
GET /api/v1/contents?page=1&size=10&type=1&category=&status=1&keyword=
```

**Query Parameters:**

| 参数 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 10 |
| type | int | 否 | 类型：1-短剧 2-影游 |
| category | String | 否 | 分类筛选 |
| status | int | 否 | 状态：0-下架 1-上架 |
| keyword | String | 否 | 搜索关键词（标题） |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 100,
    "page": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "type": 1,
        "title": "霸道总裁爱上我",
        "description": "一部浪漫的都市爱情短剧...",
        "coverUrl": "https://cdn.example.com/covers/1.jpg",
        "category": "爱情",
        "tags": "短剧,爱情,都市",
        "status": 1,
        "viewCount": 15000,
        "sortOrder": 100,
        "createdAt": "2026-08-01T10:00:00",
        "updatedAt": "2026-08-07T18:00:00"
      }
    ]
  }
}
```

---

### 2.2 热门内容

```
GET /api/v1/contents/hot?limit=8
```

| 参数 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| limit | int | 否 | 返回条数，默认 8 |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "type": 1,
      "title": "霸道总裁爱上我",
      "coverUrl": "https://cdn.example.com/covers/1.jpg",
      "category": "爱情",
      "viewCount": 15000
    }
  ]
}
```

> 缓存策略：Redis Hash 缓存 5 分钟，后台变更时主动失效。

---

### 2.3 内容详情

```
GET /api/v1/contents/{id}
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "type": 1,
    "title": "霸道总裁爱上我",
    "description": "一部浪漫的都市爱情短剧...",
    "coverUrl": "https://cdn.example.com/covers/1.jpg",
    "category": "爱情",
    "tags": "短剧,爱情,都市",
    "status": 1,
    "viewCount": 15000,
    "sortOrder": 100,
    "extras": {
      "director": "张三",
      "actor": ["李四", "王五"],
      "releaseYear": "2025",
      "trailerUrl": "https://cdn.example.com/trailers/1.mp4"
    },
    "createdAt": "2026-08-01T10:00:00",
    "updatedAt": "2026-08-07T18:00:00"
  }
}
```

> `extras` 字段来自 `content_extras` 表，以 key-value 形式返回所有扩展属性。

**Error:**
| code | msg |
|:---|:---|
| 30001 | 内容不存在 |

---

### 2.4 剧集列表

```
GET /api/v1/contents/{id}/episodes
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "episodeNumber": 1,
      "title": "第1集：命运的相遇",
      "duration": 180,
      "isFree": 1,
      "sortOrder": 1
    },
    {
      "id": 2,
      "episodeNumber": 2,
      "title": "第2集：误会重重",
      "duration": 195,
      "isFree": 0,
      "sortOrder": 2
    }
  ]
}
```

> 注意：列表不返回 `videoUrl`，需鉴权后通过播放接口获取。

---

### 2.5 获取播放信息（含鉴权）

```
GET /api/v1/episodes/{id}/play
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "contentId": 1,
    "episodeNumber": 1,
    "title": "第1集：命运的相遇",
    "videoUrl": "https://cdn.example.com/videos/1/ep1.m3u8",
    "duration": 180,
    "interactiveConfig": null,
    "isFree": 1,
    "hasAccess": true
  }
}
```

**Response - 无权限 (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 2,
    "contentId": 1,
    "episodeNumber": 2,
    "title": "第2集：误会重重",
    "duration": 195,
    "isFree": 0,
    "hasAccess": false,
    "productId": 1
  }
}
```

| 字段 | 说明 |
|:---|:---|
| hasAccess | 是否有播放权限 |
| productId | 无权限时，引导购买的商品ID |
| interactiveConfig | 影游的互动配置（JSON），短剧为 null |
| videoUrl | hasAccess=true 时返回CDN播放地址 |

**Error:**
| code | msg |
|:---|:---|
| 30002 | 剧集不存在 |
| 60001 | 无访问权限（付费内容） |

---

## 三、商品模块 (⬜ 待开发)

### 3.1 商品列表

```
GET /api/v1/products?type=&contentId=&status=1
```

| 参数 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| type | int | 否 | 类型：1-单集 2-全集 3-会员 |
| contentId | long | 否 | 关联内容ID |
| status | int | 否 | 0-下架 1-上架 |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "type": 1,
      "contentId": 1,
      "episodeId": 2,
      "name": "第2集解锁",
      "priceUsd": 0.99,
      "priceEur": 0.89,
      "status": 1
    },
    {
      "id": 2,
      "type": 2,
      "contentId": 1,
      "name": "全集解锁",
      "priceUsd": 4.99,
      "priceEur": 4.49,
      "status": 1
    },
    {
      "id": 3,
      "type": 3,
      "name": "月度会员",
      "priceUsd": 9.99,
      "priceEur": 8.99,
      "durationDays": 30,
      "status": 1
    }
  ]
}
```

---

### 3.2 商品详情

```
GET /api/v1/products/{id}
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 2,
    "type": 2,
    "contentId": 1,
    "name": "全集解锁",
    "priceUsd": 4.99,
    "priceEur": 4.49,
    "status": 1,
    "content": {
      "id": 1,
      "title": "霸道总裁爱上我",
      "coverUrl": "https://cdn.example.com/covers/1.jpg"
    }
  }
}
```

---

## 四、订单模块 (⬜ 待开发)

### 4.1 创建订单

```
POST /api/v1/orders
Authorization: Bearer <accessToken>
```

**Request Body:**
```json
{
  "productId": 2,
  "currency": "USD",
  "paymentMethod": "PAYPAL"
}
```

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| productId | Long | 是 | 商品ID |
| currency | String | 是 | 币种：USD / EUR |
| paymentMethod | String | 是 | 支付方式：PAYPAL / STRIPE |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "orderNo": "ORD202608071800001",
    "productId": 2,
    "productName": "全集解锁",
    "amount": 4.99,
    "currency": "USD",
    "status": 0,
    "paymentUrl": "https://www.paypal.com/checkout/...",
    "expiredAt": "2026-08-07T18:30:00",
    "createdAt": "2026-08-07T18:00:00"
  }
}
```

**Error:**
| code | msg |
|:---|:---|
| 40004 | 请勿重复下单 |
| 40003 | 订单已过期 |

> 订单创建后状态为 **待支付(0)**，30分钟内未支付自动过期取消。

---

### 4.2 用户订单列表

```
GET /api/v1/orders?page=1&size=10&status=
Authorization: Bearer <accessToken>
```

| 参数 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |
| status | int | 否 | 0-待支付 1-已支付 2-已取消 3-已退款 |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 5,
    "page": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "orderNo": "ORD202608071800001",
        "productName": "全集解锁",
        "amount": 4.99,
        "currency": "USD",
        "paymentMethod": "PAYPAL",
        "status": 1,
        "paidAt": "2026-08-07T18:05:00",
        "createdAt": "2026-08-07T18:00:00"
      }
    ]
  }
}
```

---

### 4.3 订单详情

```
GET /api/v1/orders/{id}
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "orderNo": "ORD202608071800001",
    "userId": 1,
    "productId": 2,
    "productName": "全集解锁",
    "amount": 4.99,
    "currency": "USD",
    "paymentMethod": "PAYPAL",
    "status": 1,
    "gatewayOrderId": "PAYPAL-XXXXX",
    "gatewayTxId": "TX-XXXXX",
    "paidAt": "2026-08-07T18:05:00",
    "expiredAt": "2026-08-07T18:30:00",
    "createdAt": "2026-08-07T18:00:00",
    "updatedAt": "2026-08-07T18:05:00"
  }
}
```

---

### 4.4 取消订单

```
POST /api/v1/orders/{id}/cancel
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "timestamp": 1704067200000
}
```

**Error:**
| code | msg |
|:---|:---|
| 40001 | 订单不存在 |
| 40002 | 订单无法取消（已支付/已退款） |

> 只有 **待支付(0)** 状态的订单可以取消。

---

## 五、支付模块 (⬜ 待开发)

### 5.1 支付网关回调 (Webhook)

```
POST /api/v1/payment/webhook/{gateway}
```

| 路径参数 | 说明 |
|:---|:---|
| gateway | paypal 或 stripe |

**PayPal Webhook 关键头:**
```
PayPal-Transmission-Id: xxx
PayPal-Transmission-Time: xxx
PayPal-Transmission-Sig: xxx
PayPal-Cert-Url: xxx
PayPal-Auth-Version: xxx
PayPal-Auth-Algo: xxx
```

**Stripe Webhook 关键头:**
```
Stripe-Signature: t=xxx,v1=xxx
```

**Request Body:** 原始 JSON（由支付网关定义）

**Response (200):**
```
OK
```

> ⚠️ **安全要点：**  
> 1. 必须验证网关签名，防止伪造回调  
> 2. 根据 `gateway_tx_id` 做幂等性判断，防止重复处理  
> 3. 签名验证（同步）通过后，业务处理应异步执行，避免阻塞网关响应  
> 4. 发放用户权益时使用分布式锁，防止并发重复发放

**处理流程:**
```
签名验证 → 幂等检查 → 更新订单状态 → 发放用户权益 → 返回200
```

---

### 5.2 查询支付状态

```
GET /api/v1/payment/status/{orderNo}
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "orderNo": "ORD202608071800001",
    "status": 1,
    "statusText": "已支付",
    "amount": 4.99,
    "currency": "USD",
    "paidAt": "2026-08-07T18:05:00"
  }
}
```

| status | statusText |
|:---|:---|
| 0 | 待支付 |
| 1 | 已支付 |
| 2 | 已取消 |
| 3 | 已退款 |

---

## 六、权益模块 (⬜ 待开发)

### 6.1 检查访问权限

```
GET /api/v1/entitlements/check?contentId=1&episodeId=2
Authorization: Bearer <accessToken>
```

| 参数 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| contentId | Long | 否 | 内容ID |
| episodeId | Long | 否 | 剧集ID（单集场景） |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "hasAccess": true,
    "entitlementType": 2,
    "expireTime": "2026-09-06T18:00:00"
  }
}
```

| 字段 | 说明 |
|:---|:---|
| hasAccess | 是否有访问权限 |
| entitlementType | 权益类型：1-内容解锁 2-会员 |
| expireTime | 到期时间（null=永久） |

**无权限:**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "hasAccess": false,
    "requiredProduct": {
      "id": 1,
      "name": "单集解锁-第2集",
      "priceUsd": 0.99
    }
  }
}
```

---

### 6.2 用户权益列表

```
GET /api/v1/entitlements/list
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "type": 1,
      "contentId": 1,
      "contentTitle": "霸道总裁爱上我",
      "episodeId": null,
      "expireTime": null,
      "createdAt": "2026-08-07T18:05:00"
    },
    {
      "id": 2,
      "type": 2,
      "contentId": null,
      "contentTitle": null,
      "expireTime": "2026-09-06T18:00:00",
      "createdAt": "2026-08-07T18:10:00"
    }
  ]
}
```

---

## 七、管理后台 API (⬜ 待开发)

> 管理后台接口需要管理员角色认证（`ROLE_ADMIN` 或 `ROLE_EDITOR`）。

### 7.1 内容管理

| Method | Endpoint | 权限 | 说明 |
|:---|:---|:---|:---|
| POST | `/api/v1/admin/contents` | ADMIN/EDITOR | 创建内容 |
| PUT | `/api/v1/admin/contents/{id}` | ADMIN/EDITOR | 更新内容 |
| DELETE | `/api/v1/admin/contents/{id}` | ADMIN | 删除内容（软删除） |
| GET | `/api/v1/admin/contents` | ADMIN/EDITOR/VIEWER | 管理端内容列表（分页+筛选） |
| GET | `/api/v1/admin/contents/{id}` | ADMIN/EDITOR/VIEWER | 内容详情 |
| PUT | `/api/v1/admin/contents/{id}/status` | ADMIN/EDITOR | 上下架切换 |

### 7.2 剧集管理

| Method | Endpoint | 权限 | 说明 |
|:---|:---|:---|:---|
| POST | `/api/v1/admin/episodes` | ADMIN/EDITOR | 添加单集/关卡 |
| PUT | `/api/v1/admin/episodes/{id}` | ADMIN/EDITOR | 更新单集/关卡信息 |
| DELETE | `/api/v1/admin/episodes/{id}` | ADMIN | 删除单集/关卡 |
| POST | `/api/v1/admin/episodes/batch` | ADMIN/EDITOR | 批量添加剧集 |
| PUT | `/api/v1/admin/episodes/sort` | ADMIN/EDITOR | 调整剧集播放顺序 |

### 7.3 文件上传

| Method | Endpoint | 权限 | 说明 |
|:---|:---|:---|:---|
| POST | `/admin/api/v1/upload/image` | ADMIN/EDITOR | 上传图片（封面/海报，最大10MB） |
| POST | `/admin/api/v1/upload/video` | ADMIN/EDITOR | 上传视频（剧集/预告片，最大2GB） |

### 7.5 商品管理

| Method | Endpoint | 权限 | 说明 |
|:---|:---|:---|:---|
| POST | `/api/v1/admin/products` | ADMIN/EDITOR | 创建商品 |
| PUT | `/api/v1/admin/products/{id}` | ADMIN/EDITOR | 更新商品 |
| DELETE | `/api/v1/admin/products/{id}` | ADMIN | 删除商品 |

### 7.6 订单管理

| Method | Endpoint | 权限 | 说明 |
|:---|:---|:---|:---|
| GET | `/api/v1/admin/orders` | ADMIN/EDITOR/VIEWER | 订单列表 |
| GET | `/api/v1/admin/orders/{id}` | ADMIN/EDITOR/VIEWER | 订单详情 |
| POST | `/api/v1/admin/orders/{id}/refund` | ADMIN | 退款 |

### 7.7 用户管理

| Method | Endpoint | 权限 | 说明 |
|:---|:---|:---|:---|
| GET | `/api/v1/admin/users` | ADMIN/EDITOR/VIEWER | 用户列表 |
| GET | `/api/v1/admin/users/{id}` | ADMIN/EDITOR/VIEWER | 用户详情 |
| PUT | `/api/v1/admin/users/{id}/status` | ADMIN | 禁用/启用用户 |

---

## 八、评论模块 (⬜ 待开发)

> 评论分为"整剧评论"（`episodeId` 为空）和"单集评论"（`episodeId` 有值），支持无限级嵌套回复。

### 8.1 发表评论/回复

```
POST /api/v1/comments
Authorization: Bearer <accessToken>
```

**Request Body:**
```json
{
  "contentId": 1,
  "episodeId": null,
  "parentId": null,
  "content": "这部剧太好看了！"
}
```

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| contentId | Long | 是 | 内容ID |
| episodeId | Long | 否 | 剧集ID，null=整剧评论 |
| parentId | Long | 否 | 父评论ID，null=一级评论 |
| content | String | 是 | 评论内容（最多500字） |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "contentId": 1,
    "episodeId": null,
    "parentId": 0,
    "rootId": 0,
    "userId": 1,
    "nickname": "testuser",
    "avatarUrl": null,
    "replyToNickname": null,
    "content": "这部剧太好看了！",
    "likeCount": 0,
    "dislikeCount": 0,
    "replyCount": 0,
    "liked": false,
    "disliked": false,
    "createdAt": "2026-08-07T18:00:00"
  }
}
```

**Error:**
| code | msg |
|:---|:---|
| 70001 | 评论包含敏感词 |
| 70002 | 评论过于频繁，请稍后再试 |

> 频率限制：1分钟最多5条。

---

### 8.2 获取评论列表

```
GET /api/v1/comments?contentId=1&episodeId=&page=1&size=10&sort=latest
```

| 参数 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| contentId | Long | 是 | 内容ID |
| episodeId | Long | 否 | 剧集ID，空=整剧评论 |
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认10 |
| sort | String | 否 | 排序：latest(最新)/hot(最热) |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 50,
    "page": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "contentId": 1,
        "episodeId": null,
        "parentId": 0,
        "rootId": 0,
        "userId": 1,
        "nickname": "testuser",
        "avatarUrl": null,
        "content": "这部剧太好看了！",
        "likeCount": 5,
        "dislikeCount": 0,
        "replyCount": 3,
        "liked": true,
        "disliked": false,
        "subReplies": [
          {
            "id": 2,
            "parentId": 1,
            "rootId": 1,
            "userId": 2,
            "nickname": "another_user",
            "avatarUrl": null,
            "replyToNickname": "testuser",
            "content": "同意！尤其是第三集",
            "likeCount": 2,
            "dislikeCount": 0,
            "replyCount": 0,
            "liked": false,
            "disliked": false,
            "createdAt": "2026-08-07T18:05:00"
          }
        ],
        "hasMoreSubReplies": true,
        "createdAt": "2026-08-07T18:00:00"
      }
    ]
  }
}
```

| 字段 | 说明 |
|:---|:---|
| subReplies | 前3条子回复（仅一级评论有） |
| hasMoreSubReplies | 是否有更多子回复（通过replies接口分页加载） |
| liked / disliked | 当前登录用户的点赞/点踩状态（未登录均为false） |

---

### 8.3 获取子回复列表

```
GET /api/v1/comments/{id}/replies?page=1&size=10
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 8,
    "page": 1,
    "size": 10,
    "records": [
      {
        "id": 2,
        "parentId": 1,
        "rootId": 1,
        "userId": 2,
        "nickname": "another_user",
        "avatarUrl": null,
        "replyToNickname": "testuser",
        "content": "同意！尤其是第三集",
        "likeCount": 2,
        "dislikeCount": 0,
        "replyCount": 0,
        "liked": false,
        "disliked": false,
        "createdAt": "2026-08-07T18:05:00"
      }
    ]
  }
}
```

---

### 8.4 编辑评论

```
PUT /api/v1/comments/{id}
Authorization: Bearer <accessToken>
```

**Request Body:**
```json
{
  "content": "修改后的评论内容"
}
```

> 仅限评论发布者本人操作。

---

### 8.5 删除评论

```
DELETE /api/v1/comments/{id}
Authorization: Bearer <accessToken>
```

> 评论发布者本人或管理员可删除。

---

### 8.6 点赞/取消点赞

```
POST /api/v1/comments/{id}/like
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "liked": true,
    "likeCount": 6
  }
}
```

> 幂等操作：已点赞→取消点赞，已点踩→切换为点赞。

---

### 8.7 点踩/取消点踩

```
POST /api/v1/comments/{id}/dislike
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "disliked": true,
    "dislikeCount": 1
  }
}
```

> 幂等操作：已点踩→取消点踩，已点赞→切换为点踩。

---

## 九、弹幕模块 (⬜ 待开发)

> 弹幕按视频时间点分段查询，使用 Redis 缓存（30秒过期）降低高频查询压力。

### 9.1 发送弹幕

```
POST /api/v1/danmaku
Authorization: Bearer <accessToken>
```

**Request Body:**
```json
{
  "episodeId": 1,
  "content": "前方高能！",
  "videoTime": 120,
  "color": "#FF0000",
  "position": 0,
  "fontSize": 2
}
```

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| episodeId | Long | 是 | 剧集ID |
| content | String | 是 | 弹幕内容（最多200字） |
| videoTime | int | 是 | 视频时间点（秒） |
| color | String | 否 | 颜色（十六进制），默认 #FFFFFF |
| position | int | 否 | 位置：0-滚动 1-顶部 2-底部 |
| fontSize | int | 否 | 字号：1-小 2-中 3-大，默认2 |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "episodeId": 1,
    "userId": 1,
    "content": "前方高能！",
    "videoTime": 120,
    "color": "#FF0000",
    "position": 0,
    "fontSize": 2,
    "likeCount": 0,
    "createdAt": "2026-08-07T18:00:00"
  }
}
```

> 频率限制：3条/10秒。

---

### 9.2 获取弹幕列表

```
GET /api/v1/danmaku?episodeId=1&startTime=0&endTime=30
```

| 参数 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| episodeId | Long | 是 | 剧集ID |
| startTime | int | 是 | 起始时间（秒） |
| endTime | int | 是 | 结束时间（秒） |

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "nickname": "testuser",
      "content": "前方高能！",
      "videoTime": 15,
      "color": "#FF0000",
      "position": 0,
      "fontSize": 2,
      "likeCount": 3,
      "liked": false,
      "createdAt": "2026-08-07T18:00:00"
    }
  ]
}
```

> 前端播放器每5秒请求一次，覆盖当前播放位置±15秒的时间段。  
> **缓存策略**：Redis String 缓存30秒，key格式 `danmaku:{episodeId}:{startTime}:{endTime}`。

---

### 9.3 弹幕点赞

```
POST /api/v1/danmaku/{id}/like
Authorization: Bearer <accessToken>
```

**Response (200):**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "liked": true,
    "likeCount": 4
  }
}
```

---

### 9.4 删除弹幕

```
DELETE /api/v1/danmaku/{id}
Authorization: Bearer <accessToken>
```

> 弹幕发布者本人或管理员可删除。

---

## 开发进度

| 模块 | 接口数 | 已开发 | 待开发 | 完成率 |
|:---|:---|:---|:---|:---|
| 用户模块 | 6 | 6 | 0 | 100% |
| 内容模块 | 5 | 0 | 5 | 0% |
| 商品模块 | 2 | 0 | 2 | 0% |
| 订单模块 | 4 | 0 | 4 | 0% |
| 支付模块 | 2 | 0 | 2 | 0% |
| 权益模块 | 2 | 0 | 2 | 0% |
| 管理后台 | 12 | 0 | 12 | 0% |
| 管理-上传 | 2 | 0 | 2 | 0% |
| 管理-剧集 | 2 | 0 | 2 | 0% |
| 评论模块 | 7 | 0 | 7 | 0% |
| 弹幕模块 | 4 | 0 | 4 | 0% |
| **合计** | **48** | **6** | **42** | **13%** |

---

**文档版本：** v1.0  
**最后更新：** 2026-08-07  
**API 在线文档：** http://localhost:8080/doc.html
