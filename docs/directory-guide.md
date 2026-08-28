# 目录结构详解

## 整体结构

```
scaffolding/
├── jsj-admin/              # 后台管理 - 后端
├── jsj-admin-ui/           # 后台管理 - 前端
├── jsj-mini/               # 小程序 - 后端
├── jsj-mini-app/           # 小程序 - 前端
├── sql/                    # 数据库脚本
├── docs/                   # 项目文档
└── README.md               # 项目说明
```

## jsj-admin（后台管理后端）

基于 RuoYi 框架，Spring Boot 3 单体架构。

```
jsj-admin/
├── src/main/java/com/ruoyi/
│   ├── RuoYiApplication.java           # 启动类
│   ├── common/                         # 通用模块
│   │   ├── annotation/                 # 自定义注解（日志、限流、数据权限）
│   │   ├── config/                     # 基础配置
│   │   ├── constant/                   # 常量定义
│   │   ├── core/                       # 核心类（AjaxResult、BaseEntity、分页）
│   │   ├── enums/                      # 枚举
│   │   ├── exception/                  # 异常定义
│   │   ├── filter/                     # Servlet 过滤器
│   │   ├── utils/                      # 工具类
│   │   └── xss/                        # XSS 防护
│   ├── framework/                      # 框架层
│   │   ├── aspectj/                    # AOP 切面（日志、数据权限）
│   │   ├── config/                     # Spring 配置（安全、Redis、MyBatis）
│   │   ├── datasource/                 # 动态数据源
│   │   ├── interceptor/                # 拦截器（防重复提交）
│   │   ├── manager/                    # 异步任务管理
│   │   ├── security/                   # Security 过滤器
│   │   └── web/                        # Web 基类和全局异常处理
│   ├── system/                         # 系统管理模块
│   │   ├── domain/                     # 实体类
│   │   ├── mapper/                     # MyBatis Mapper
│   │   └── service/                    # 业务逻辑
│   ├── quartz/                         # 定时任务模块
│   ├── generator/                      # 代码生成模块
│   └── web/                            # 控制器层
│       └── controller/
│           ├── system/                 # 系统管理接口
│           ├── monitor/                # 监控接口
│           └── tool/                   # 工具接口
└── src/main/resources/
    ├── application.yml                 # 主配置
    ├── application-dev.yml             # 开发环境（数据库、Redis）
    ├── application-prod.yml            # 生产环境
    ├── logback.xml                     # 日志配置
    └── mapper/                         # MyBatis XML
```

## jsj-mini（小程序后端）

轻量级小程序后端，专注于微信登录和用户管理。

```
jsj-mini/
├── src/main/java/com/ruoyi/miniapp/
│   ├── MiniApplication.java            # 启动类
│   ├── common/                         # 通用组件
│   │   ├── Result.java                 # 统一返回结构
│   │   ├── UserContext.java            # 当前用户上下文
│   │   ├── NoLoginRequest.java         # 免登录注解
│   │   └── cachekey/                   # Redis 缓存 Key 体系
│   ├── config/                         # 配置
│   │   ├── RedisConfig.java            # Redis 配置
│   │   ├── MiniWebMvcConfig.java       # MVC + 拦截器
│   │   ├── MiniLoginInterceptor.java   # 登录拦截器
│   │   └── WxMiniProperties.java       # 微信配置属性
│   ├── controller/                     # 接口层
│   ├── domain/                         # 实体
│   ├── dto/                            # 数据传输对象
│   ├── exception/                      # 异常处理
│   ├── mapper/                         # MyBatis-Plus Mapper
│   ├── request/                        # 请求参数
│   ├── response/                       # 响应参数
│   ├── service/                        # 业务逻辑
│   └── util/                           # 工具类
└── src/main/resources/
    ├── application.yml                 # 主配置（端口、通用设置）
    ├── application-dev.yml             # 开发环境（数据库、Redis、微信）
    └── logback.xml                     # 日志配置
```

## jsj-admin-ui（后台管理前端）

Vue 3 + TypeScript + Element Plus 管理后台。

```
jsj-admin-ui/
├── src/
│   ├── api/              # API 接口定义（按模块组织）
│   ├── assets/           # 静态资源
│   ├── components/       # 通用组件
│   ├── directive/        # 自定义指令（权限、复制）
│   ├── layout/           # 页面布局
│   ├── plugins/          # 插件（Element Plus 等）
│   ├── router/           # 路由配置
│   ├── store/            # Pinia 状态管理
│   ├── utils/            # 工具函数（request、auth、validate）
│   └── views/            # 页面视图（按模块组织）
├── .env.development      # 开发环境变量
├── .env.production       # 生产环境变量
└── vite.config.ts        # Vite 配置
```

## jsj-mini-app（微信小程序前端）

原生微信小程序。

```
jsj-mini-app/
├── api/                  # API 接口定义
├── components/           # 通用组件
├── pages/                # 页面
│   ├── index/            # 首页
│   ├── login/            # 登录页
│   └── mine/             # 个人中心
├── utils/                # 工具
│   └── request.js        # 请求封装
├── app.js                # 应用入口
├── app.json              # 应用配置
├── config.js             # 环境配置
└── project.config.json   # 项目配置
```

## sql（数据库脚本）

```
sql/
├── 01_admin_init.sql     # 后台管理系统：表结构 + 基础数据（用户、角色、菜单等）
├── 02_admin_quartz.sql   # Quartz 定时任务框架表
└── 03_mini_init.sql      # 小程序：用户表 + 敏感信息表
```

执行顺序：按文件名前缀数字顺序。01 和 02 导入管理库，03 导入小程序库。
