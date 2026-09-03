import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

function createRewrite(prefix: string, replacement: string) {
  return (requestPath: string) => requestPath.startsWith(prefix)
    ? replacement + requestPath.slice(prefix.length)
    : requestPath
}

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  const {
    VITE_APP_ENV,
    VITE_APP_BASE_API = '/dev-api',
    VITE_APP_MINI_API = '/mini-api',
    VITE_ADMIN_PROXY_TARGET = 'http://127.0.0.1:8080',
    VITE_MINI_PROXY_TARGET = 'http://127.0.0.1:8081',
    VITE_ADMIN_PROXY_REWRITE = '',
    VITE_MINI_PROXY_REWRITE = '',
    VITE_PROXY_SECURE = 'true'
  } = env
  const proxySecure = VITE_PROXY_SECURE !== 'false'
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === 'production' ? '/admin/' : '/',
    plugins: createVitePlugins(env, command === 'build'),
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    // 打包配置
    build: {
      // https://vite.dev/config/build-options.html
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    // vite 相关配置
    server: {
      port: 80,
      host: true,
      open: true,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        [VITE_APP_BASE_API]: {
          target: VITE_ADMIN_PROXY_TARGET,
          changeOrigin: true,
          secure: proxySecure,
          rewrite: createRewrite(VITE_APP_BASE_API, VITE_ADMIN_PROXY_REWRITE)
        },
        [VITE_APP_MINI_API]: {
          target: VITE_MINI_PROXY_TARGET,
          changeOrigin: true,
          secure: proxySecure,
          rewrite: createRewrite(VITE_APP_MINI_API, VITE_MINI_PROXY_REWRITE)
        },
         // springdoc proxy
         '^/v3/api-docs/(.*)': {
          target: VITE_ADMIN_PROXY_TARGET,
          changeOrigin: true,
          secure: proxySecure,
        }
      }
    },
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule: any) => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})

