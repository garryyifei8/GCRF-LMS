import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import viteCompression from 'vite-plugin-compression'
import { viteMockServe } from 'vite-plugin-mock'

// https://vitejs.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [
    vue({
      script: {
        defineModel: true,
        propsDestructure: true
      }
    }),
    // Vue API 自动导入（不包含 Element Plus，因为已在 main.js 中全局引入）
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      dts: false
    }),
    // Mock 服务 - 已禁用，使用真实后端API
    // viteMockServe({
    //   mockPath: 'mock',
    //   enable: command === 'serve'
    // }),
    // Gzip 压缩
    viteCompression({
      verbose: true,
      disable: false,
      threshold: 10240,
      algorithm: 'gzip',
      ext: '.gz'
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3011,
    open: false,
    // 开发环境启用 source maps 以便调试
    sourcemap: true,
    proxy: {
      // 认证服务 - auth-service
      '/api/v1/auth': {
        target: 'http://localhost:18081',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 图书服务 - book-service
      '/api/v1/books': {
        target: 'http://localhost:18082',
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/api/v1/categories': {
        target: 'http://localhost:18082',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 库存管理 - book-service
      '/api/v1/inventory': {
        target: 'http://localhost:18082',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 读者服务 - reader-service
      '/api/v1/readers': {
        target: 'http://localhost:18084',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 借阅管理 - circulation-service
      '/api/v1/borrows': {
        target: 'http://localhost:18083',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 预约管理 - circulation-service
      '/api/v1/reserves': {
        target: 'http://localhost:18083',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 旧的circulation接口兼容
      '/api/v1/circulation': {
        target: 'http://localhost:18083',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 罚款管理 - circulation-service
      '/api/v1/fines': {
        target: 'http://localhost:18083',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 统计分析 - analytics-service
      '/api/v1/analytics': {
        target: 'http://localhost:18089',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 系统管理 - system-service
      '/api/v1/system': {
        target: 'http://localhost:18085',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 推荐 - recommend-service
      '/api/v1/recommend': {
        target: 'http://localhost:18086',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 聊天 - chat-service
      '/api/v1/chat': {
        target: 'http://localhost:18087',
        changeOrigin: true,
        rewrite: (path) => path
      },
      // 其他 API 请求代理到 Gateway (fallback)
      '/api': {
        target: 'http://localhost:18080',
        changeOrigin: true,
        rewrite: (path) => path
      }
    }
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'element-plus', '@element-plus/icons-vue']
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    minify: 'esbuild',
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        // 确保正确的chunk加载顺序
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]',
        manualChunks(id) {
          // 将 node_modules 中的包分组
          if (id.includes('node_modules')) {
            // Element Plus 相关单独打包
            if (id.includes('element-plus')) {
              return 'element-plus'
            }
            // ECharts 单独打包
            if (id.includes('echarts')) {
              return 'echarts'
            }
            // Vue 全家桶放在一起，避免循环依赖
            if (id.includes('vue') || id.includes('pinia') || id.includes('@vue')) {
              return 'vue-vendor'
            }
            // 其他依赖
            return 'vendor'
          }
        }
      }
    }
  }
}))
