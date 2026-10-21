# 苍穹外卖 sky-take-out

> 基于 Spring Boot 的外卖点餐系统后端，包含**管理端**（商家后台）与**用户端**（微信小程序）两套接口。
> 项目源自「黑马程序员 · 苍穹外卖」课程，在此基础上完成了全部业务模块、定时任务、WebSocket 来单提醒与数据统计报表。

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [核心设计](#核心设计)
- [快速开始](#快速开始)
- [接口文档](#接口文档)
- [数据库](#数据库)
- [常见问题](#常见问题)

## 项目简介

苍穹外卖是一套完整的餐饮外卖系统：商家在管理端维护菜品/套餐、接单派送、查看营业数据；顾客在微信小程序里浏览商品、下单支付、催单评价。

后端采用 Maven 多模块结构，通过 JWT 区分**管理端令牌**（请求头 `token`）与**用户端令牌**（请求头 `authentication`），两套接口分别挂载在 `/admin/**` 与 `/user/**` 下。

```
管理端(Vue + Element UI) ──┐
                          ├──► Nginx ──► sky-server :8080 ──► MySQL / Redis
用户端(微信小程序 uniapp) ──┘
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
| 工作台 | 今日营业额、有效订单、订单完成率、客单价、新增用户；订单/菜品/套餐总览 |
| 数据统计 | 营业额、用户、订单三类趋势统计 + 销量 Top10 |
| 报表导出 | 基于 Excel 模板导出最近 30 天运营数据报表 |

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

| 分类 | 技术 | 版本 |
|---|---|---|
| 基础框架 | Spring Boot | 2.7.3 |
| 持久层 | MyBatis / MyBatis-Spring-Boot-Starter | 2.2.0 |
| 数据库 | MySQL | 5.7+ |
| 连接池 | Druid | 1.2.1 |
| 分页 | PageHelper | 1.3.0 |
| 缓存 | Redis（Spring Data Redis + Spring Cache） | — |
| 接口文档 | Knife4j | 3.0.2 |
| 鉴权 | JJWT | 0.9.1 |
| 实时通信 | Spring WebSocket（JSR-356 `@ServerEndpoint`） | — |
| 定时任务 | Spring Task（`@Scheduled`） | — |
| 报表导出 | Apache POI | 3.16 |
| JSON | Fastjson | 1.2.76 |
| 对象存储 | 阿里云 OSS SDK | 3.10.2 |
| 其他 | Lombok / AspectJ | — |

## 项目结构

```
sky-take-out
├── pom.xml                 # 聚合工程，统一管理依赖版本
├── sky-common              # 公共模块：常量、上下文、异常、结果封装、JWT、工具类
│   └── src/main/java/com/sky/{constant,context,enumeration,exception,json,properties,result,utils}
├── sky-pojo                # 实体模块：entity / dto / vo
│   └── src/main/java/com/sky/{entity,dto,vo}
└── sky-server              # 业务模块：Web 层、服务层、持久层、配置
    └── src/main
        ├── java/com/sky
        │   ├── SkyApplication.java      # 启动类（@EnableScheduling）
        │   ├── annotation               # @AutoFill 等自定义注解
        │   ├── aspect                   # 公共字段自动填充
        │   ├── config                   # WebMvc、Redis、OSS、WebSocket 配置
        │   ├── controller/admin|user    # 管理端 9 个、用户端 8 个 Controller
        │   ├── handler                  # 全局异常处理
        │   ├── interceptor              # JWT 拦截器（管理端 / 用户端各一）
        │   ├── mapper                   # MyBatis 接口
        │   ├── service                  # 业务接口与实现
        │   ├── task                     # 订单定时任务
        │   └── websocket                # WebSocket 服务端点
        └── resources
            ├── mapper/*.xml             # MyBatis 映射文件
            ├── template/                # Excel 报表模板
            └── application.yml          # 占位符配置（真实值放本地 profile）
```

## 核心设计

### 双端 JWT 鉴权
- 管理端令牌放在请求头 `token`，用户端令牌放在 `authentication`；
- 两个拦截器分别拦截 `/admin/**` 与 `/user/**`，解析后将当前用户 id 存入 `BaseContext`（ThreadLocal），服务层直接取用，避免层层传参。

### 公共字段自动填充
通过自定义注解 `@AutoFill(OperationType.INSERT | UPDATE)` + AOP 切面，统一填充 `create_time`、`update_time`、`create_user`、`update_user`，业务代码不再重复赋值。

### 订单超时与自动完成
- 待付款超过 15 分钟的订单自动取消；
- 派送中的订单超过一定时间自动完成；
- 更新语句带 `status` 作为条件（CAS），防止并发下重复处理。

### WebSocket 来单提醒 / 客户催单
- 端点 `/ws/{sid}`，会话保存在 `ConcurrentHashMap` 中；
- 支付成功推送 `type=1`（来单提醒），用户催单推送 `type=2`（催单），管理端收到后弹窗并播放对应铃声；
- 注意 `ServerEndpointExporter` 仅在内嵌 Tomcat 下需要注册。

### 数据统计与导出
- 营业额、用户、订单趋势按天聚合，缺失日期补 0；
- 销量 Top10 按 `order_detail` 数量汇总，仅统计已完成订单；
- 报表导出读取 `resources/template/运营数据报表模板.xlsx`，用 POI 填充概览与每日明细后写入响应流。

## 快速开始

### 1. 环境要求
- JDK 8+（本项目在 JDK 17 下编译运行）
- MySQL 5.7+、Redis 5+
- Maven 3.6+

### 2. 初始化数据库
创建数据库 `sky_take_out`（utf8mb4），并导入课程提供的建表与初始化 SQL。

### 3. 配置
`application.yml` 中的配置全部使用 `${sky.*}` 占位符，真实值放在本地 `application-dev.yml`（该文件已被 `.gitignore` 忽略，不会提交）。

在 `sky-server/src/main/resources/` 下新建 `application-dev.yml`：

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

### 4. 启动
```bash
mvn -pl sky-server -am spring-boot:run
```
或直接在 IDEA 中运行 `com.sky.SkyApplication`，服务默认监听 `8080`。

### 5. 前端（独立仓库/工程）
- 管理端：Vue 2 + TypeScript + Element UI，接口基地址指向 `/api`，由 Nginx 反代到 `8080`；`/ws/` 需配置 `Upgrade`/`Connection` 头用于 WebSocket 握手。
- 用户端：uni-app 微信小程序，接口直连 `http://localhost:8080`。

## 接口文档

启动后访问 Knife4j 页面：

```
http://localhost:8080/doc.html
```

管理端接口前缀 `/admin`，用户端接口前缀 `/user`，均在 Swagger 中分组展示。

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
- **管理端能登录但接口 401**：检查请求头是否带了 `token`（管理端）或 `authentication`（用户端）。
- **WebSocket 握手失败**：Nginx 的 `/ws/` location 必须设置 `proxy_http_version 1.1`、`Upgrade`、`Connection "upgrade"`。
- **支付为什么没有唤起微信支付**：当前为课程模拟支付，下单后直接调用支付成功逻辑，未接入真实商户号与证书。
- **报表导出中文乱码/打不开**：确认 `template/运营数据报表模板.xlsx` 随包发布，且接口以二进制流输出（不要用 `Result` 包装）。

## 说明

本项目仅用于学习与技术验证，尚未接入真实微信支付、短信、地图等第三方服务；相关配置项均为占位符，请勿把真实密钥提交到仓库。
