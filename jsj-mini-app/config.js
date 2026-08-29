// 配置文件
module.exports = {
  baseUrl: 'http://localhost:8081',

  /**
   * 是否展示开发入口：登录页底部提示、消息页右下角「⇄」、长按标题切测试号。
   * 正式/提审包务必为 false。
   */
  testLoginEnabled: false,

  wx: {
    appId: 'your-appid-here',
    appSecret: 'your-appsecret-here'
  }
}
