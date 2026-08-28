# 首次启动指南

## 环境准备

| 工具 | 版本要求 | 用途 |
|------|---------|------|
| JDK | 21+ | 后端运行 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.0+ | 缓存/会话 |
| 微信开发者工具 | 最新版 | 小程序开发 |

## 第一步：初始化数据库

### 1.1 创建数据库

```sql
-- 后台管理系统数据库
CREATE DATABASE `ry-vue` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 小程序数据库
CREATE DATABASE `chat` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 1.2 导入表结构和数据

按顺序执行 `sql/` 目录下的脚本：

```bash
# 1. 后台管理系统基础表 + 初始数据（导入到 ry-vue 库）
mysql -u root -p ry-vue < sql/01_admin_init.sql

# 2. Quartz 定时任务表（导入到 ry-vue 库）
mysql -u root -p ry-vue < sql/02_admin_quartz.sql

# 3. 小程序用户表（导入到 chat 库）
mysql -u root -p chat < sql/03_mini_init.sql
```

## 第二步：修改配置

### 2.1 后台管理后端

编辑 `jsj-admin/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    druid:
      master:
        url: jdbc:mysql://localhost:3306/ry-vue?...
        username: root
        password: 你的密码

  data:
    redis:
      host: localhost
      port: 6379
      password: 你的Redis密码（没有则留空）
```

### 2.2 小程序后端

编辑 `jsj-mini/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chat?...
    username: root
    password: 你的密码

  data:
    redis:
      host: localhost
      port: 6379

wx:
  mini:
    app-id: 你的微信小程序AppId
    app-secret: 你的微信小程序AppSecret
```

### 2.3 后台管理前端

编辑 `jsj-admin-ui/.env.development`：

```properties
VITE_APP_PROXY_TARGET = 'http://localhost:8080'
```

### 2.4 小程序前端

编辑 `jsj-mini-app/config.js`：

```javascript
module.exports = {
  baseUrl: 'http://localhost:8081',  // 小程序后端地址
  // ...
}
```

## 第三步：启动服务

> 按以下顺序启动，确保依赖服务先于应用服务。

### 3.1 确保 MySQL 和 Redis 已启动

```bash
# 检查 MySQL
mysql -u root -p -e "SELECT 1"

# 检查 Redis
redis-cli ping
```

### 3.2 启动后台管理后端（端口 8080）

```bash
cd jsj-admin
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或在 IDE 中运行 `RuoYiApplication.java`，激活 `dev` Profile。

### 3.3 启动后台管理前端（端口 80）

```bash
cd jsj-admin-ui
npm install    # 首次需要
npm run dev
```

浏览器访问 http://localhost:80

**默认管理员账号**：`admin` / `admin123`

### 3.4 启动小程序后端（端口 8081）

```bash
cd jsj-mini
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或在 IDE 中运行 `MiniApplication.java`。

### 3.5 启动小程序前端

1. 打开微信开发者工具
2. 导入项目，选择 `jsj-mini-app` 目录
3. AppID 填写你的小程序 AppID（或使用测试号）

## 常见问题

### Q: 后端启动报数据库连接失败？
A: 检查 MySQL 是否启动，数据库是否创建，application-dev.yml 中的连接信息是否正确。

### Q: 前端启动报端口被占用？
A: 修改 `jsj-admin-ui/vite.config.ts` 中的 `server.port`。

### Q: 小程序请求失败？
A: 1) 确认小程序后端已启动在 8081 端口；2) 微信开发者工具中关闭「不校验合法域名」。

### Q: JWT 报密钥长度不够？
A: 确保 `application.yml` 中 `token.secret` 至少 64 个字符（HS512 要求 >= 512 bits）。
