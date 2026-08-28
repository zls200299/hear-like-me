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
