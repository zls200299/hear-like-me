# jsj-mini - 小程序后端

轻量级微信小程序后端服务。

## 技术栈

- Java 21 + Spring Boot 3.5
- MyBatis-Plus 3.5
- Redis（Token 存储）
- Druid（连接池）

## 启动

```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或在 IDE 中运行 MiniApplication.java
```

默认端口：`8081`

## 配置文件

| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置（端口、通用设置） |
| `application-dev.yml` | 开发环境（数据库、Redis、微信配置） |
| `application-prod.yml` | 生产环境 |

## 接口列表

| 方法 | 路径 | 说明 | 需登录 |
|------|------|------|--------|
| POST | `/miniapi/auth/wx-login` | 微信登录 | 否 |
| GET | `/miniapi/auth/current-user` | 获取当前用户 | 是 |
| POST | `/miniapi/auth/logout` | 退出登录 | 是 |

## 认证方式

请求头携带 Token（以下任一方式）：
- `Authorization: Bearer <token>`
- `token: <token>`
- `access-token: <token>`

Token 存储在 Redis，有效期 30 天。
