# jsj-admin - 后台管理后端

基于 RuoYi 框架的后台管理系统后端。

## 技术栈

- Java 21 + Spring Boot 3.5
- Spring Security + JWT
- MyBatis + PageHelper
- Redis（会话、缓存）
- Druid（连接池、监控）
- SpringDoc OpenAPI（API 文档）

## 启动

```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或在 IDE 中运行 RuoYiApplication.java
```

默认端口：`8080`

## 配置文件

| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置（端口、token、通用设置） |
| `application-dev.yml` | 开发环境（数据库、Redis） |
| `application-prod.yml` | 生产环境 |
| `application-druid.yml` | Druid 数据源配置 |
| `logback.xml` | 日志配置 |

## API 文档

启动后访问：http://localhost:8080/swagger-ui.html

## 默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 超级管理员 |
