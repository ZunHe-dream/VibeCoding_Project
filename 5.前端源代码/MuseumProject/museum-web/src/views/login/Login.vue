<template>
  <div class="login-page">
    <div class="bg-overlay"></div>

    <div class="login-wrapper">
      <div class="login-brand">
        <div class="brand-content">
          <div class="logo-circle">🏛️</div>
          <h1 class="brand-title">智慧博览</h1>
          <div class="brand-divider"></div>
          <p class="brand-slogan">识古今之变，鉴人文之光</p>
        </div>
      </div>

      <div class="login-form-area">
        <div class="form-header">
          <h2>登 录</h2>
          <p>Login to Heritage System</p>
        </div>

        <el-form :model="loginForm" class="custom-form">
          <el-form-item>
            <el-input v-model="loginForm.username" placeholder="请输入账号" :prefix-icon="User" class="museum-input"
              @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password
              class="museum-input" @keyup.enter="handleLogin" />
          </el-form-item>

          <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">
            开启文化之旅
          </el-button>
        </el-form>

        <div class="form-footer">
          <el-link :underline="false">忘记密码</el-link>
          <el-link type="primary" :underline="false" @click="router.push('/register')">注册新账号</el-link>
        </div>
      </div>
    </div>

    <div class="museum-footer">
      <p>© 2026 文物巡展管理系统 · 典藏文明 传承未来</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    return ElMessage.warning('请输入账号和密码')
  }

  loading.value = true
  try {
    const userData = await request.post('/user/login', loginForm)
    console.log('后端返回的原始数据:', userData) // <--- 加这一行
    ElMessage.success('欢迎回来，' + (userData.nickname || userData.username))
    sessionStorage.setItem('user', JSON.stringify(userData)) // 统一使用 'user' 键名

    if (userData.role === 'ADMIN') {
      router.push('/admin/audit')
    } else if (userData.role === 'APPLICANT') {
      router.push('/planner/apply')
    } else {
      router.push('/user/exhibition')
    }
  } catch (error) {
    console.error('登录流程出错:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 全局背景：采用绢布色/米白色 */
.login-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f4f1de;
  /* 绢布背景色 */
  background-image: url('https://www.transparenttextures.com/patterns/paper-fibers.png');
  /* 纸张质感 */
  position: relative;
  overflow: hidden;
}

/* 装饰性遮罩（可以放一个淡的水墨画图片） */
.bg-overlay {
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at center, transparent, rgba(139, 26, 26, 0.05));
  pointer-events: none;
}

/* 登录容器：左右结构 */
.login-wrapper {
  display: flex;
  width: 850px;
  height: 500px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 10;
}

/* 左侧品牌区：朱砂红 */
.login-brand {
  flex: 1.2;
  background: linear-gradient(135deg, #8b1a1a, #5c1111);
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  position: relative;
}

.brand-content {
  text-align: center;
}

.logo-circle {
  font-size: 50px;
  background: rgba(255, 255, 255, 0.1);
  width: 100px;
  height: 100px;
  line-height: 100px;
  border-radius: 50%;
  margin: 0 auto 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.brand-title {
  font-size: 32px;
  font-family: "Hiragino Sans GB", "Microsoft YaHei", serif;
  letter-spacing: 4px;
  margin-bottom: 10px;
}

.brand-divider {
  width: 40px;
  height: 3px;
  background: #fdbb2d;
  margin: 15px auto;
}

.brand-slogan {
  font-size: 16px;
  opacity: 0.8;
  letter-spacing: 2px;
}

/* 右侧表单区 */
.login-form-area {
  flex: 1.5;
  padding: 50px 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 40px;
}

.form-header h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}

.form-header p {
  color: #999;
  font-size: 12px;
  letter-spacing: 1px;
}

/* 输入框定制 */
.museum-input :deep(.el-input__wrapper) {
  background-color: #f9f9f9;
  box-shadow: none;
  border-bottom: 2px solid #eee;
  border-radius: 0;
  padding: 5px 0;
}

.museum-input :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: #8b1a1a;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 45px;
  background-color: #8b1a1a !important;
  border-color: #8b1a1a !important;
  font-size: 16px;
  letter-spacing: 4px;
  margin-top: 20px;
  border-radius: 4px;
}

.login-btn:hover {
  background-color: #a52a2a !important;
  box-shadow: 0 4px 12px rgba(139, 26, 26, 0.3);
}

.form-footer {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.museum-footer {
  margin-top: 30px;
  color: #8b8b8b;
  font-size: 13px;
  z-index: 10;
}
</style>
