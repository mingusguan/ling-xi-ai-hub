import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

/**
 * PC Web 用户端构建配置。
 *
 * 开发环境把 /api 反向代理到本机模块化单体，生产环境由入口层按同一前缀转发，
 * 因此客户端始终使用相对路径，不区分环境硬编码后端地址。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.LINGXI_API_TARGET ?? 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false
  }
});
