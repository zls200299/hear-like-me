// 配置文件
// 真机调试时请把 baseUrl 改为电脑局域网 IP，例如 http://192.168.1.100:8081
// const baseUrl = 'http://192.168.1.4:8081'
const baseUrl = 'https://43.143.30.134'

module.exports = {
  baseUrl,
  wsBaseUrl: baseUrl.replace(/^http:/, 'ws:').replace(/^https:/, 'wss:'),

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
