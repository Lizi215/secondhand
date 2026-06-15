# 二手物品交易系统 (Second-Hand Trading Platform)

[![JDK](https://img.shields.io/badge/JDK-1.8-blue.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.12-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Hoxton.SR12-blueviolet.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Alibaba-2.2.6-orange.svg)](https://spring.io/projects/spring-cloud-alibaba)
[![License](https://img.shields.io/badge/License-MIT-green.svg)]()

基于 **Java Swing 桌面客户端** + **Spring Cloud 微服务** 的二手物品交易平台，支持商品发布浏览、聊天沟通、用户管理等功能。

## 目录

- [项目简介](#项目简介)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [模块说明](#模块说明)
- [快速开始](#快速开始)
  - [环境要求](#环境要求)
  - [本地运行](#本地运行)
  - [Docker 部署](#docker-部署)
- [API 接口](#api-接口)
- [数据库设计](#数据库设计)
- [Redis 缓存策略](#redis-缓存策略)
- [项目结构](#项目结构)
- [开发指南](#开发指南)
- [常见问题](#常见问题)

## 项目简介

一个完整的二手物品交易系统，提供以下核心功能：

- **用户模块**：注册、登录、个人信息管理
- **商品模块**：商品的发布、浏览、搜索、修改、下架
- **聊天模块**：买家和卖家之间实时聊天（轮询模式）
- **管理后台**：管理员对用户和商品进行管理（禁言、删除等）
- **桌面客户端**：基于 Swing 的跨平台桌面应用

后端采用微服务架构，通过 Nacos 实现服务注册与发现，Gateway 统一路由和鉴权，ShardingSphere 实现数据库分片。

## 系统架构

```
┌──────────────────────────────────────────────────────────────────┐
│                    Swing 桌面客户端 (Java)                         │
│         LoginFrame / MainFrame / ChatFrame / AdminFrame           │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTP REST API (JSON)
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                  Spring Cloud Gateway (端口 8088)                   │
│              路由转发 / JWT 鉴权 / CORS / 请求头传递                │
└───────┬──────────┬──────────┬──────────┬─────────────────────────┘
        │          │          │          │
        ▼          ▼          ▼          ▼
┌────────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐
│ Auth       │ │ User     │ │ Product  │ │ Chat       │
│ Service    │ │ Service  │ │ Service  │ │ Service    │
│ (8081)     │ │ (8082)   │ │ (8083)   │ │ (8084)     │
│ 认证/授权  │ │ 用户管理  │ │ 商品CRUD  │ │ 消息收发   │
└──────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬─────┘
       │            │            │               │
       │            │     ┌──────▼──────┐        │
       │            │     │   Redis 7   │        │
       │            │     │ (缓存/ID)   │        │
       │            │     └─────────────┘        │
       └────────────┴──────────┬─────────────────┘
                               │
                        ┌──────▼──────┐
                        │  MySQL 5.7  │
                        │  ShardingSphere 分片  │
                        └─────────────┘

                        ┌─────────────┐
                        │ Nacos 2.0.4 │
                        │ 注册发现/配置 │
                        └─────────────┘
```

### 组件协作关系

| 组件 | 作用 |
|------|------|
| **Nacos** | 服务注册与发现，所有微服务启动时向 Nacos 注册 |
| **Gateway** | 统一入口，JWT 鉴权过滤器解析 Token，通过 `X-User-Id`/`X-User-Role` 请求头传递用户信息给下游服务 |
| **Auth Service** | 登录/注册，使用 SHA-256 哈希密码，签发 JWT Token |
| **User Service** | 用户 CRUD，管理员功能（列表、删除、禁言、改密码） |
| **Product Service** | 商品 CRUD、搜索分页、Redis 缓存 |
| **Chat Service** | 消息发送、历史记录查询、轮询新消息 |
| **Redis** | 商品详情缓存（5分钟）、列表缓存（15秒）、商品 ID 序号生成 |
| **ShardingSphere** | 每逻辑表 3 张物理分片，按用户 ID 取模分片 |

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 1.8 |
| 框架 | Spring Boot | 2.3.12.RELEASE |
| 微服务 | Spring Cloud (Hoxton) | Hoxton.SR12 |
| 微服务 | Spring Cloud Alibaba | 2.2.6.RELEASE |
| 注册中心 | Nacos | 2.0.4 |
| ORM | MyBatis-Plus | 3.4.3.4 |
| 分库分表 | ShardingSphere-JDBC | 5.0.0 |
| 数据库 | MySQL | 5.7 |
| 连接池 | Druid | 1.2.8 |
| 缓存 | Redis | 7.x |
| 消息鉴权 | JWT (JJWT) | 0.9.1 |
| 工具库 | Hutool | 5.8.16 |
| 构建工具 | Maven | 3.6.3+ |
| 客户端 | Java Swing / AWT | — |
| 容器化 | Docker / docker-compose | — |

## 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| `common` | — | 公共 DTO、常量、异常定义、Result 封装、JwtUtils（jar 包引用） |
| `gateway` | 8088 | API 网关，JWT 鉴权过滤器，CORS 跨域配置，路由转发 |
| `auth-service` | 8081 | 登录、注册、JWT Token 签发 |
| `user-service` | 8082 | 用户 CRUD、管理员接口（列表/删除/禁言/改密码） |
| `product-service` | 8083 | 商品 CRUD、搜索分页、Redis 缓存 |
| `chat-service` | 8084 | 消息收发、历史记录、轮询新消息 |
| `client` | — | Swing 桌面客户端（LoginFrame, MainFrame, ChatFrame, 等） |

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 6.2+
- Nacos 2.0.4+
- Docker & docker-compose（可选，用于容器化部署）

### 本地运行

#### 1. 启动基础设施

```bash
# 启动 Nacos（服务注册中心）
# 下载 nacos-server-2.0.4 并执行：
sh bin/startup.sh -m standalone

# 启动 MySQL（确保有 secondhand_trade 数据库）
mysql -uroot -p < doc/sql/init.sql

# 启动 Redis
redis-server
```

#### 2. 构建项目

```bash
# 安装父 POM 和 common 模块到本地仓库
mvn install -N -q
mvn install -pl common -DskipTests -q

# 打包所有服务
mvn package -pl auth-service,user-service,product-service,chat-service,gateway,client -DskipTests -q
```

#### 3. 按顺序启动服务

```bash
# 1. auth-service（端口 8081）
java -jar auth-service/target/auth-service-1.0.0.jar

# 2. user-service（端口 8082）
java -jar user-service/target/user-service-1.0.0.jar

# 3. product-service（端口 8083）
java -jar product-service/target/product-service-1.0.0.jar

# 4. chat-service（端口 8084）
java -jar chat-service/target/chat-service-1.0.0.jar

# 5. gateway（端口 8088）
java -jar gateway/target/gateway-1.0.0.jar
```

#### 4. 启动客户端

```bash
java -jar client/target/client-1.0.0-jar-with-dependencies.jar
```

或通过 IDE 运行 `com.secondhand.client.ui.LoginFrame` 的 `main()` 方法。

> **注意**：客户端需在图形桌面环境中运行（Windows / Linux with X11 / macOS）。

### Docker 部署

项目支持完整的 Docker 容器化部署，一键启动所有服务。

#### 构建并启动

```bash
# 1. 构建 Java 项目（本地编译，Docker 仅运行）
mvn install -N -q
mvn install -pl common -DskipTests -q
mvn package -pl auth-service,user-service,product-service,chat-service,gateway -DskipTests -q

# 2. 构建 Docker 镜像并启动
docker-compose build
docker-compose up -d

# 3. 启动客户端（宿主机直连网关）
java -jar client/target/client-1.0.0-jar-with-dependencies.jar
```

#### 容器服务列表

| 容器名 | 内部端口 | 宿主机端口 |
|--------|---------|-----------|
| secondhand-mysql | 3306 | 3307 |
| secondhand-redis | 6379 | 6380 |
| secondhand-nacos | 8848 | 8849 |
| secondhand-auth | 8081 | 8081 |
| secondhand-user | 8082 | 8082 |
| secondhand-product | 8083 | 8083 |
| secondhand-chat | 8084 | 8084 |
| secondhand-gateway | 8088 | 8088 |

#### 常用运维命令

```bash
# 查看所有服务状态
docker ps --filter "name=secondhand" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 查看日志
docker logs secondhand-auth -f

# 停止所有服务（保留数据卷）
docker-compose down

# 停止并删除数据卷（重新初始化）
docker-compose down -v

# 重启所有服务
docker-compose up -d

# 导入测试数据
docker exec -i secondhand-mysql mysql -uroot -pCHANGED_DOCKER_PASS secondhand_trade \
  --default-character-set=utf8mb4 < doc/sql/rebuild.sql
```

> **注意**：Docker 部署默认密码为 `CHANGED_DOCKER_PASS`，数据库名为 `secondhand_trade`，详见 `application-docker.yml` 配置文件。

## API 接口

### 认证服务（auth-service）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录，返回 JWT Token |
| POST | `/auth/register` | 注册新用户 |

### 用户服务（user-service）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/info` | 获取当前登录用户信息 |
| GET | `/user/{id}` | 获取指定用户信息 |
| GET | `/user/batch?ids=1,2,3` | 批量获取用户信息 |
| GET | `/user/admin/list` | 管理员获取全部用户列表 |
| GET | `/user/admin/search?keyword=` | 管理员根据 ID 或用户名搜索用户 |
| DELETE | `/user/admin/delete/{id}` | 管理员删除用户 |
| PUT | `/user/admin/password` | 管理员修改用户密码 |
| PUT | `/user/admin/mute/{id}` | 管理员禁言/解禁用户 |

### 商品服务（product-service）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/product/add` | 上架商品 |
| PUT | `/product/update` | 修改商品信息 |
| DELETE | `/product/delete/{id}` | 删除自己的商品 |
| GET | `/product/my` | 获取自己的商品列表 |
| GET | `/product/list?page=&size=` | 分页获取商品列表（排除自己） |
| GET | `/product/search?keyword=` | 搜索商品（排除自己） |
| GET | `/product/{id}` | 获取商品详情 |
| DELETE | `/product/admin/delete/{id}` | 管理员删除任意商品 |
| GET | `/product/admin/user-products/{userId}` | 管理员查看指定用户的商品 |

### 聊天服务（chat-service）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/chat/send` | 发送消息 |
| GET | `/chat/history?userId=&productId=` | 获取聊天历史记录 |
| GET | `/chat/poll?lastMsgId=&otherUserId=` | 轮询新消息（每 3 秒） |
| GET | `/chat/product-inquirers/{productId}` | 查看询问某商品的买家列表 |

## 数据库设计

### 分片策略（ShardingSphere 5.0.0）

采用 **取模分片** 策略，每张逻辑表对应 3 张物理表：

| 逻辑表 | 分片键 | 物理表 | 算法 |
|--------|--------|--------|------|
| `user` | `user_id` | `user_0`, `user_1`, `user_2` | `user_id % 3` |
| `product` | `user_id` | `product_0`, `product_1`, `product_2` | `user_id % 3` |
| `chat_message` | `from_user_id` | `chat_message_0`, `chat_message_1`, `chat_message_2` | `from_user_id % 3` |

### 核心表结构

#### user 表 — 用户

```sql
CREATE TABLE user_x (
    user_id    BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,            -- SHA-256 哈希
    role       TINYINT      NOT NULL DEFAULT 0,  -- 0:普通用户 1:管理员
    nickname   VARCHAR(50),
    phone      VARCHAR(20),
    is_muted   TINYINT      NOT NULL DEFAULT 0,  -- 0:正常 1:禁言
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL
);
```

#### product 表 — 商品

```sql
CREATE TABLE product_x (
    product_id   BIGINT        PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(100)  NOT NULL,
    price        DECIMAL(10,2) NOT NULL,
    description  TEXT,
    pickup_point VARCHAR(200),                   -- 自提点
    user_id      BIGINT        NOT NULL,         -- 商家 ID
    seller_name  VARCHAR(50),                    -- 卖家昵称（冗余字段）
    status       TINYINT       NOT NULL DEFAULT 1, -- 1:上架 0:下架
    created_at   DATETIME      NOT NULL,
    updated_at   DATETIME      NOT NULL
);
```

#### chat_message 表 — 聊天消息

```sql
CREATE TABLE chat_message_x (
    msg_id        BIGINT       PRIMARY KEY AUTO_INCREMENT,
    from_user_id  BIGINT       NOT NULL,
    to_user_id    BIGINT       NOT NULL,
    product_id    BIGINT       DEFAULT NULL,     -- 关联商品
    content       TEXT         NOT NULL,
    created_at    DATETIME     NOT NULL,
    has_read      TINYINT      NOT NULL DEFAULT 0
);
```

### 关键设计决策

- **商品 ID**：自定义格式 `YYYYMMDD + 6位序号`（如 `20260614000001`），通过 Redis `INCR` 生成，DB 查询兜底
- **用户 ID**：雪花算法，通过 MyBatis-Plus `IdType.ASSIGN_ID` 生成
- **聊天消息 ID**：自增（`IdType.AUTO`），分片键为 `from_user_id`
- **卖家昵称**：`product` 表中冗余 `seller_name` 字段，商品发布时从 user-service 获取一次，列表查询零跨服务调用
- **密码哈希**：Hutool `DigestUtil.sha256Hex()`

## Redis 缓存策略

| 缓存项 | Key | TTL | 说明 |
|--------|-----|-----|------|
| 商品 ID 序号 | `product:seq:YYYYMMDD` | 30 天 | INCR 原子操作；首次使用时与 DB MAX 同步 |
| 商品详情 | `product:detail:{id}` | 5 分钟 | 更新/删除时清除 |
| 商品列表（第 1 页） | `product:list:page:1:size:50:user:{id}:v{version}` | 15 秒 | 任何商品写入时版本号递增 |

> Redis 使用 database 2，默认无密码（Docker 部署密码为 `CHANGED_DOCKER_PASS`）。

## 项目结构

```
second-hand-trading/
├── pom.xml                              # 父工程（依赖版本管理）
│
├── common/                              # 公共模块
│   └── src/main/java/com/secondhand/
│       ├── config/                      # 全局异常处理器
│       ├── dto/                         # 公共 DTO
│       ├── entity/                      # 公共实体
│       ├── exception/                   # 异常定义
│       ├── utils/                       # JwtUtils 等工具类
│       └── Result.java                  # 统一 API 响应封装
│
├── gateway/                             # 网关服务
│   └── src/main/java/com/secondhand/gateway/
│       ├── config/                      # CORS 跨域配置
│       └── filter/                      # JwtAuthGlobalFilter
│
├── auth-service/                        # 认证服务
│   └── src/main/java/com/secondhand/auth/
│       ├── controller/                  # AuthController
│       ├── service/impl/                # 登录/注册逻辑
│       ├── dto/                         # LoginDTO, RegisterDTO
│       └── AuthServiceApplication.java
│
├── user-service/                        # 用户服务
│   └── src/main/java/com/secondhand/user/
│       ├── controller/                  # UserController, AdminController
│       ├── service/impl/
│       ├── mapper/
│       ├── entity/                      # User 实体
│       ├── dto/
│       ├── config/                      # MyBatis-Plus 配置
│       └── UserServiceApplication.java
│
├── product-service/                     # 商品服务
│   └── src/main/java/com/secondhand/product/
│       ├── controller/                  # ProductController, AdminController
│       ├── service/impl/
│       ├── mapper/
│       ├── entity/                      # Product 实体
│       ├── dto/
│       ├── config/                      # Redis 配置
│       └── ProductServiceApplication.java
│
├── chat-service/                        # 聊天服务
│   └── src/main/java/com/secondhand/chat/
│       ├── controller/                  # ChatController
│       ├── service/impl/
│       ├── mapper/
│       ├── entity/                      # ChatMessage 实体
│       ├── dto/
│       └── ChatServiceApplication.java
│
├── client/                              # Swing 桌面客户端
│   └── src/main/java/com/secondhand/client/
│       ├── ui/                          # 界面类
│       │   ├── LoginFrame.java          # 登录/注册窗口
│       │   ├── MainFrame.java           # 商品浏览主界面
│       │   ├── ChatFrame.java           # 聊天窗口
│       │   ├── MyProductFrame.java      # 我的商品管理
│       │   └── AdminFrame.java          # 管理员界面
│       ├── util/                        # HTTP 请求工具
│       └── dto/                         # 客户端 DTO
│
├── doc/sql/                             # 数据库脚本
│   ├── init.sql                         # 建库建表（含分片表）
│   ├── rebuild.sql                      # 重建并插入测试数据
│   └── more_data.sql                    # 更多测试数据
│
├── Dockerfile                           # 通用 Docker 镜像构建
├── docker-compose.yml                   # 容器编排
└── .dockerignore
```

## 开发指南

### 服务启动顺序

```
Nacos (8848) → MySQL + Redis → auth-service (8081) → user-service (8082)
→ product-service (8083) → chat-service (8084) → gateway (8088) → client
```

### 构建命令备忘

```bash
# 安装父 POM + common（首次或 common 变更时）
mvn install -N -q && mvn install -pl common -DskipTests -q

# 编译特定模块
mvn compile -pl product-service -am -q

# 打包所有服务
mvn package -pl auth-service,user-service,product-service,chat-service,gateway,client -DskipTests -q
```

### 配置注意事项

1. **服务排除数据源**：所有 `@SpringBootApplication` 必须排除 `DataSourceAutoConfiguration`、`DataSourceTransactionManagerAutoConfiguration` 和 `DruidDataSourceAutoConfigure`，同时配置 `@MapperScan`
2. **ShardingSphere + Druid 兼容**：Seata 已移除，每个服务保持 `seata.enabled: false` 和 `enable-auto-data-source-proxy: false`，避免 CGLIB 代理冲突
3. **Hutool HTTP 请求**：使用 `HttpRequest.post(url).execute()` 静态方法，而非 `HttpUtil.post()`
4. **配置文件**：本地开发使用 `application.yml`，Docker 部署使用 `application-docker.yml`（覆盖连接地址为容器名）

### 客户端功能入口

| 类 | 触发条件 | 功能 |
|---|---|---|
| `LoginFrame` | 启动类 | 登录/注册，根据 role 跳转不同主界面 |
| `MainFrame` | 普通用户登录 | 商品列表（分页滚动）、搜索、查看详情、聊天 |
| `MyProductFrame` | MainFrame → 我的商品 | 上架/修改/删除商品，查看询问买家 |
| `AdminFrame` | 管理员登录 | 搜索用户、管理用户、查看用户商品并删除 |
| `ChatFrame` | 从商品详情打开 | 聊天历史、发送消息、3 秒轮询 |

## 常见问题

### 中文乱码
MySQL 默认字符集为 `latin1`，启动时需指定 `--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci`。

### 商品列表为空
分页查询默认排除当前登录用户的商品。如需查看自己的商品，使用 `/product/my` 接口。

### 聊天消息不实时
客户端采用轮询机制，每 3 秒轮询一次 `/chat/poll?lastMsgId=N&otherUserId=M`。确保两个参数都正确传递。

### Docker 下 Nacos 健康检查失败
Nacos 2.x 的健康检查路径为 `/nacos/actuator/health`，非 `/nacos/v1/ns/health`。

### 端口冲突
本地运行时各服务端口为 8081-8088，Docker 部署时 MySQL/Redis/Nacos 分别映射到 3307/6380/8849，避免与本机已有服务冲突。

---

## License

[MIT License](LICENSE)
