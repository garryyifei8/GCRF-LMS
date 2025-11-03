<template>
  <div class="login-container">
    <!-- 左侧插画区域 -->
    <div class="login-left">
      <div class="auth-illustration">
        <svg class="auth-svg" viewBox="0 0 500 500" xmlns="http://www.w3.org/2000/svg">
          <!-- 背景装饰圆圈 -->
          <circle cx="100" cy="100" r="60" fill="#E3F2FD" opacity="0.5"/>
          <circle cx="400" cy="400" r="80" fill="#E8EAF6" opacity="0.5"/>

          <!-- 书架 -->
          <rect x="100" y="280" width="300" height="180" rx="10" fill="#8B7355" opacity="0.3"/>
          <line x1="100" y1="340" x2="400" y2="340" stroke="#6B5545" stroke-width="3"/>
          <line x1="100" y1="400" x2="400" y2="400" stroke="#6B5545" stroke-width="3"/>

          <!-- 书本 -->
          <rect x="120" y="290" width="35" height="45" rx="3" fill="#5D87FF"/>
          <rect x="160" y="285" width="35" height="50" rx="3" fill="#FF6B9D"/>
          <rect x="200" y="292" width="35" height="43" rx="3" fill="#FFA726"/>
          <rect x="240" y="288" width="35" height="47" rx="3" fill="#66BB6A"/>
          <rect x="280" y="295" width="35" height="40" rx="3" fill="#AB47BC"/>
          <rect x="320" y="290" width="35" height="45" rx="3" fill="#29B6F6"/>

          <rect x="130" y="350" width="35" height="45" rx="3" fill="#7E57C2"/>
          <rect x="170" y="353" width="35" height="42" rx="3" fill="#EF5350"/>
          <rect x="210" y="348" width="35" height="47" rx="3" fill="#26A69A"/>
          <rect x="250" y="355" width="35" height="40" rx="3" fill="#FFA726"/>
          <rect x="290" y="351" width="35" height="44" rx="3" fill="#5C6BC0"/>

          <!-- 打开的书本 -->
          <path d="M 200 150 L 150 200 L 200 240 L 250 200 Z" fill="#5D87FF" opacity="0.9"/>
          <path d="M 250 200 L 300 240 L 350 200 L 300 150 Z" fill="#4A90E2" opacity="0.9"/>
          <line x1="250" y1="200" x2="250" y2="150" stroke="#fff" stroke-width="3"/>

          <!-- 书页线条 -->
          <line x1="200" y1="180" x2="240" y2="190" stroke="#fff" stroke-width="2" opacity="0.7"/>
          <line x1="200" y1="200" x2="240" y2="210" stroke="#fff" stroke-width="2" opacity="0.7"/>
          <line x1="260" y1="190" x2="300" y2="180" stroke="#fff" stroke-width="2" opacity="0.7"/>
          <line x1="260" y1="210" x2="300" y2="200" stroke="#fff" stroke-width="2" opacity="0.7"/>

          <!-- 装饰性星星/亮点 -->
          <circle cx="380" cy="120" r="5" fill="#FFD700"/>
          <circle cx="120" cy="180" r="4" fill="#FFD700"/>
          <circle cx="350" cy="280" r="3" fill="#FFD700"/>
        </svg>
      </div>
    </div>

    <!-- 右侧登录表单区域 -->
    <div class="login-right">
      <div class="login-form-wrapper">
        <!-- Logo -->
        <div class="logo-section">
          <div class="logo-icon">
            <svg width="40" height="40" viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="8" fill="url(#gradient)" />
              <!-- 书本图标 -->
              <path d="M 8 10 L 8 24 L 16 22 L 24 24 L 24 10 L 16 12 L 8 10 Z" fill="white" opacity="0.9"/>
              <line x1="16" y1="12" x2="16" y2="22" stroke="white" stroke-width="1.5"/>
              <defs>
                <linearGradient id="gradient" x1="0" y1="0" x2="32" y2="32">
                  <stop offset="0%" stop-color="#667eea" />
                  <stop offset="100%" stop-color="#764ba2" />
                </linearGradient>
              </defs>
            </svg>
          </div>
          <span class="logo-text">国创睿峰图书馆</span>
        </div>

        <!-- 欢迎文字 -->
        <div class="welcome-section">
          <h1 class="welcome-title">欢迎使用智慧图书馆系统</h1>
          <p class="welcome-subtitle">AI驱动的现代化图书管理平台</p>
        </div>


        <!-- 登录表单 -->
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          class="login-form"
        >
          <div class="form-group">
            <label class="form-label">用户名</label>
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
              />
            </el-form-item>
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
          </div>

          <div class="form-options">
            <el-checkbox v-model="loginForm.remember">记住此设备</el-checkbox>
            <a href="#" class="forgot-link">忘记密码？</a>
          </div>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="signin-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '立即登录' }}
          </el-button>

          <div class="signup-link">
            <span>还没有账号？</span>
            <a href="#">联系管理员开通</a>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

// 密码登录
async function handleLogin() {
  try {
    await loginFormRef.value.validate()
    loading.value = true

    await userStore.login(loginForm)

    ElMessage.success('登录成功')

    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (error) {
    console.error('Login failed:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  width: 100%;
  height: 100vh;
  background: #fff;
}

// 左侧插画区域
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #E3F2FD 0%, #E8EAF6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  // 添加装饰性背景元素
  &::before {
    content: '';
    position: absolute;
    top: -200px;
    left: -200px;
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(93, 135, 255, 0.08) 0%, transparent 70%);
    animation: floatAnimation 20s ease-in-out infinite;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -150px;
    right: -150px;
    width: 350px;
    height: 350px;
    background: radial-gradient(circle, rgba(118, 75, 162, 0.06) 0%, transparent 70%);
    animation: floatAnimation 25s ease-in-out infinite reverse;
  }

  .auth-illustration {
    width: 100%;
    max-width: 500px;
    padding: 40px;
    position: relative;
    z-index: 1;

    .auth-svg {
      width: 100%;
      height: auto;
      filter: drop-shadow(0 10px 40px rgba(93, 135, 255, 0.15));

      // 添加书本的悬浮动画
      rect[fill="#5D87FF"],
      rect[fill="#FF6B9D"],
      rect[fill="#FFA726"],
      rect[fill="#66BB6A"],
      rect[fill="#AB47BC"],
      rect[fill="#29B6F6"],
      rect[fill="#7E57C2"],
      rect[fill="#EF5350"],
      rect[fill="#26A69A"],
      rect[fill="#5C6BC0"] {
        transition: transform 0.3s ease;
      }

      // 星星闪烁动画
      circle[fill="#FFD700"] {
        animation: sparkle 2s ease-in-out infinite;
      }
    }
  }
}

@keyframes floatAnimation {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(30px, -30px) scale(1.1);
  }
}

@keyframes sparkle {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.8);
  }
}

// 右侧表单区域
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
}

.login-form-wrapper {
  width: 100%;
  max-width: 450px;
}

// Logo 区域
.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 48px;
  animation: slideInDown 0.6s ease-out;

  .logo-icon {
    flex-shrink: 0;
    position: relative;

    svg {
      transition: transform 0.3s ease;
      filter: drop-shadow(0 4px 12px rgba(93, 135, 255, 0.15));

      &:hover {
        transform: rotate(-5deg) scale(1.05);
      }
    }
  }

  .logo-text {
    font-size: 20px;
    font-weight: 700;
    color: #2A3547;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      bottom: -4px;
      left: 0;
      width: 0;
      height: 2px;
      background: linear-gradient(90deg, #667eea, #764ba2);
      transition: width 0.4s ease;
    }

    &:hover::after {
      width: 100%;
    }
  }
}

@keyframes slideInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 欢迎文字
.welcome-section {
  margin-bottom: 32px;
  animation: fadeInUp 0.8s ease-out 0.2s both;

  .welcome-title {
    font-size: 28px;
    font-weight: 700;
    color: #2A3547;
    margin-bottom: 8px;
    letter-spacing: -0.5px;
    background: linear-gradient(135deg, #2A3547 0%, #5D87FF 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .welcome-subtitle {
    font-size: 14px;
    color: #5A6A85;
    font-weight: 500;
    position: relative;
    padding-left: 12px;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 16px;
      background: linear-gradient(180deg, #667eea, #764ba2);
      border-radius: 2px;
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}


// 登录表单
.login-form {
  animation: fadeInUp 1s ease-out 0.4s both;

  .form-group {
    margin-bottom: 20px;

    .form-label {
      display: block;
      font-size: 14px;
      font-weight: 500;
      color: #2A3547;
      margin-bottom: 8px;
      transition: color 0.3s ease;
    }
  }

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-input__wrapper) {
    border-radius: 8px;
    padding: 6px 12px;
    box-shadow: 0 0 0 1px #E5E7EB;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    background: #fff;

    &:hover {
      box-shadow: 0 0 0 1px #5D87FF, 0 4px 12px rgba(93, 135, 255, 0.1);
      transform: translateY(-1px);
    }

    &.is-focus {
      box-shadow: 0 0 0 2px rgba(93, 135, 255, 0.2), 0 8px 16px rgba(93, 135, 255, 0.15);
      transform: translateY(-2px);
      background: #fff;
    }
  }

  .form-options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    :deep(.el-checkbox) {
      transition: transform 0.2s ease;

      &:hover {
        transform: scale(1.02);
      }
    }

    :deep(.el-checkbox__label) {
      font-size: 13px;
      color: #5A6A85;
      transition: color 0.3s ease;
    }

    :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
      color: #5D87FF;
    }

    .forgot-link {
      color: #5D87FF;
      text-decoration: none;
      font-size: 13px;
      font-weight: 500;
      transition: all 0.3s ease;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        bottom: -2px;
        left: 0;
        width: 0;
        height: 1px;
        background: linear-gradient(90deg, #5D87FF, #4570EA);
        transition: width 0.3s ease;
      }

      &:hover {
        color: #4570EA;

        &::after {
          width: 100%;
        }
      }
    }
  }

  .signin-button {
    width: 100%;
    height: 46px;
    border-radius: 8px;
    font-size: 15px;
    font-weight: 600;
    background: linear-gradient(135deg, #5D87FF 0%, #4570EA 100%);
    border: none;
    margin-bottom: 20px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 4px 12px rgba(93, 135, 255, 0.3);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
      transition: left 0.5s ease;
    }

    &:hover {
      background: linear-gradient(135deg, #4570EA 0%, #3461D9 100%);
      box-shadow: 0 8px 20px rgba(93, 135, 255, 0.4);
      transform: translateY(-2px);

      &::before {
        left: 100%;
      }
    }

    &:active {
      transform: translateY(0);
      box-shadow: 0 4px 12px rgba(93, 135, 255, 0.3);
    }
  }

  .signup-link {
    text-align: center;
    font-size: 14px;
    color: #5A6A85;

    span {
      margin-right: 4px;
    }

    a {
      color: #5D87FF;
      text-decoration: none;
      font-weight: 600;
      position: relative;
      transition: color 0.3s ease;

      &::after {
        content: '';
        position: absolute;
        bottom: -2px;
        left: 0;
        width: 0;
        height: 2px;
        background: linear-gradient(90deg, #5D87FF, #4570EA);
        transition: width 0.3s ease;
      }

      &:hover {
        color: #4570EA;

        &::after {
          width: 100%;
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 968px) {
  .login-left {
    display: none;
  }

  .login-right {
    flex: 1;
  }
}

@media (max-width: 480px) {
  .login-right {
    padding: 24px;
  }

  .logo-section {
    margin-bottom: 32px;
  }

  .welcome-section .welcome-title {
    font-size: 24px;
  }

  .social-login {
    flex-direction: column;

    .social-btn {
      width: 100%;
    }
  }
}
</style>
