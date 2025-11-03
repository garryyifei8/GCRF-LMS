<template>
  <div class="avatar-upload">
    <div class="avatar-upload-preview">
      <div class="avatar-container" :class="{ 'is-circle': shape === 'circle' }">
        <img v-if="previewUrl || modelValue" :src="previewUrl || modelValue" class="avatar-image" alt="Avatar" />
        <div v-else class="avatar-placeholder">
          <el-icon class="avatar-icon"><User /></el-icon>
          <p v-if="showText" class="avatar-text">{{ placeholderText }}</p>
        </div>

        <div class="avatar-overlay" @click="handleClickUpload">
          <el-icon class="overlay-icon"><Camera /></el-icon>
          <p v-if="showText" class="overlay-text">上传头像</p>
        </div>
      </div>

      <input
        ref="fileInputRef"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleFileChange"
      />
    </div>

    <div v-if="showActions" class="avatar-upload-actions">
      <el-button size="small" :icon="Upload" @click="handleClickUpload">选择文件</el-button>
      <el-button v-if="enableCamera" size="small" :icon="Camera" @click="handleOpenCamera">拍照上传</el-button>
      <el-button v-if="modelValue || previewUrl" size="small" :icon="Delete" @click="handleRemove">移除</el-button>
    </div>

    <div v-if="showTips" class="avatar-upload-tips">
      <p v-for="(tip, index) in tips" :key="index">{{ tip }}</p>
    </div>

    <!-- 拍照对话框 -->
    <el-dialog
      v-model="cameraDialogVisible"
      title="拍照上传"
      width="700px"
      :close-on-click-modal="false"
      @close="handleCloseCamera"
    >
      <div class="camera-container">
        <video ref="videoRef" autoplay playsinline class="camera-video"></video>
        <canvas ref="canvasRef" class="camera-canvas"></canvas>

        <div v-if="capturedPhoto" class="captured-preview">
          <img :src="capturedPhoto" alt="Captured Photo" />
        </div>
      </div>

      <template #footer>
        <el-button v-if="!capturedPhoto" :icon="Camera" type="primary" @click="capturePhoto">
          拍照
        </el-button>
        <el-button v-if="capturedPhoto" :icon="RefreshRight" @click="retakePhoto">重拍</el-button>
        <el-button v-if="capturedPhoto" :icon="Check" type="success" @click="confirmPhoto">确认使用</el-button>
        <el-button @click="handleCloseCamera">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  // v-model绑定的值（图片URL）
  modelValue: {
    type: String,
    default: ''
  },
  // 头像形状：circle 圆形，square 方形
  shape: {
    type: String,
    default: 'circle',
    validator: value => ['circle', 'square'].includes(value)
  },
  // 头像尺寸
  size: {
    type: Number,
    default: 120
  },
  // 是否显示文字提示
  showText: {
    type: Boolean,
    default: true
  },
  // 占位文字
  placeholderText: {
    type: String,
    default: '点击上传'
  },
  // 是否显示操作按钮
  showActions: {
    type: Boolean,
    default: true
  },
  // 是否启用摄像头拍照
  enableCamera: {
    type: Boolean,
    default: true
  },
  // 是否显示提示信息
  showTips: {
    type: Boolean,
    default: true
  },
  // 提示信息
  tips: {
    type: Array,
    default: () => ['支持 JPG、PNG 格式', '建议尺寸 400x400 像素', '文件大小不超过 2MB']
  },
  // 文件大小限制（MB）
  maxSize: {
    type: Number,
    default: 2
  },
  // 允许的文件类型
  accept: {
    type: String,
    default: 'image/jpeg,image/png,image/jpg'
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'upload', 'error'])

// 状态
const previewUrl = ref('')
const fileInputRef = ref(null)

// 摄像头相关
const cameraDialogVisible = ref(false)
const videoRef = ref(null)
const canvasRef = ref(null)
const capturedPhoto = ref(null)
let mediaStream = null

// 计算CSS变量
const avatarStyle = computed(() => ({
  '--avatar-size': `${props.size}px`
}))

// 点击上传
const handleClickUpload = () => {
  fileInputRef.value?.click()
}

// 文件选择变化
const handleFileChange = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!props.accept.split(',').some(type => file.type === type.trim())) {
    ElMessage.error('不支持的文件格式')
    emit('error', new Error('Unsupported file format'))
    return
  }

  // 验证文件大小
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    emit('error', new Error('File size exceeded'))
    return
  }

  // 读取文件并预览
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target.result
    emit('update:modelValue', e.target.result)
    emit('change', e.target.result, file)
    emit('upload', file)
  }
  reader.readAsDataURL(file)

  // 清空input，允许重复选择同一文件
  event.target.value = ''
}

// 打开摄像头
const handleOpenCamera = async () => {
  try {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      ElMessage.error('当前浏览器不支持摄像头功能')
      return
    }

    cameraDialogVisible.value = true

    // 等待对话框打开
    await new Promise(resolve => setTimeout(resolve, 300))

    const stream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: 640,
        height: 480,
        facingMode: 'user'
      },
      audio: false
    })

    mediaStream = stream

    if (videoRef.value) {
      videoRef.value.srcObject = stream
    }
  } catch (error) {
    console.error('Failed to open camera:', error)

    let errorMessage = '启动摄像头失败'
    if (error.name === 'NotAllowedError') {
      errorMessage = '摄像头权限被拒绝'
    } else if (error.name === 'NotFoundError') {
      errorMessage = '未检测到摄像头设备'
    }

    ElMessage.error(errorMessage)
    emit('error', error)
    cameraDialogVisible.value = false
  }
}

// 关闭摄像头
const handleCloseCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }

  if (videoRef.value) {
    videoRef.value.srcObject = null
  }

  capturedPhoto.value = null
  cameraDialogVisible.value = false
}

// 拍照
const capturePhoto = () => {
  if (!videoRef.value || !canvasRef.value) return

  const video = videoRef.value
  const canvas = canvasRef.value

  canvas.width = video.videoWidth
  canvas.height = video.videoHeight

  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

  capturedPhoto.value = canvas.toDataURL('image/jpeg', 0.8)
}

// 重拍
const retakePhoto = () => {
  capturedPhoto.value = null
}

// 确认使用照片
const confirmPhoto = () => {
  if (!capturedPhoto.value) return

  previewUrl.value = capturedPhoto.value
  emit('update:modelValue', capturedPhoto.value)
  emit('change', capturedPhoto.value)

  ElMessage.success('照片上传成功')
  handleCloseCamera()
}

// 移除头像
const handleRemove = () => {
  previewUrl.value = ''
  emit('update:modelValue', '')
  emit('change', '')
  ElMessage.info('已移除头像')
}

// 组件卸载时清理
onBeforeUnmount(() => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
  }
})
</script>

<style lang="scss" scoped>
.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;

  &-preview {
    position: relative;
  }

  &-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  &-tips {
    text-align: center;

    p {
      margin: 4px 0;
      font-size: 12px;
      color: rgba(0, 0, 0, 0.45);
      line-height: 1.5;
    }
  }
}

.avatar-container {
  position: relative;
  width: var(--avatar-size);
  height: var(--avatar-size);
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: #1890ff;

    .avatar-overlay {
      opacity: 1;
    }
  }

  &.is-circle {
    border-radius: 50%;
  }

  .avatar-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .avatar-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background: #fafafa;

    .avatar-icon {
      font-size: 48px;
      color: #8c8c8c;
    }

    .avatar-text {
      margin-top: 8px;
      font-size: 14px;
      color: #8c8c8c;
    }
  }

  .avatar-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.5);
    opacity: 0;
    transition: opacity 0.3s;

    .overlay-icon {
      font-size: 32px;
      color: #fff;
    }

    .overlay-text {
      margin-top: 8px;
      font-size: 14px;
      color: #fff;
    }
  }
}

.camera-container {
  position: relative;
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  background: #000;
  border-radius: 8px;
  overflow: hidden;

  .camera-video {
    width: 100%;
    height: auto;
    display: block;
  }

  .camera-canvas {
    display: none;
  }

  .captured-preview {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: #000;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }
}
</style>
