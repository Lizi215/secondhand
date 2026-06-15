---
name: docker-deployment
description: 二手交易平台 Docker 化部署完整步骤与踩坑记录
metadata: 
  node_type: memory
  type: reference
  originSessionId: 2d4fc454-44ac-4624-910f-4ab4b4b3b1a0
---

# Docker 部署完整流程

适用于 spring-boot + spring-cloud-alibaba + shardingSphere + swing 多模块项目。

## 文件清单（共 7 个新文件）

| 文件 | 位置 | 说明 |
|------|------|------|
| Dockerfile | 项目根目录 | 通用镜像，ARG SERVICE_NAME 参数化 |
| .dockerignore | 项目根目录 | 排除 client/ cn/ target/ 等 |
| docker-compose.yml | 项目根目录 | 8 容器编排 + healthcheck + volumes |
| application-docker.yml ×5 | 各服务 resources/ | 覆盖连接地址为容器名 |

## 配置要点

### application-docker.yml 覆盖内容
- MySQL URL：`127.0.0.1:3306` → `mysql:3306`
- Nacos：`127.0.0.1:8848` → `nacos:8848`
- Redis：`192.168.73.128:6379` → `redis:6379`
- 数据库密码也要覆盖，因为 Docker 内密码可能不同

### Dockerfile 设计
```dockerfile
FROM eclipse-temurin:8-jre       # 不要用 openjdk:8-jre-slim（已弃用）
ARG SERVICE_NAME
COPY ${SERVICE_NAME}/target/${SERVICE_NAME}-1.0.0.jar /app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
```

### docker-compose 关键配置
- MySQL 加 `command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci`
- 健康检查路径：Nacos 用 `/nacos/actuator/health`，不是 `/nacos/v1/ns/health`
- 端口映射避免与本机服务冲突：MySQL→3307, Redis→6380, Nacos→8849
- Swing 客户端不进 Docker，宿主机直连 `localhost:8088`

## 启动步骤

```bash
# 1. 打包 Java 项目
mvn install -N -q
mvn install -pl common -DskipTests -q
mvn package -pl auth-service,user-service,product-service,chat-service,gateway -DskipTests -q

# 2. 构建 Docker 镜像
docker-compose build

# 3. 启动所有容器（后台）
docker-compose up -d

# 4. 启动 Swing 客户端
cd client/target && java -jar client-1.0.0.jar
```

## 常用运维命令

```bash
# 查看状态
docker ps --filter "name=secondhand" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 查看日志
docker logs secondhand-auth -f

# 停止服务（保留数据）
docker-compose down

# 停止服务并删除数据卷（重新初始化）
docker-compose down -v

# 重新启动
docker-compose up -d

# 只重启某个服务
docker-compose restart auth-service
```

## 数据管理

```bash
# 检查数据
docker exec -it secondhand-mysql mysql -uroot -pCHANGED_DOCKER_PASS secondhand_trade -e "SELECT * FROM user_1;"

# 批量导入测试数据
docker exec -i secondhand-mysql mysql -uroot -pCHANGED_DOCKER_PASS secondhand_trade --default-character-set=utf8mb4 < doc/sql/rebuild.sql

# 检查 Redis
docker exec -it secondhand-redis redis-cli -a CHANGED_DOCKER_PASS -n 2 KEYS '*'
```

## 容器服务列表

| 容器名 | 内部端口 | 宿主机端口 | 健康检查 |
|--------|---------|-----------|---------|
| secondhand-mysql | 3306 | 3307 | mysqladmin ping |
| secondhand-redis | 6379 | 6380 | redis-cli ping |
| secondhand-nacos | 8848 | 8849 | curl /nacos/actuator/health |
| secondhand-auth | 8081 | 8081 | — |
| secondhand-user | 8082 | 8082 | — |
| secondhand-product | 8083 | 8083 | — |
| secondhand-chat | 8084 | 8084 | — |
| secondhand-gateway | 8088 | 8088 | — |

## 启动顺序（depends_on）

```
mysql ──→ nacos ──→ auth-service, user-service, chat-service ──┐
redis ───────────── product-service ────────────────────────────┤──→ gateway
```

## 踩坑记录

1. **基础镜像**：`openjdk:8-jre-slim` 在 Docker Hub 已弃用，用 `eclipse-temurin:8-jre`
2. **Nacos 版本**：`2.0.4` tag 不存在，用 `2.0.3`
3. **健康检查路径**：Nacos 2.x 用 `/nacos/actuator/health`
4. **密码覆盖**：application-docker.yml 不仅要改 URL 还要改 password
5. **分片数据**：ShardingSphere MOD 分片 `user_id % 3`，insert 时要插对物理分片表
6. **MySQL 字符集**：默认 latin1，必须加 `command: --character-set-server=utf8mb4`，否则中文乱码
7. **客户端运行**：`client-1.0.0.jar` 需要 `lib/` 目录在旁，要 `cd target/` 再执行
