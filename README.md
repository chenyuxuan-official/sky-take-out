# 苍穹外卖 · sky-take-out

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.3-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-5.7%2B-blue)
![Redis](https://img.shields.io/badge/Redis-5%2B-red)
![Vue](https://img.shields.io/badge/Vue-2.6-42b883)
![uni--app](https://img.shields.io/badge/uni--app-微信小程序-07c160)

一套完整的外卖点餐系统，单体仓库（monorepo）：**Spring Boot 后端** + **Vue 管理端** + **uni-app 微信小程序**。

商家在后台维护商品、接单派送、查看营业数据；顾客在小程序里点餐、下单、支付、催单。项目源自「黑马程序员 · 苍穹外卖」课程，后端业务全部落地，管理端与小程序源码也一并收录，可跑通「点餐 → 下单 → 支付 → 来单提醒 → 接单派送 → 完成」的完整链路。

---

## 目录

- [项目简介](#项目简介)
- [功能总览](#功能总览)
- [技术栈](#技术栈)
- [仓库结构](#仓库结构)
- [架构与请求链路](#架构与请求链路)
- [核心设计](#核心设计)
- [前端页面地图](#前端页面地图)
- [接口速览](#接口速览)
- [数据库设计](#数据库设计)
- [快速开始](#快速开始)
- [部署拓扑](#部署拓扑)
- [常见问题](#常见问题)
- [已知限制与接口缺口](#已知限制与接口缺口)
- [开发日志](#开发日志)

## 项目简介

系统分管理端与用户端两套接口，用 JWT 区分身份：管理端令牌放请求头 `token`，用户端令牌放 `authentication`。

```
┌──────────────────────┐          ┌─────────────────────┐
│ 管理端 Vue (8888/81)  │──/api───►│  Nginx :81          │
└──────────────────────┘          │  /api → :8080/admin │
┌──────────────────────┐          │  /ws  → :8080/ws    │
│ 小程序 uni-app        │──直连──► └─────────┬───────────┘
└──────────────────────┘                    │
                                 ┌──────────▼──────────┐
                                 │  sky-server :8080   │
                                 │ 拦截器 → Service    │
                                 └──────┬───────┬──────┘
                                        │       │
                                   MySQL 5.7  Redis 5
```

- **管理端**：员工、分类、菜品、套餐、店铺营业状态、订单全流程、来单提醒/催单、工作台、数据统计、报表导出。
- **用户端**：微信登录、商品浏览、购物车、地址簿、下单、支付（模拟）、历史订单、取消、再来一单、催单。

## 功能总览

### 管理端 `/admin`

| 模块 | 能力 | 关键接口 |
|---|---|---|
| 员工管理 | 登录、新增、编辑、分页、启停用 | `POST /admin/employee/login`、`GET /admin/employee/page` |
| 分类管理 | 增删改查、启停用、按类型查询 | `/admin/category` |
| 菜品管理 | 新增（含口味）、修改、批量删除、起售停售、分页 | `/admin/dish` |
| 套餐管理 | 新增（含菜品关系）、修改、删除、起售停售 | `/admin/setmeal` |
| 店铺状态 | 营业 / 打烊（Redis `SHOP_STATUS`） | `PUT /admin/shop/{status}` |
| 订单管理 | 条件搜索、状态统计、详情、接单、拒单、派送、完成、取消 | `/admin/order` |
| 来单提醒 | WebSocket 推送新订单，前端弹窗 + 响铃 | `/ws/{sid}` |
| 客户催单 | 接收催单消息并提醒商家 | `/ws/{sid}` |
| 工作台 | 今日营业额、有效订单、完成率、客单价、新增用户；订单/菜品/套餐总览 | `/admin/workspace` |
| 数据统计 | 营业额、用户、订单趋势 + 销量 Top10（ECharts） | `/admin/report` |
| 报表导出 | 按模板导出近 30 天运营数据 Excel | `GET /admin/report/export` |

### 用户端 `/user`

| 模块 | 能力 | 关键接口 |
|---|---|---|
| 微信登录 | `code` 换 openid，自动注册并签发 token | `POST /user/user/login` |
| 商品浏览 | 分类、菜品（含口味）、套餐 | `/user/category`、`/user/dish`、`/user/setmeal` |
| 购物车 | 加购、减购、查看、清空 | `/user/shoppingCart` |
| 地址簿 | 增删改查、设默认、查默认 | `/user/addressBook` |
| 下单 | 校验地址、购物车、配送范围（≤5km） | `POST /user/order/submit` |
| 支付 | 模拟支付，成功后触发来单提醒 | `PUT /user/order/payment` |
| 订单 | 历史订单、详情、取消、再来一单、催单 | `/user/order` |
| 店铺信息 | 店铺名称、地址、电话（小程序首页展示） | `GET /user/shop/info` |

## 技术栈

### 后端

| 分类 | 技术 | 版本 | 用途 |
|---|---|---|---|
| 基础框架 | Spring Boot | 2.7.3 | Web、自动装配 |
| 持久层 | MyBatis-Spring-Boot-Starter | 2.2.0 | ORM |
| 数据库 | MySQL | 5.7+ | 业务数据 |
| 连接池 | Druid | 1.2.1 | 数据源 |
| 分页 | PageHelper | 1.3.0 | 分页查询 |
| 缓存 | Spring Data Redis + Spring Cache | — | 菜品/套餐/店铺状态 |
| 接口文档 | Knife4j（Swagger2） | 3.0.2 | `/doc.html` |
| 鉴权 | JJWT | 0.9.1 | 双端令牌 |
| 实时通信 | Spring WebSocket（JSR-356） | — | 来单提醒、催单 |
| 定时任务 | Spring Task | — | 超时取消、自动完成 |
| 报表 | Apache POI | 3.16 | Excel 模板填充 |
| JSON | Fastjson | 1.2.76 | 序列化 |
| 对象存储 | 阿里云 OSS SDK | 3.10.2 | 图片上传 |
| 其他 | Lombok / AspectJ / commons-lang | — | 简化代码、AOP |

### 前端

| 端 | 技术 | 说明 |
|---|---|---|
| 管理端 `frontend/admin` | Vue 2.6.10 + TypeScript 3.6 + Element UI 2.12 + Vue CLI 3.11 + ECharts 5.3 + Vuex 3 + axios 0.19 | 基于 vue-typescript-admin-template，开发端口 8888 |
| 小程序 `frontend/miniapp` | uni-app（Vue 2 语法）+ uni-ui | HBuilderX 编译到微信开发者工具 |

### 运行环境

| 组件 | 版本 |
|---|---|
| JDK | 8+（本项目在 17 下编译运行） |
| Node | 16+ |
| MySQL | 5.7+ |
| Redis | 5+ |
| Nginx | 1.20+（托管管理端 + 反向代理） |

## 仓库结构

```
sky-take-out
├── pom.xml                      # 聚合工程，统一依赖版本
├── sky-common                   # 常量/上下文/异常/结果封装/JWT/工具类/OSS
├── sky-pojo                     # entity(11) / dto / vo
├── sky-server                   # 业务模块
│   └── src/main
│       ├── java/com/sky
│       │   ├── SkyApplication.java        # @EnableScheduling / @EnableCaching
│       │   ├── annotation / aspect        # @AutoFill 与公共字段填充切面
│       │   ├── config                     # WebMvc、Redis、OSS、WebSocket
│       │   ├── controller/admin(9)        # 管理端接口
│       │   ├── controller/user(8)         # 用户端接口
│       │   ├── handler                    # 全局异常
│       │   ├── interceptor                # JWT 双端拦截器
│       │   ├── mapper(11)                 # MyBatis 接口
│       │   ├── service / service/impl     # 业务层
│       │   ├── task                       # OrderTask 定时任务
│       │   └── websocket                  # WebSocketServer
│       └── resources
│           ├── mapper/*.xml               # 11 个映射文件
│           ├── template/                  # 运营数据报表模板.xlsx
│           └── application.yml            # 占位符配置
└── frontend
    ├── admin                    # 管理端源码（Vue2 + TS）
    └── miniapp                  # 小程序源码（uni-app）
```

> `application-dev.yml` 存放数据库、Redis、OSS、微信等真实凭据，已在 `.gitignore` 中忽略，不随仓库分发。
> `frontend/*/node_modules`、`frontend/admin/dist`、`frontend/miniapp/unpackage` 都是本地产物（依赖与编译结果），同样被忽略，仓库里只保留源码。

## 架构与请求链路

**一次管理端请求**：

```
浏览器 → Nginx:81  /api/employee/page
       → 去掉 /api 前缀，转发 http://127.0.0.1:8080/admin/employee/page
       → JwtTokenAdminInterceptor 校验 token，把 empId 写入 BaseContext
       → EmployeeController → EmployeeServiceImpl → EmployeeMapper → MySQL
       → Result<T> 统一包装
```

**一次来单提醒**：

```
用户支付成功 → OrderServiceImpl.paySuccess()
            → CAS 更新订单为「待接单」
            → WebSocketServer.sendToAllClient({"type":1,"orderId":..,"content":"订单号：.."})
            → 管理端 Navbar 收到消息，弹窗 + 播放提示音
```

## 核心设计

### 1. 双端 JWT 鉴权
- 管理端令牌头 `token`、用户端 `authentication`（配置在 `sky.jwt.*`）；
- 两个拦截器分别拦截 `/admin/**`、`/user/**`，放行登录与店铺状态接口；
- 解析出的 id 存入 `BaseContext`（ThreadLocal），Service 直接 `BaseContext.getCurrentId()`；
- 校验失败 `response.setStatus(401)` 并中断。

### 2. 公共字段自动填充
`@AutoFill(INSERT | UPDATE)` + AOP 拦截 `com.sky.mapper.*` 上带注解的方法，反射填充 `create_time / create_user / update_time / update_user`，业务层不再手写。

### 3. 订单状态机

| 值 | 状态 | 允许的流转 |
|---|---|---|
| 1 | 待付款 | 支付→2；超时/用户取消→6 |
| 2 | 待接单 | 接单→3；拒单→6；用户取消→6 |
| 3 | 已接单 | 派送→4；管理端取消→6 |
| 4 | 派送中 | 完成→5；超时自动完成→5；管理端取消→6 |
| 5 | 已完成 | 再来一单（复制明细到购物车） |
| 6 | 已取消 | — |

支付状态 `pay_status`：0 未支付、1 已支付、2 已退款（取消已付款订单时置 2，不真实退款）。

### 4. 定时任务 `OrderTask`

| cron | 规则 |
|---|---|
| `0 * * * * ?` | 每分钟取消「创建超过 15 分钟仍待付款」的订单 |
| `0 0 1 * * ?` | 每天 01:00 把「派送中超过 60 分钟」的订单置为已完成 |

两处都用 `where status = ?` 的 CAS 更新，避免并发重复处理。

### 5. WebSocket 来单提醒 / 催单
- 端点 `/ws/{sid}`，`static ConcurrentHashMap<String, Session>` 保存会话；
- 支付成功推 `type=1`，用户催单推 `type=2`，消息体 `{type, orderId, content}`；
- 管理端弹窗提示，点击跳转订单详情；内嵌 Tomcat 需要 `ServerEndpointExporter`，打 war 部署外部 Tomcat 时要移除。

### 6. 缓存

| 数据 | 方式 | key |
|---|---|---|
| 菜品（按分类） | 手写 `RedisTemplate` | `dish_{categoryId}`，增删改/起停售时清除 |
| 套餐（按分类） | Spring Cache 注解 | `setmealCache`，写操作 `@CacheEvict` |
| 店铺营业状态 | Redis | `SHOP_STATUS` |

### 7. 数据统计与导出
- 营业额：按天 `sum(amount) where status=5`，日期与金额以逗号串返回，前端 `split(',')`；
- 用户：每日新增 + 截止当日累计；
- 订单：每日总数 / 有效数 + 区间总单量、有效单量、完成率；
- Top10：`order_detail` 按名称汇总数量取前 10；
- 工作台：营业额、有效订单、完成率、客单价、新增用户（分母为 0 时返回 0）；
- 导出：POI 读取 `template/运营数据报表模板.xlsx`，填概览（第 4/5 行）与近 30 天明细（第 8 行起），以二进制流写出。

## 前端页面地图

### 管理端路由（`src/router.ts`）

| 路由 | 页面 | 说明 |
|---|---|---|
| `/login` | 登录 | 默认填入 `admin / 123456`，实际调 `/employee/login` |
| `/dashboard` | 工作台 | 营业数据卡片、订单状态统计、菜品/套餐总览、订单列表 |
| `/order` | 订单管理 | 搜索 + 全部/待接单/待派送/派送中/已完成/已取消，接单、拒单、派送、完成、取消、详情 |
| `/statistics` | 数据统计 | 营业额、用户、订单、Top10 图表 + 导出 Excel |
| `/dish`、`/dish/add` | 菜品 | 列表、起售停售、新增/编辑 |
| `/setmeal`、`/setmeal/add` | 套餐 | 列表、起售停售、新增/编辑 |
| `/category` | 分类 | 菜品/套餐分类管理 |
| `/employee`、`/employee/add` | 员工 | 列表、启停用、新增/编辑 |

请求封装在 `src/utils/request.ts`：`baseURL = VUE_APP_BASE_API`、超时 600s、自动带 `token`、401 跳登录、重复请求自动取消。

### 小程序页面（`pages.json`）

| 页面 | 作用 |
|---|---|
| `pages/index/index` | 点餐首页：分类、菜品/套餐、规格选择、购物车、营业状态门禁 |
| `pages/order/index` | 确认订单：地址、备注、金额、提交 |
| `pages/pay/index` | 支付页：15 分钟倒计时，调 `PUT /user/order/payment`（模拟支付） |
| `pages/success/index` | 下单成功 |
| `pages/details/index` | 订单详情：催单、取消、再来一单 |
| `pages/historyOrder/historyOrder` | 历史订单（全部/待付款/退款） |
| `pages/address/address`、`pages/addOrEditAddress/addOrEditAddress` | 地址簿 |
| `pages/remark/index` | 订单备注 |
| `pages/my/my` | 个人中心 |
| `pages/nonet/index` | 网络异常页 |

`utils/env.js` 硬编码 `baseUrl = http://localhost:8080`；`utils/request.js` 统一带 `authentication` 头；token 只存 Vuex，不落本地存储，重启小程序需重新授权登录。

## 接口速览

共 **17 个 Controller、72 个接口**（管理端 46 / 用户端 26），启动后访问 `http://localhost:8080/doc.html` 查看完整文档（分管理端、用户端两个分组）。

| 分组 | 控制器 | 前缀 | 接口数 |
|---|---|---|---|
| 管理端 | Employee | `/admin/employee` | 7 |
| 管理端 | Category | `/admin/category` | 6 |
| 管理端 | Dish | `/admin/dish` | 7 |
| 管理端 | Setmeal | `/admin/setmeal` | 6 |
| 管理端 | Order | `/admin/order` | 8 |
| 管理端 | Report | `/admin/report` | 5 |
| 管理端 | WorkSpace | `/admin/workspace` | 4 |
| 管理端 | Shop | `/admin/shop` | 2 |
| 管理端 | Common | `/admin/common` | 1 |
| 用户端 | User | `/user/user` | 1 |
| 用户端 | Order | `/user/order` | 8 |
| 用户端 | ShoppingCart | `/user/shoppingCart` | 4 |
| 用户端 | AddressBook | `/user/addressBook` | 7 |
| 用户端 | Category | `/user/category` | 1 |
| 用户端 | Dish | `/user/dish` | 1 |
| 用户端 | Setmeal | `/user/setmeal` | 2 |
| 用户端 | Shop | `/user/shop` | 2 |

## 数据库设计

库名 `sky_take_out`（utf8mb4），共 11 张表：

| 表 | 说明 | 关键字段 |
|---|---|---|
| `employee` | 员工 | username、password(MD5)、status |
| `category` | 分类 | type(1菜品/2套餐)、sort、status |
| `dish` | 菜品 | category_id、price、image、status |
| `dish_flavor` | 菜品口味 | dish_id、name、value |
| `setmeal` | 套餐 | category_id、price、status |
| `setmeal_dish` | 套餐-菜品 | setmeal_id、dish_id、copies |
| `user` | 用户 | openid、create_time |
| `address_book` | 地址簿 | user_id、is_default、经纬度 |
| `shopping_cart` | 购物车 | user_id、dish_id/setmeal_id、number |
| `orders` | 订单 | number、status、pay_status、amount、地址快照 |
| `order_detail` | 订单明细 | order_id、name、number、amount |

> 仓库未附带建表 SQL，需按课程提供的 `sky_take_out.sql` 初始化。

## 快速开始

### 1. 准备环境
JDK 8+、Maven 3.6+、MySQL 5.7+、Redis 5+、Node 16+；小程序另需 HBuilderX + 微信开发者工具。

### 2. 初始化数据库
创建 `sky_take_out`（utf8mb4），导入建表与初始化数据。

### 3. 配置后端
`application.yml` 全部使用 `${sky.*}` 占位符，真实值放本地 `sky-server/src/main/resources/application-dev.yml`（已被忽略）：

```yaml
sky:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost
    port: 3306
    database: sky_take_out
    username: root
    password: 你的数据库密码
  redis:
    host: localhost
    port: 6379
    password: 你的Redis密码
    database: 10
  alioss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key-id: 你的AccessKeyId
    access-key-secret: 你的AccessKeySecret
    bucket-name: 你的Bucket名称
  wechat:
    appid: 你的小程序AppID
    secret: 你的小程序Secret
  baidu:
    ak: 你的百度地图AK
  shop:
    name: 苍穹外卖
    address: 北京市海淀区上地十街10号   # 小程序首页展示 + 配送范围校验用
    phone: 400-000-0000
```

### 4. 启动后端
```bash
mvn -pl sky-server -am spring-boot:run
# 或 IDEA 运行 com.sky.SkyApplication
```
默认 `http://localhost:8080`，接口文档 `http://localhost:8080/doc.html`。

### 5. 启动管理端
```bash
cd frontend/admin
npm install --legacy-peer-deps
npm run serve      # http://localhost:8888
npm run build      # 生产构建，dist/ 交给 Nginx
```
- 默认账号 `admin / 123456`（口令存 `employee` 表，MD5）；
- 开发环境 `.env.development`：`/api` 代理到 `http://localhost:8080/admin`，WebSocket 指向 `ws://localhost:8080/ws/`；
- 生产环境 `.env.production`：`/api` 与 `ws://localhost:81/ws/`，由 Nginx 反代；
- Node 17+ 需 `NODE_OPTIONS=--openssl-legacy-provider`（webpack 4）。

### 6. 启动小程序
1. HBuilderX 打开 `frontend/miniapp`，运行到微信开发者工具（首次运行会在本地生成 `unpackage/`，该目录不进仓库）；
2. 或先编译，再用微信开发者工具导入 `unpackage/dist/dev/mp-weixin`；
3. 后端地址在 `utils/env.js` 的 `baseUrl`，默认 `http://localhost:8080`（不走 Nginx）。

> 注意：微信开发者工具读的是**编译产物** `unpackage/dist/dev/mp-weixin`，不是 `pages/` 下的源码。
> 改完源码必须在 HBuilderX 里重新“运行到小程序”，只关掉小程序重进是不会生效的。

## 部署拓扑

管理端静态资源与接口统一由 Nginx 暴露（示例端口 81）：

```nginx
server {
    listen       81;
    server_name  localhost;
    root         html/sky;
    index        index.html;

    client_max_body_size 20m;

    # SPA history 路由兜底
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 接口：/api/xxx → http://127.0.0.1:8080/admin/xxx
    location ^~ /api/ {
        proxy_pass http://127.0.0.1:8080/admin/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # WebSocket：/ws/xxx → http://127.0.0.1:8080/ws/xxx
    location ^~ /ws/ {
        proxy_pass http://127.0.0.1:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }
}
```

## 常见问题

| 现象 | 原因 / 处理 |
|---|---|
| 启动报数据库或 Redis 连接失败 | `application-dev.yml` 缺失或账号密码错误，MySQL/Redis 未启动 |
| 接口 401 | 管理端请求头 `token`、用户端 `authentication` 未带或已过期 |
| 管理端打包报 `ERR_OSSL_EVP_UNSUPPORTED` | 设置 `NODE_OPTIONS=--openssl-legacy-provider` |
| WebSocket 握手失败 | Nginx `/ws/` 未配 `Upgrade`/`Connection`；地址要带 81 端口 |
| 小程序改了代码不生效 | 微信开发者工具读的是 `unpackage/dist/dev/mp-weixin`，需 HBuilderX 重新编译 |
| 下单提示「购物车数据为空」 | 下单成功会立即清空购物车，页面残留是前端状态未刷新 |
| 导出文件打不开 | 模板未随包发布，或接口被 `Result` 包装成 JSON |
| 报表出现 NaN | 统计分母为 0，代码已兜底为 0 |

## 已知限制与接口缺口

**功能限制**

- 支付为模拟：`payment()` 直接调用 `paySuccess()` 并返回 `null`，未接入微信商户号与证书；
- `BaseContext` 基于 ThreadLocal，拦截器未在 `afterCompletion` 中 `remove()`，线程复用时存在脏数据风险；
- 「再来一单」按订单 id 复制明细，未校验订单归属；
- 店铺名称 / 地址 / 电话来自后端配置 `sky.shop.*`，管理端没有编辑入口，改地址要改配置文件并重启；
- `application.yml` 中 JWT 密钥为课程默认弱口令，生产环境应改为环境变量注入的随机串；
- 未附带数据库初始化 SQL 与单元测试；
- 小程序 token 只存 Vuex，重启后需重新登录。

**前端已调用、后端暂无对应实现**

| 前端位置 | 请求 | 影响 |
|---|---|---|
| 管理端消息中心 `views/inform` | `/messages/*` | 页面未注册路由，功能不可达 |
| 管理端 Navbar 改密 | `/employee/editPassword` | 改密弹层不可用 |
| 管理端 `api/index.ts` | `/report/dataOverView` | 未被页面调用，无影响 |
| 管理端图片展示 | `/common/download` | 后端未提供下载接口 |

## 开发日志

仓库共 57 次提交，按功能推进大致为：

1. **基础**：多模块骨架、公共字段填充、JWT 双端鉴权、Knife4j；
2. **后台商品**：员工、分类、菜品（含口味）、套餐、图片上传；
3. **用户端**：微信登录、商品浏览、购物车、地址簿、下单与模拟支付；
4. **订单闭环**：搜索、状态统计、接单、拒单、派送、完成、取消、再来一单；
5. **增强**：Spring Task 定时处理、WebSocket 来单提醒与催单；
6. **数据**：工作台、营业额/用户/订单/Top10 统计、Excel 报表导出；
7. **工程化**：管理端与小程序源码合入、README 与部署说明。

## 说明

本项目仅用于学习与技术验证，未接入真实微信支付、短信、地图等第三方服务。仓库内所有密钥均为占位符或课程默认值，请勿把真实的数据库口令、OSS/微信密钥提交到仓库。
