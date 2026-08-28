# Scaffolding-v2 (脚手架 v2)

全栈多平台脚手架项目，包含代码生成器、后台管理系统、以及微信小程序。

## 项目结构

```
scaffolding-v2/
├── generator/        # 代码生成器 (JavaFX 桌面应用)
├── jsj-admin/        # 后台管理系统 - 后端
├── jsj-admin-ui/     # 后台管理系统 - 前端
├── jsj-mini/         # 小程序服务 - 后端
├── jsj-mini-app/     # 微信小程序 - 前端
├── sql/              # 数据库初始化脚本
└── docs/             # 项目文档
```

## 技术栈

| 模块 | 技术栈 | 说明 |
|------|--------|------|
| **generator** | Java 21 + JavaFX 21 + Freemarker + SQLite | 可视化代码生成器，连接数据库后自动生成 CRUD 代码 |
| **jsj-admin** | Java 21 + Spring Boot 3.5 + MyBatis + Spring Security + JWT + Redis + Quartz | 基于 RuoYi-Vue 架构升级到 Spring Boot 3 的后台管理后端 |
| **jsj-admin-ui** | Vue 3.5 + TypeScript + Vite 6 + Element Plus + Pinia + ECharts | 后台管理前端 |
| **jsj-mini** | Java 21 + Spring Boot 3.5 + MyBatis-Plus 3.5 + Hutool | 小程序专用后端，轻量级 API 服务 |
| **jsj-mini-app** | 原生微信小程序 (WXML / WXSS / JS) | 微信小程序前端 |
| **sql** | MySQL DDL / DML | 两个数据库：`ry-vue`（管理端）和 `jsj_mini`（小程序端） |

## 架构设计

```
┌─────────────────┐     ┌──────────────────┐
│  jsj-admin-ui   │────>│    jsj-admin     │────> MySQL (ry-vue) + Redis
│  (Vue 3 前端)    │     │  (Spring Boot 3)  │
└─────────────────┘     └──────────────────┘

┌─────────────────┐     ┌──────────────────┐
│  jsj-mini-app   │────>│    jsj-mini      │────> MySQL (jsj_mini)
│  (微信小程序)     │     │  (Spring Boot 3)  │
└─────────────────┘     └──────────────────┘

┌─────────────────┐
│    generator    │────> 连接数据库，生成代码到 jsj-admin / jsj-mini
│  (JavaFX 桌面)   │
└─────────────────┘
```

## 核心特点

- **生成器驱动开发** — `generator` 工具连接数据库表，一键生成 Controller / Service / DAO / DTO 等代码，减少重复劳动
- **前后端分离** — 管理端和小程序端都采用前后端分离架构
- **双后端独立部署** — 管理系统 (`jsj-admin`) 和小程序服务 (`jsj-mini`) 解耦，可独立扩展维护
- **现代化技术栈** — 全面使用 Java 21 + Spring Boot 3 + Vue 3，最新 LTS 版本组合
- **基于 RuoYi** — 管理端基于成熟的 RuoYi-Vue 框架升级，自带用户 / 角色 / 权限 / 菜单管理等基础功能

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Redis

### 1. 初始化数据库

执行 `sql/` 目录下的 SQL 脚本，创建 `ry-vue` 和 `jsj_mini` 数据库。

### 2. 启动后台管理后端 (jsj-admin)

```bash
cd jsj-admin
# 修改 application-dev.yml 中的数据库和 Redis 连接信息
mvn spring-boot:run
```

### 3. 启动后台管理前端 (jsj-admin-ui)

```bash
cd jsj-admin-ui
npm install
npm run dev
```

### 4. 启动小程序后端 (jsj-mini)

```bash
cd jsj-mini
# 修改 application-dev.yml 中的数据库连接信息
mvn spring-boot:run
```

### 5. 小程序前端 (jsj-mini-app)

使用微信开发者工具打开 `jsj-mini-app/` 目录。

### 6. 代码生成器 (generator)

```bash
cd generator
mvn javafx:run
```

## 项目文档

更多详细文档请参阅 `docs/` 目录：

- [目录指南](docs/directory-guide.md)
- [快速上手](docs/getting-started.md)
- [新建项目指南](docs/new-project-guide.md)
