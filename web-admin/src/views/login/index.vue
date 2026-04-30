<template>
  <div class="login-container">
    <!-- 左侧插画区域：2D 现代矢量画 — 远山书海·智读 -->
    <div class="login-left">
      <div class="auth-illustration">
        <svg
          class="auth-svg"
          viewBox="0 0 600 600"
          xmlns="http://www.w3.org/2000/svg"
          role="img"
          aria-label="国创睿峰 智慧图书馆插画"
        >
          <defs>
            <linearGradient id="skyGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#EEF3FF" />
              <stop offset="100%" stop-color="#FFFFFF" />
            </linearGradient>
            <linearGradient id="mountFar" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#C5D2FF" />
              <stop offset="100%" stop-color="#A9BEFF" />
            </linearGradient>
            <linearGradient id="mountMid" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#7B9BFF" />
              <stop offset="100%" stop-color="#5D87FF" />
            </linearGradient>
            <linearGradient id="mountNear" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#3759C7" />
              <stop offset="100%" stop-color="#2A3D8F" />
            </linearGradient>
            <linearGradient id="sunGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#FFE08A" />
              <stop offset="100%" stop-color="#FFB347" />
            </linearGradient>
            <linearGradient id="bookA" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stop-color="#7B9BFF" />
              <stop offset="100%" stop-color="#5D87FF" />
            </linearGradient>
            <linearGradient id="bookB" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stop-color="#FFB7CB" />
              <stop offset="100%" stop-color="#FF6B9D" />
            </linearGradient>
            <linearGradient id="bookC" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stop-color="#FFD79A" />
              <stop offset="100%" stop-color="#FFA726" />
            </linearGradient>
            <linearGradient id="floorGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0" />
              <stop offset="100%" stop-color="#5D87FF" stop-opacity="0.10" />
            </linearGradient>
            <radialGradient id="halo" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0%" stop-color="#FFE08A" stop-opacity="0.5" />
              <stop offset="100%" stop-color="#FFE08A" stop-opacity="0" />
            </radialGradient>
          </defs>

          <!-- 天空 -->
          <rect width="600" height="600" fill="url(#skyGrad)" />

          <!-- 远景太阳光晕 -->
          <circle cx="430" cy="170" r="160" fill="url(#halo)" />
          <circle cx="430" cy="170" r="42" fill="url(#sunGrad)" />

          <!-- 远山（最远，淡） -->
          <path
            d="M0 360 L80 280 L150 320 L230 240 L310 300 L390 250 L470 300 L540 270 L600 320 L600 600 L0 600 Z"
            fill="url(#mountFar)"
            opacity="0.55"
          />

          <!-- 中山（睿峰主峰） -->
          <path
            d="M0 420 L100 320 L180 380 L260 290 L340 360 L420 310 L500 380 L600 350 L600 600 L0 600 Z"
            fill="url(#mountMid)"
            opacity="0.85"
          />

          <!-- 山顶白雪点 -->
          <path d="M250 300 L260 290 L270 300 L260 305 Z" fill="#FFFFFF" opacity="0.9" />
          <path d="M408 320 L420 310 L432 320 L420 326 Z" fill="#FFFFFF" opacity="0.9" />

          <!-- 近山 -->
          <path
            d="M-20 480 L80 410 L160 460 L240 400 L320 460 L420 420 L520 470 L620 440 L620 600 L-20 600 Z"
            fill="url(#mountNear)"
          />

          <!-- 飘云 -->
          <g opacity="0.85">
            <ellipse cx="120" cy="130" rx="42" ry="10" fill="#FFFFFF" />
            <ellipse cx="150" cy="120" rx="30" ry="8" fill="#FFFFFF" />
            <ellipse cx="500" cy="90" rx="36" ry="9" fill="#FFFFFF" />
            <ellipse cx="525" cy="82" rx="22" ry="6" fill="#FFFFFF" />
          </g>

          <!-- 飞鸟 -->
          <g stroke="#3759C7" stroke-width="2" fill="none" stroke-linecap="round">
            <path d="M210 165 q8 -8 16 0 q8 -8 16 0" />
            <path d="M270 200 q6 -6 12 0 q6 -6 12 0" opacity="0.7" />
          </g>

          <!-- 地板倒影 -->
          <rect x="0" y="500" width="600" height="100" fill="url(#floorGrad)" />

          <!-- 主体：阅读人物（极简几何风） -->
          <g transform="translate(190 360)">
            <!-- 影子 -->
            <ellipse cx="60" cy="190" rx="105" ry="10" fill="#3759C7" opacity="0.12" />

            <!-- 椅垫 -->
            <rect x="-10" y="170" width="150" height="14" rx="6" fill="#3759C7" opacity="0.25" />

            <!-- 身体（侧坐） -->
            <path
              d="M40 80 q-10 30 -2 65 q4 22 14 35 l50 0 q14 -10 16 -32 q3 -34 -8 -68 q-6 -16 -22 -16 q-30 0 -48 16 z"
              fill="#5D87FF"
            />
            <!-- 衣领高光 -->
            <path
              d="M55 86 q12 -10 32 -10 q14 0 22 6 q-8 6 -22 6 q-22 0 -32 -2 z"
              fill="#FFFFFF"
              opacity="0.18"
            />

            <!-- 手臂 -->
            <path d="M60 110 q-6 24 6 46 q6 8 16 4 q4 -10 -2 -18 q-4 -14 -2 -28 z" fill="#3759C7" />

            <!-- 脖颈 + 头 -->
            <rect x="62" y="58" width="20" height="14" rx="6" fill="#F4C7A0" />
            <circle cx="72" cy="44" r="22" fill="#F4C7A0" />
            <!-- 头发 -->
            <path
              d="M52 36 q-2 -22 18 -28 q22 -6 32 12 q6 12 0 24 q-10 -10 -28 -8 q-14 2 -22 0 z"
              fill="#2A3D8F"
            />
            <!-- 耳后发束 -->
            <path d="M50 44 q-6 12 0 24 q6 -2 6 -12 z" fill="#2A3D8F" />
            <!-- 闭目专注（微笑弧线） -->
            <path
              d="M62 44 q4 3 8 0"
              stroke="#2A3D8F"
              stroke-width="1.6"
              fill="none"
              stroke-linecap="round"
            />
            <path
              d="M76 46 q4 3 8 0"
              stroke="#2A3D8F"
              stroke-width="1.6"
              fill="none"
              stroke-linecap="round"
            />
            <path
              d="M68 56 q6 4 10 0"
              stroke="#C97070"
              stroke-width="1.6"
              fill="none"
              stroke-linecap="round"
            />

            <!-- 怀中的书（打开） -->
            <g transform="translate(20 110)">
              <path
                d="M0 12 L40 4 L40 36 L0 44 Z"
                fill="#FFFFFF"
                stroke="#5D87FF"
                stroke-width="1.5"
              />
              <path
                d="M40 4 L80 12 L80 44 L40 36 Z"
                fill="#FFFFFF"
                stroke="#5D87FF"
                stroke-width="1.5"
              />
              <line x1="40" y1="4" x2="40" y2="36" stroke="#5D87FF" stroke-width="1.5" />
              <line x1="8" y1="18" x2="32" y2="14" stroke="#7B9BFF" stroke-width="1.2" />
              <line x1="8" y1="24" x2="32" y2="20" stroke="#7B9BFF" stroke-width="1.2" />
              <line x1="48" y1="14" x2="72" y2="18" stroke="#7B9BFF" stroke-width="1.2" />
              <line x1="48" y1="20" x2="72" y2="24" stroke="#7B9BFF" stroke-width="1.2" />
            </g>
          </g>

          <!-- 书堆（左下） -->
          <g transform="translate(70 470)">
            <rect x="0" y="40" width="140" height="22" rx="4" fill="url(#bookA)" />
            <rect x="6" y="60" width="128" height="18" rx="4" fill="url(#bookB)" />
            <rect x="14" y="76" width="108" height="16" rx="4" fill="url(#bookC)" />
            <!-- 倾斜的书 -->
            <g transform="translate(110 0) rotate(-8)">
              <rect width="22" height="56" rx="3" fill="#3759C7" />
              <line x1="3" y1="6" x2="3" y2="50" stroke="#FFFFFF" stroke-width="1.2" />
            </g>
          </g>

          <!-- 书堆（右下） -->
          <g transform="translate(420 480)">
            <rect x="0" y="36" width="120" height="20" rx="4" fill="url(#bookB)" />
            <rect x="-4" y="56" width="128" height="18" rx="4" fill="url(#bookA)" />
            <rect x="6" y="74" width="108" height="16" rx="4" fill="url(#bookC)" />
            <!-- 一本立着 -->
            <g transform="translate(0 -36)">
              <rect width="16" height="40" rx="2" fill="#FF6B9D" />
              <line x1="3" y1="6" x2="3" y2="34" stroke="#FFFFFF" stroke-width="1" />
            </g>
          </g>

          <!-- 漂浮纸鹤 -->
          <g transform="translate(95 220) rotate(-12)">
            <path
              d="M0 10 L24 0 L48 10 L24 20 Z"
              fill="#FFFFFF"
              stroke="#5D87FF"
              stroke-width="1.5"
            />
            <path d="M24 0 L24 20" stroke="#5D87FF" stroke-width="1.2" />
            <path d="M16 6 L24 10 L32 6" fill="none" stroke="#5D87FF" stroke-width="1.2" />
          </g>
          <g transform="translate(440 270) rotate(8)">
            <path
              d="M0 10 L24 0 L48 10 L24 20 Z"
              fill="#FFFFFF"
              stroke="#FF6B9D"
              stroke-width="1.5"
            />
            <path d="M24 0 L24 20" stroke="#FF6B9D" stroke-width="1.2" />
          </g>

          <!-- 漂浮智能节点 (AI 主题) -->
          <g stroke="#5D87FF" stroke-width="1.2" fill="none" opacity="0.55">
            <circle cx="60" cy="320" r="3.5" fill="#5D87FF" />
            <circle cx="120" cy="280" r="2.5" fill="#5D87FF" />
            <circle cx="180" cy="260" r="3" fill="#5D87FF" />
            <line x1="60" y1="320" x2="120" y2="280" />
            <line x1="120" y1="280" x2="180" y2="260" />
            <circle cx="500" cy="350" r="3" fill="#FF6B9D" />
            <circle cx="540" cy="310" r="2.5" fill="#FF6B9D" />
            <line x1="500" y1="350" x2="540" y2="310" />
          </g>

          <!-- 星点装饰 -->
          <g fill="#FFD55A">
            <circle cx="380" cy="80" r="2.5" />
            <circle cx="60" cy="100" r="2" />
            <circle cx="320" cy="200" r="2" />
            <circle cx="560" cy="200" r="2.5" />
          </g>
        </svg>

        <!-- 副标语 -->
        <div class="auth-tagline">
          <h2 class="auth-tagline-title">书山有路 · 智读启程</h2>
          <p class="auth-tagline-text">国创睿峰 · 让每一所学校都拥有现代化的智慧图书馆</p>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区域 -->
    <div class="login-right">
      <div class="login-form-wrapper">
        <!-- Logo -->
        <div class="logo-section">
          <BrandLogo :size="44" />
        </div>

        <!-- 欢迎文字 -->
        <div class="welcome-section">
          <h1 class="welcome-title">{{ brandStore.brand.loginTitle }}</h1>
          <p class="welcome-subtitle">{{ brandStore.brand.loginSubtitle }}</p>
        </div>

        <!-- 登录表单 -->
        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" class="login-form">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" />
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useBrandStore } from '@/stores/brand'
import { ElMessage } from 'element-plus'
import BrandLogo from '@/components/BrandLogo.vue'

const brandStore = useBrandStore()
onMounted(() => brandStore.loadBrand())

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
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
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
  background: linear-gradient(135deg, #e3f2fd 0%, #e8eaf6 100%);
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
    max-width: 560px;
    padding: 40px;
    position: relative;
    z-index: 1;
    text-align: center;

    .auth-tagline {
      margin-top: 24px;
      animation: fadeInUp 1s ease-out 0.4s both;

      .auth-tagline-title {
        font-size: 22px;
        font-weight: 700;
        color: #2a3547;
        margin: 0 0 8px;
        letter-spacing: 2px;
        line-height: 1.4;
      }
      .auth-tagline-text {
        font-size: 13px;
        color: #5a6a85;
        margin: 0;
        letter-spacing: 0.5px;
      }
    }

    .auth-svg {
      width: 100%;
      height: auto;
      filter: drop-shadow(0 10px 40px rgba(93, 135, 255, 0.15));

      // 添加书本的悬浮动画
      rect[fill='#5D87FF'],
      rect[fill='#FF6B9D'],
      rect[fill='#FFA726'],
      rect[fill='#66BB6A'],
      rect[fill='#AB47BC'],
      rect[fill='#29B6F6'],
      rect[fill='#7E57C2'],
      rect[fill='#EF5350'],
      rect[fill='#26A69A'],
      rect[fill='#5C6BC0'] {
        transition: transform 0.3s ease;
      }

      // 星星闪烁动画
      circle[fill='#FFD700'] {
        animation: sparkle 2s ease-in-out infinite;
      }
    }
  }
}

@keyframes floatAnimation {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(30px, -30px) scale(1.1);
  }
}

@keyframes sparkle {
  0%,
  100% {
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
    color: #2a3547;
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
    color: #2a3547;
    margin-bottom: 8px;
    letter-spacing: 0;
    line-height: 1.4;
    padding: 4px 2px 4px 0;
    word-break: keep-all;
    background: linear-gradient(135deg, #2a3547 0%, #5d87ff 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .welcome-subtitle {
    font-size: 14px;
    color: #5a6a85;
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
      color: #2a3547;
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
    box-shadow: 0 0 0 1px #e5e7eb;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    background: #fff;

    &:hover {
      box-shadow:
        0 0 0 1px #5d87ff,
        0 4px 12px rgba(93, 135, 255, 0.1);
      transform: translateY(-1px);
    }

    &.is-focus {
      box-shadow:
        0 0 0 2px rgba(93, 135, 255, 0.2),
        0 8px 16px rgba(93, 135, 255, 0.15);
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
      color: #5a6a85;
      transition: color 0.3s ease;
    }

    :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
      color: #5d87ff;
    }

    .forgot-link {
      color: #5d87ff;
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
        background: linear-gradient(90deg, #5d87ff, #4570ea);
        transition: width 0.3s ease;
      }

      &:hover {
        color: #4570ea;

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
    background: linear-gradient(135deg, #5d87ff 0%, #4570ea 100%);
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
      background: linear-gradient(135deg, #4570ea 0%, #3461d9 100%);
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
    color: #5a6a85;

    span {
      margin-right: 4px;
    }

    a {
      color: #5d87ff;
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
        background: linear-gradient(90deg, #5d87ff, #4570ea);
        transition: width 0.3s ease;
      }

      &:hover {
        color: #4570ea;

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
