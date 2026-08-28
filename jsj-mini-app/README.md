# jsj-mini-app - 微信小程序前端

原生微信小程序，配合 jsj-mini 后端使用。

## 启动

1. 打开微信开发者工具
2. 导入本目录
3. 填写 AppID（或使用测试号）
4. 确保 jsj-mini 后端已在 `localhost:8081` 启动

## 配置

编辑 `config.js`：

```javascript
module.exports = {
  baseUrl: 'http://localhost:8081',  // 后端地址
  testLoginEnabled: false,           // 开发入口开关
}
```

## 目录结构

```
jsj-mini-app/
├── api/                # API 接口
├── components/         # 通用组件
├── pages/              # 页面
├── utils/
│   └── request.js      # 请求封装
├── app.js              # 应用入口
├── app.json            # 应用配置
└── config.js           # 环境配置
```

## 已集成能力

- 微信登录（wx.login → 后端换 token）
- 统一请求封装（自动携带 token、业务码检查、401 处理）
- 本地登录态持久化（wx.Storage）
