# UniApp 多端扩展架构设计文档

> 版本：v1.0 | 日期：2026-04-23 | 作者：架构组

---

## 一、架构设计

### 1.1 新增模块定位

在 `scaffolding-v2/` 下新增 `jsj-uni-app/` 模块，作为 UniApp 多端前端（App + H5）。

```
scaffolding-v2/
├── generator/          # 代码生成器
├── jsj-admin/          # 后台管理后端
├── jsj-admin-ui/       # 管理端前端
├── jsj-mini/           # 小程序 + UniApp 共用后端  ← 复用，小幅改造
├── jsj-mini-app/       # 微信小程序前端
├── jsj-uni-app/        # 【新增】UniApp 前端（App + H5）
├── sql/
└── docs/
```

### 1.2 整体架构关系

```
                        ┌──────────────────┐
  jsj-admin-ui ────────>│    jsj-admin     │────> MySQL (ry-vue) + Redis
  (Vue 3)               │  (Spring Boot 3)  │
                        └──────────────────┘

                        ┌──────────────────┐
  jsj-mini-app ────────>│                  │
  (微信小程序)           │    jsj-mini      │────> MySQL (jsj_mini) + Redis
                        │  (Spring Boot 3)  │
  jsj-uni-app  ────────>│                  │
  (App + H5)            └──────────────────┘
                              ↑
  generator ─────────────────-┘ 生成后端 + UniApp 前端代码
```

核心原则：**不新建后端**，jsj-mini 同时服务微信小程序和 UniApp 端。

### 1.3 API 分层建议

**推荐方案：按功能分模块，不按终端分路径。**

```
/api/auth/wx-login          ← 微信小程序专用
/api/auth/phone-login       ← 【新增】手机号登录（UniApp 用）
/api/auth/current-user      ← 通用
/api/auth/logout             ← 通用

/api/user/**                 ← 通用用户接口
/api/xxx/**                  ← 业务接口，全端通用
```

不建议 `/api/app/**` 和 `/api/mini/**` 分开的原因：
- 业务逻辑完全一致，分开会导致大量重复代码
- 终端差异仅在登录方式，不在业务接口

**终端识别方案：通过请求头区分**

```
X-Client-Type: mini-app     ← 微信小程序
X-Client-Type: uni-app      ← UniApp (App)
X-Client-Type: uni-h5       ← UniApp (H5)
X-App-Version: 1.0.0        ← 客户端版本号
```

### 1.4 鉴权调整

当前 jsj-mini 使用 UUID Token + Redis + DB 三重验证，**无需改为 JWT**，理由：
- 现有方案已支持多端（Token 与平台无关）
- Redis 可控制在线状态、强制下线
- 改 JWT 收益低、成本高

需要新增的鉴权能力：

| 项目 | 说明 |
|------|------|
| 手机号登录 | UniApp 不走微信 jscode2session，需要短信验证码登录 |
| 设备类型记录 | `user_login_session.deviceType` 区分终端来源 |
| 多端同时在线 | 同一用户允许小程序和 App 各持有一个有效 Token |

---

## 二、后端复用评估（jsj-mini 改造方案）

### 2.1 可直接复用的部分

| 模块 | 说明 |
|------|------|
| Token 体系 | UUID Token + Redis，与平台无关，直接复用 |
| 拦截器 | `LoginInterceptor` 从 Header 取 Token，不依赖微信，直接复用 |
| `@NoLoginRequest` 注解 | 免登标记，直接复用 |
| `Result<T>` 响应包装 | 通用返回结构，直接复用 |
| 全局异常处理 | `GlobalExceptionHandler`，直接复用 |
| 所有业务 Service/Mapper | 与终端无关，直接复用 |
| Redis 缓存层 | Token 存储和查询，直接复用 |

### 2.2 需要改造的部分（最小改造清单）

#### 改造点 1：新增手机号登录接口

当前只有 `wx-login`，UniApp 需要手机号 + 验证码登录。

```
新增文件：
  controller/AuthController.java     ← 如果还没有 Controller，需要补上
  service/SmsService.java            ← 短信验证码发送/校验
  request/PhoneLoginReq.java         ← { phone, code }

新增接口：
  POST /api/auth/send-code           ← 发送验证码
  POST /api/auth/phone-login         ← 手机号登录

拦截器排除：
  /api/auth/send-code
  /api/auth/phone-login
```

#### 改造点 2：User 表扩展

当前 `user.sourceType` 已支持多来源（1=admin, 2=phone, 3=mini, 4=公众号, 5=email），无需改表结构。

`user_sensitive_info` 表新增字段：

```sql
ALTER TABLE user_sensitive_info ADD COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '手机号';
```

#### 改造点 3：终端标识透传

在 `LoginInterceptor` 中解析 `X-Client-Type` 头，写入 `UserContext`：

```java
// UserContext 新增
private static final ThreadLocal<String> CLIENT_TYPE = new ThreadLocal<>();

public static String getClientType() { return CLIENT_TYPE.get(); }
public static void setClientType(String type) { CLIENT_TYPE.set(type); }
```

#### 改造点 4：多端同时在线

当前登录会生成新 Token，但未清除旧 Token。需要按 `deviceType` 管理：
- 同一 `deviceType` 登录时，使旧 Token 失效
- 不同 `deviceType` 允许共存

```java
// AuthServiceImpl.login() 中新增逻辑
sessionMapper.invalidateByUserIdAndDeviceType(userId, deviceType);
```

### 2.3 改造影响评估

| 改造项 | 影响范围 | 风险 | 工作量 |
|--------|---------|------|--------|
| 新增手机号登录 | 新增文件，不改现有代码 | 低 | 1天 |
| user_sensitive_info 加字段 | DDL，不影响现有字段 | 低 | 0.5小时 |
| 终端标识透传 | 改 LoginInterceptor + UserContext | 低 | 2小时 |
| 多端在线管理 | 改 AuthServiceImpl | 中 | 0.5天 |

**总计约 2 天工作量，零破坏性改动。**

---

## 三、UniApp 项目骨架设计

### 3.1 完整目录结构

```
jsj-uni-app/
├── src/
│   ├── api/                          # 接口层
│   │   ├── index.js                  # 统一导出
│   │   ├── auth.js                   # 登录/鉴权接口
│   │   └── user.js                   # 用户接口（示例）
│   ├── config/
│   │   └── index.js                  # 环境配置（baseUrl 等）
│   ├── pages/
│   │   ├── index/
│   │   │   └── index.vue             # 首页
│   │   └── login/
│   │       └── index.vue             # 登录页
│   ├── store/
│   │   ├── index.js                  # Pinia 入口
│   │   └── modules/
│   │       └── user.js               # 用户状态（token、userInfo）
│   ├── utils/
│   │   ├── request.js                # 请求封装（核心）
│   │   ├── auth.js                   # Token 管理工具
│   │   └── platform.js               # 平台判断工具
│   ├── static/                       # 静态资源
│   ├── App.vue                       # 根组件
│   ├── main.js                       # 入口
│   ├── pages.json                    # 页面路由配置
│   ├── manifest.json                 # UniApp 应用配置
│   └── uni.scss                      # 全局样式变量
├── .env.development                  # 开发环境变量
├── .env.production                   # 生产环境变量
├── index.html
├── package.json
├── vite.config.js
└── README.md
```

### 3.2 核心文件设计

#### 3.2.1 请求封装 `src/utils/request.js`

```javascript
import { getToken, clearAuth } from './auth'
import { getClientType } from './platform'
import config from '@/config'

const request = (options) => {
  const { url, method = 'POST', data, header = {} } = options

  const token = getToken()
  const defaultHeader = {
    'content-type': 'application/json',
    'X-Client-Type': getClientType(),
    'X-App-Version': config.appVersion
  }
  if (token) {
    defaultHeader['Authorization'] = `Bearer ${token}`
    defaultHeader['token'] = token
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: config.baseUrl + url,
      method,
      data,
      header: { ...defaultHeader, ...header },
      success: (res) => {
        const { statusCode, data: resData } = res
        if (statusCode === 200) {
          // 业务层判断
          if (resData.code === 200) {
            resolve(resData.data)
          } else if (resData.code === 401) {
            clearAuth()
            uni.reLaunch({ url: '/pages/login/index' })
            reject(resData)
          } else {
            uni.showToast({ title: resData.msg || '请求失败', icon: 'none' })
            reject(resData)
          }
        } else {
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

export const get = (url, data) => request({ url, method: 'GET', data })
export const post = (url, data) => request({ url, method: 'POST', data })

export default request
```

#### 3.2.2 Token 管理 `src/utils/auth.js`

```javascript
const TOKEN_KEY = 'jsj_token'
const USER_KEY = 'jsj_user_info'

export const getToken = () => uni.getStorageSync(TOKEN_KEY) || ''

export const setToken = (token) => uni.setStorageSync(TOKEN_KEY, token)

export const getUserInfo = () => {
  const raw = uni.getStorageSync(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export const setUserInfo = (info) => {
  uni.setStorageSync(USER_KEY, JSON.stringify(info))
}

export const clearAuth = () => {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

export const isLoggedIn = () => !!getToken()
```

#### 3.2.3 平台判断 `src/utils/platform.js`

```javascript
export const getClientType = () => {
  // #ifdef APP-PLUS
  return 'uni-app'
  // #endif
  // #ifdef H5
  return 'uni-h5'
  // #endif
  // #ifdef MP-WEIXIN
  return 'mini-app'
  // #endif
  return 'unknown'
}

export const isApp = () => {
  // #ifdef APP-PLUS
  return true
  // #endif
  return false
}

export const isH5 = () => {
  // #ifdef H5
  return true
  // #endif
  return false
}
```

#### 3.2.4 环境配置 `src/config/index.js`

```javascript
const ENV = {
  development: {
    baseUrl: 'http://localhost:8081',
    appVersion: '1.0.0-dev'
  },
  production: {
    baseUrl: 'https://api.yoursite.com',
    appVersion: '1.0.0'
  }
}

const currentEnv = process.env.NODE_ENV || 'development'

export default ENV[currentEnv]
```

#### 3.2.5 API 接口层 `src/api/auth.js`

```javascript
import { post } from '@/utils/request'

// 发送短信验证码
export const sendCode = (phone) => post('/api/auth/send-code', { phone })

// 手机号登录
export const phoneLogin = (phone, code) => post('/api/auth/phone-login', { phone, code })

// 获取当前用户信息
export const getCurrentUser = () => post('/api/auth/current-user')

// 退出登录
export const logout = () => post('/api/auth/logout')
```

#### 3.2.6 用户状态管理 `src/store/modules/user.js`

```javascript
import { defineStore } from 'pinia'
import { getToken, setToken, clearAuth, getUserInfo, setUserInfo } from '@/utils/auth'
import { phoneLogin, getCurrentUser, logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: getUserInfo()
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    nickname: (state) => state.userInfo?.nickname || ''
  },

  actions: {
    async login(phone, code) {
      const res = await phoneLogin(phone, code)
      this.token = res.token
      this.userInfo = res
      setToken(res.token)
      setUserInfo(res)
      return res
    },

    async fetchUser() {
      const res = await getCurrentUser()
      this.userInfo = res
      setUserInfo(res)
    },

    async logout() {
      try { await logoutApi() } catch (e) { /* ignore */ }
      this.token = ''
      this.userInfo = null
      clearAuth()
      uni.reLaunch({ url: '/pages/login/index' })
    }
  }
})
```

#### 3.2.7 页面路由 `src/pages.json`

```json
{
  "pages": [
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "首页" } },
    { "path": "pages/login/index", "style": { "navigationBarTitleText": "登录" } }
  ],
  "globalStyle": {
    "navigationBarTextStyle": "black",
    "navigationBarTitleText": "JSJ",
    "navigationBarBackgroundColor": "#ffffff",
    "backgroundColor": "#f8f8f8"
  },
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#333333",
    "list": [
      { "pagePath": "pages/index/index", "text": "首页" }
    ]
  }
}
```

---

## 四、Generator 扩展设计

### 4.1 扩展思路

在现有 generator 的 Freemarker 模板体系中，新增 UniApp 页面模板。生成流程复用现有管线：

```
数据库表 → 读取字段元数据 → 用户配置 → GeneratorContent → UniApp 模板 → .vue 文件
```

### 4.2 新增模板文件

在 `generator/src/main/resources/template/` 下新增：

```
template/
├── zhsEntity.ftl              # 已有
├── zhsController.ftl          # 已有
├── ...
├── uniList.ftl                # 【新增】列表页模板
├── uniDetail.ftl              # 【新增】详情页模板
├── uniForm.ftl                # 【新增】表单页模板（新增/编辑）
└── uniApi.ftl                 # 【新增】API 接口文件模板
```

### 4.3 模板数据模型

复用现有 `GeneratorContent`，模板中可直接使用：

```freemarker
${content.entity.className}          → "User"
${content.entity.classNameLower}     → "user"
${content.entity.tableName}          → "user"
${content.entity.attrs}              → 字段列表
${content.entity.primaryKey}         → "id"

遍历字段：
<#list content.entity.attrs as attr>
  ${attr.field}                      → "nickname"
  ${attr.fieldPascal}                → "Nickname"
  ${attr.javaType}                   → "String"
  ${attr.remarks}                    → "用户昵称"
  ${attr.nullable}                   → true/false
</#list>
```

### 4.4 模板结构设计

#### uniList.ftl — 列表页

```
生成文件：pages/${content.entity.classNameLower}/list.vue

模板结构：
<template>
  - 搜索栏（遍历 queryReq 字段生成搜索条件）
  - 列表区域（uni-list / scroll-view）
    - 每行展示关键字段（取前 3-4 个非主键字段）
  - 下拉刷新 + 上拉加载
  - 新增按钮（跳转 form 页）
</template>

<script setup>
  - 引入 api/${classNameLower}.js
  - 分页查询逻辑
  - 搜索/重置
  - 删除（滑动删除或长按）
  - 跳转详情/编辑
</script>

调用接口：
  GET /api/${classNameLower}/getByPage
  DELETE /api/${classNameLower}/delete/{id}
```

#### uniDetail.ftl — 详情页

```
生成文件：pages/${content.entity.classNameLower}/detail.vue

模板结构：
<template>
  - 字段展示区（遍历 resp/detailResp 字段）
    - 每个字段一行：label + value
  - 操作按钮（编辑 / 删除）
</template>

<script setup>
  - 接收 id 参数
  - 调用 getById 接口
  - 编辑跳转 / 删除确认
</script>

调用接口：
  GET /api/${classNameLower}/getById/{id}
```

#### uniForm.ftl — 表单页（新增 + 编辑）

```
生成文件：pages/${content.entity.classNameLower}/form.vue

模板结构：
<template>
  <uni-forms>
    遍历 addReq 字段生成表单项：
    - String → <uni-easyinput>
    - Integer/Long → <uni-easyinput type="number">
    - Date → <uni-datetime-picker>
    - Boolean → <switch>
    必填字段添加 rules 校验
  </uni-forms>
  - 提交按钮
</template>

<script setup>
  - 有 id → 编辑模式，先加载数据
  - 无 id → 新增模式
  - 表单校验 + 提交
</script>

调用接口：
  POST /api/${classNameLower}/addOrUpdate
  GET /api/${classNameLower}/getById/{id}  (编辑时)
```

#### uniApi.ftl — API 接口文件

```
生成文件：api/${classNameLower}.js

模板输出：
import { get, post } from '@/utils/request'

export const getByPage = (params) => get('/api/${classNameLower}/getByPage', params)
export const getById = (id) => get('/api/${classNameLower}/getById/' + id)
export const addOrUpdate = (data) => post('/api/${classNameLower}/addOrUpdate', data)
export const deleteById = (id) => post('/api/${classNameLower}/delete/' + id)
export const deleteByIds = (ids) => post('/api/${classNameLower}/deleteByIds', { idList: ids })
```

### 4.5 Java 类型到 UniApp 组件映射

模板中需要的类型映射逻辑：

| Java 类型 | 表单组件 | 列表展示 |
|-----------|---------|---------|
| String | `<uni-easyinput>` | 直接显示 |
| Integer / Long | `<uni-easyinput type="number">` | 直接显示 |
| BigDecimal | `<uni-easyinput type="digit">` | 保留两位小数 |
| Date | `<uni-datetime-picker>` | 格式化日期 |
| Boolean | `<switch>` | 是/否 |

### 4.6 Generator 代码改造点

需要在 generator 中新增的 Java 代码：

```
1. entity/UniAppContent.java          ← UniApp 生成配置（输出目录、页面路径等）
2. controller/SetUniAppController.java ← UI 配置面板
3. FXMLPage.java 新增枚举值            ← UniApp 配置页面
4. IndexController.java 新增生成逻辑   ← 调用 CreateFileUtil 生成 .vue 文件
5. ConfigUtil.java 新增存取方法         ← 保存 UniApp 配置到 SQLite
```

生成调用示例（在 IndexController 中）：

```java
// 生成 UniApp 列表页
CreateFileUtil.createFile(
    content,
    "uniList.ftl",
    uniAppProjectPath + "/src",     // UniApp 项目 src 目录
    "pages/" + classNameLower,       // 页面目录
    "list.vue",
    "UTF-8",
    isOverride
);
```

---

## 五、未来扩展建议

### 5.1 多端统一策略

```
当前：
  微信小程序 → jsj-mini-app (原生)
  App + H5   → jsj-uni-app  (UniApp)

未来可选演进：
  方案 A：UniApp 统一所有端（推荐）
    jsj-uni-app 编译到微信小程序 + App + H5
    逐步废弃 jsj-mini-app

  方案 B：保持双前端
    jsj-mini-app 保留（微信生态深度集成场景）
    jsj-uni-app 覆盖 App + H5
```

推荐方案 A，因为 UniApp 对微信小程序的编译支持已经很成熟，维护一套代码成本更低。

### 5.2 是否需要 BFF 层

**当前阶段：不需要。**

理由：
- jsj-mini 本身就是轻量 API 服务，已经是面向前端的接口
- 没有多后端聚合需求（不需要同时调 jsj-admin 和 jsj-mini）
- 引入 BFF 增加运维复杂度，收益不明显

**何时需要 BFF：**
- 前端需要聚合多个后端服务的数据
- 需要针对不同终端做接口裁剪（返回不同字段）
- 接口数量超过 100+ 且终端差异大

### 5.3 是否需要网关

**当前阶段：不需要。**

理由：
- 只有两个后端服务（jsj-admin 8080, jsj-mini 8081），Nginx 反向代理足够
- 没有服务发现、限流、熔断等微服务治理需求

**推荐的 Nginx 配置：**

```nginx
server {
    listen 443 ssl;

    # 管理端 API
    location /admin-api/ {
        proxy_pass http://127.0.0.1:8080/;
    }

    # 小程序 + UniApp API
    location /api/ {
        proxy_pass http://127.0.0.1:8081/api/;
    }

    # 管理端前端
    location /admin/ {
        root /path/to/jsj-admin-ui/dist;
    }

    # H5 前端
    location / {
        root /path/to/jsj-uni-app/dist/build/h5;
        try_files $uri $uri/ /index.html;
    }
}
```

**何时需要网关（Spring Cloud Gateway / Kong）：**
- 后端服务拆分为 3 个以上微服务
- 需要统一鉴权、限流、灰度发布
- 需要服务注册与发现

---

## 六、实施路线图

```
Phase 1（2天）：jsj-mini 后端改造
  ├─ 新增手机号登录接口
  ├─ 终端标识透传
  ├─ 多端在线管理
  └─ user_sensitive_info 加 phone 字段

Phase 2（1天）：jsj-uni-app 项目骨架
  ├─ 初始化 UniApp 项目
  ├─ 请求封装 + Token 管理
  ├─ 登录页 + 首页
  └─ 环境配置

Phase 3（2天）：Generator 扩展
  ├─ 新增 4 个 Freemarker 模板
  ├─ 新增 UniApp 配置面板
  └─ 联调生成 → 运行验证

Phase 4（持续）：业务页面开发
  └─ 使用 Generator 生成 CRUD 页面，按需调整
```
