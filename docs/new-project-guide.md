# 新项目复制使用指南

本脚手架设计为「复制即用」。新项目只需复制一份仓库，修改以下配置即可快速启动。

## 修改清单

### 1. 项目名称（全局替换 `jsj`）

| 位置 | 当前值 | 改为 |
|------|--------|------|
| 目录名 `jsj-admin` | jsj-admin | `你的前缀-admin` |
| 目录名 `jsj-admin-ui` | jsj-admin-ui | `你的前缀-admin-ui` |
| 目录名 `jsj-mini` | jsj-mini | `你的前缀-mini` |
| 目录名 `jsj-mini-app` | jsj-mini-app | `你的前缀-mini-app` |
| `jsj-admin/pom.xml` artifactId | jsj-admin | 你的前缀-admin |
| `jsj-mini/pom.xml` artifactId | jsj-mini | 你的前缀-mini |
| `jsj-mini/application.yml` spring.application.name | jsj-mini | 你的前缀-mini |
| Redis 缓存前缀 `jsj:mini:` | jsj | 你的前缀 |

### 2. 数据库

| 配置文件 | 配置项 | 说明 |
|---------|--------|------|
| `jsj-admin/src/main/resources/application-dev.yml` | spring.datasource.druid.master.url | 修改数据库地址和库名 |
| `jsj-admin/src/main/resources/application-dev.yml` | spring.datasource.druid.master.username/password | 修改用户名密码 |
| `jsj-mini/src/main/resources/application-dev.yml` | spring.datasource.url | 修改数据库地址和库名 |
| `jsj-mini/src/main/resources/application-dev.yml` | spring.datasource.username/password | 修改用户名密码 |

### 3. Redis

| 配置文件 | 配置项 |
|---------|--------|
| `jsj-admin/src/main/resources/application.yml` | spring.data.redis.host / password |
| `jsj-mini/src/main/resources/application-dev.yml` | spring.data.redis.host / password |

### 4. 端口

| 服务 | 配置文件 | 默认端口 |
|------|---------|---------|
| 后台后端 | `jsj-admin/application.yml` → server.port | 8080 |
| 后台前端 | `jsj-admin-ui/vite.config.ts` → server.port | 80 |
| 小程序后端 | `jsj-mini/application.yml` → server.port | 8081 |

### 5. 微信小程序配置

| 配置文件 | 配置项 |
|---------|--------|
| `jsj-mini/src/main/resources/application-dev.yml` | wx.mini.app-id / app-secret |
| `jsj-mini-app/project.config.json` | appid |

### 6. 前端代理地址

| 配置文件 | 配置项 | 说明 |
|---------|--------|------|
| `jsj-admin-ui/.env.development` | VITE_APP_PROXY_TARGET | 后台后端地址 |
| `jsj-mini-app/config.js` | baseUrl | 小程序后端地址 |

### 7. JWT 密钥

| 配置文件 | 配置项 | 说明 |
|---------|--------|------|
| `jsj-admin/application.yml` | token.secret | 至少 64 字符，每个项目应不同 |

### 8. 可选：修改包名

如果需要修改 Java 包名（`com.zhs` → 你的包名）：

1. IDE 全局重命名包 `com.zhs` → `com.yourcompany`
2. 修改 `pom.xml` 中的 `groupId`
3. 修改 `application.yml` 中的 `mybatis.typeAliasesPackage`
4. 修改 `logback.xml` 中的 logger name

> 注意：包名修改涉及文件较多，建议使用 IDE 的重构功能批量操作。

## 快速检查表

新项目启动前，确认以下事项：

- [ ] 数据库已创建并导入 SQL
- [ ] `application-dev.yml` 数据库连接信息已修改
- [ ] Redis 地址已修改
- [ ] JWT 密钥已更换
- [ ] 微信 AppID / AppSecret 已配置（如需小程序）
- [ ] 前端代理地址已配置
- [ ] 能正常登录后台（admin / admin123）
