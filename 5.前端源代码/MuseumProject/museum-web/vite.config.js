import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 设置 @ 指向 src 目录，这样写路径更方便
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173, // 前端运行端口
    proxy: {
      // 这里的 /api 是前端请求的暗号
      '/api': {
        target: 'http://localhost:8080', // 你的后端地址
        changeOrigin: true, // 允许跨域
        rewrite: (path) => path.replace(/^\/api/, '') // 发给后端时去掉 /api 前缀
      }
    }
  }
})