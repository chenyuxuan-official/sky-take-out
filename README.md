# 苍穹外卖 sky-take-out

> 一套完整的外卖点餐系统，单体仓库（monorepo）：**Spring Boot 后端** + **Vue 管理端** + **uni-app 微信小程序**。
> 项目源自「黑马程序员 · 苍穹外卖」课程，完成了全部业务模块、定时任务、WebSocket 来单提醒、数据统计报表与前端页面。

## 目录

- [项目简介](#项目简介)
- [仓库结构](#仓库结构)
- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [核心设计](#核心设计)
- [快速开始](#快速开始)
- [接口文档](#接口文档)
- [数据库](#数据库)
- [常见问题](#常见问题)

## 项目简介

苍穹外卖是一套完整的餐饮外卖系统：商家在管理端维护菜品/套餐、接单派送、查看营业数据；顾客在微信小程序里浏览商品、下单支付、催单评价。

```
frontend/miniapp (uni-app 小程序) ──┐
                                    ├──► sky-server :8080 ──► MySQL / Redis
frontend/admin (Vue2 + Element UI) ─┘            ▲
                                                 │ /api、/ws 反向代理
                                              Nginx :81
```

后端为 Maven 多模块工程，通过 JWT 区分**管理端令牌**（请求头 `token`）和**用户端令牌**（请求头 `authentication`），两套接口分别挂载在 `/admin/**` 与 `/user/**`。

## 仓库结构

```
sky-take-out
├── pom.xml                    # 聚合工程，统一管理依赖版本
├── sky-common                 # 公共模块：常量、上下文、异常、结果封装、JWT、工具类
├── sky-pojo                   # 实体模块：entity / dto / vo
├── sky-server                 # 业务模块：Web 层、服务层、持久层、配置
│   └── src/main
│       ├── java/com/sky
│       │   ├── SkyApplication.java      # 启动类（@EnableScheduling）
│       │   ├── annotation / aspect      # @AutoFill 与公共字段自动填充
│       │   ├── config                   # WebMvc、Redis、OSS、WebSocket 配置
│       │   ├── controller/admin|user    # 管理端 9 个、用户端 8 个 Controller
│       │   ├── handler / interceptor    # 全局异常、JWT 拦截器
│       │   ├── mapper / service         # MyBatis 接口与业务实现
│       │   ├── task                     # 订单定时任务
│       │   └── websocket                # 来单提醒 / 催单端点
│       └── resources
│           ├── mapper/*.xml             # MyBatis 映射
│           ├── template/                # Excel 报表模板
│           └── application.yml          # 占位符配置（真实值放本地 profile）
└── frontend
    ├── admin                  # 管理端：Vue 2 + TypeScript + Element UI（Vue CLI 3）
    └── miniapp                # 用户端：uni-app 微信小程序
```

## 功能特性

### 管理端 `/admin`

| 模块 | 说明 |
|---|---|
| 员工管理 | 登录、新增/修改/启停用、分页查询 |
| 分类管理 | 菜品/套餐分类的增删改查、启用禁用 |
| 菜品管理 | 新增（含口味）、修改、起售停售、分页查询、图片上传 |
| 套餐管理 | 新增（含套餐菜品关系）、修改、起售停售、删除 |
| 店铺状态 | 营业 / 打烊切换 |
| 订单管理 | 条件搜索、各状态数量统计、接单、拒单、派送、完成、取消 |
| 来单提醒 | WebSocket 推送新订单，管理端弹窗 + 响铃 |
| 客户催单 | 接收用户催单消息并提醒商家 |
| 工作台 | 今日营业额、有效订单、完成率、客单价、新增用户；订单/菜品/套餐总览 |
| 数据统计 | 营业额、用户、订单三类趋势统计 + 销量 Top10（ECharts） |
| 报表导出 | 按 Excel 模板导出最近 30 天运营数据 |

### 用户端 `/user`

| 模块 | 说明 |
|---|---|
| 微信登录 | `code` 换取 openid，自动注册并签发 JWT |
| 商品浏览 | 分类、菜品（含口味）、套餐列表与详情 |
| 购物车 | 加购、减购、清空、查看 |
| 地址簿 | 新增、修改、删除、查询默认地址 |
| 下单支付 | 提交订单、模拟支付、清空购物车 |
| 订单管理 | 历史订单、订单详情、取消订单、再来一单、催单 |

### 订单状态机

```
1 待付款 ──支付──► 2 待接单 ──接单──► 3 已接单 ──派送──► 4 派送中 ──完成──► 5 已完成
   │                  │
   └──超时/取消──► 6 已取消       └──拒单──► 7 已拒单
```

## 技术栈

### 后端

| 分类 | 技术 | 版本 |
|---|---|---|
| 基础框架 | Spring Boot | 2.7.3 |
| 持久层 | MyBatis / MyBatis-Spring-Boot-Starter | 2.2.0 |
| 数据库 | MySQL | 5.7+ |
| 连接池 / 分页 | Druid / PageHelper | 1.2.1 / 1.3.0 |
| 缓存 | Redis（Spring Data Redis + Spring Cache） | — |
| 接口文档 | Knife4j | 3.0.2 |
| 鉴权 | JJWT | 0.9.1 |
| 实时通信 | Spring WebSocket（`@ServerEndpoint`） | — |
| 定时任务 | Spring Task（`@Scheduled`） | — |
| 报表导出 | Apache POI | 3.16 |
| 其他 | Fastjson / Lombok / AspectJ / 阿里云 OSS SDK | — |

### 前端

| 端 | 技术 | 说明 |
|---|---|---|
| 管理端 | Vue 2 + TypeScript + Element UI + Vue CLI 3 + ECharts | 开发端口 8888，生产构建产物由 Nginx 托管 |
| 小程序 | uni-app（Vue 2 语法） | HBuilderX 编译到微信开发者工具 |

## 核心设计

### 双端 JWT 鉴权
- 管理端令牌放在请求头 `token`，用户端令牌放在 `authentication`；
- 两个拦截器分别拦截 `/admin/**` 与 `/user/**`，解析后把当前用户 id 放进 `BaseContext`（ThreadLocal），服务层直接取用。

### 公共字段自动填充
自定义注解 `@AutoFill(INSERT | UPDATE)` + AOP 切面统一填充 `create_time`、`update_time`、`create_user`、`update_user`。

### 订单超时与自动完成
- 每分钟扫描待付款超过 15 分钟的订单并取消；
- 每天凌晨扫描派送中超过 60 分钟的订单并自动完成；
- 更新语句带 `status` 条件（CAS），避免并发重复处理。

### WebSocket 来单提醒 / 客户催单
- 端点 `/ws/{sid}`，会话存 `ConcurrentHashMap`；
- 支付成功推 `type=1`（来单），用户催单推 `type=2`（催单），管理端弹窗并播放对应铃声；
- Nginx 反代 `/ws/` 必须带 `Upgrade` / `Connection "upgrade"`。

### 数据统计与导出
- 营业额、用户、订单趋势按天聚合，缺失日期补 0；
- Top10 按 `order_detail` 数量汇总，只统计已完成订单；
- 导出读取 `resources/template/运营数据报表模板.xlsx`，用 POI 填充后写入响应流。

## 快速开始

### 1. 环境要求

- JDK 8+（本项目在 JDK 17 下编译运行）
- MySQL 5.7+、Redis 5+
- Maven 3.6+
- Node 16+（管理端）、HBuilderX + 微信开发者工具（小程序）

### 2. 初始化数据库

创建 `sky_take_out`（utf8mb4），导入课程提供的建表与初始化 SQL。

### 3. 后端配置

`application.yml` 里全部是 `${sky.*}` 占位符，真实值放在本地 `application-dev.yml`（已在 `.gitignore` 中忽略）。在 `sky-server/src/main/resources/` 新建：

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
```

启动后端：

```bash
mvn -pl sky-server -am spring-boot:run
```

或直接在 IDEA 运行 `com.sky.SkyApplication`，默认端口 `8080`。

### 4. 管理端

```bash
cd frontend/admin
npm install --legacy-peer-deps
npm run serve          # 开发模式，http://localhost:8888
npm run build          # 生产构建，产物在 dist/，拷到 Nginx 静态目录
```

- 接口地址由 `.env.development`（开发，代理到 `http://localhost:8080/admin`）和 `.env.production`（生产，走 `/api` 由 Nginx 反代）控制；
- 登录账号：`admin / 123456`；
- 老版本 webpack 4 在 Node 17+ 下需要 `NODE_OPTIONS=--openssl-legacy-provider`。

### 5. 小程序

1. 用 HBuilderX 打开 `frontend/miniapp`，运行到微信开发者工具；
2. 或先编译，再用微信开发者工具导入 `unpackage/dist/dev/mp-weixin`；
3. 后端地址在 `utils/env.js` 的 `baseUrl`（默认 `http://localhost:8080`，不经过 Nginx）。

## 接口文档

启动后端后访问 Knife4j：

```
http://localhost:8080/doc.html
```

管理端接口前缀 `/admin`，用户端接口前缀 `/user`，在文档中分组展示。

## 数据库

共 11 张表：

| 表名 | 说明 |
|---|---|
| `employee` | 员工 |
| `category` | 分类（菜品/套餐） |
| `dish` / `dish_flavor` | 菜品 / 菜品口味 |
| `setmeal` / `setmeal_dish` | 套餐 / 套餐-菜品关系 |
| `user` | 用户（微信） |
| `address_book` | 收货地址 |
| `shopping_cart` | 购物车 |
| `orders` / `order_detail` | 订单 / 订单明细 |

## 常见问题

- **启动报数据库/Redis 连接失败**：确认本地 `application-dev.yml` 存在且账号密码正确，MySQL、Redis 已启动。
- **管理端登录后接口 401**：请求头要带 `token`（管理端）或 `authentication`（用户端）。
- **WebSocket 握手失败**：Nginx 的 `/ws/` 必须配置 `proxy_http_version 1.1`、`Upgrade`、`Connection "upgrade"`；前端地址要带端口（`ws://localhost:81/ws/`）。
- **管理端打包报 `ERR_OSSL_EVP_UNSUPPORTED`**：设置 `NODE_OPTIONS=--openssl-legacy-provider`。
- **小程序改了代码没生效**：微信开发者工具加载的是 `unpackage/dist/dev/mp-weixin`，需在 HBuilderX 重新编译。
- **支付没有唤起微信支付**：当前为模拟支付，下单后直接调用支付成功逻辑，未接入真实商户号。
- **报表导出打不开**：确认 `template/运营数据报表模板.xlsx` 随包发布，接口以二进制流输出（不要用 `Result` 包装）。

## 说明

本项目仅用于学习与技术验证，未接入真实微信支付、短信、地图等第三方服务。仓库内所有第三方密钥均为占位符，请不要把真实的数据库口令、OSS/微信密钥提交到仓库。
