<template>
  <div class="face-recognition">
    <div v-if="!isCapturing" class="face-recognition-start">
      <el-button type="primary" size="large" :icon="CameraFilled" @click="startCapture">
        开始人脸识别
      </el-button>
      <p class="hint">点击按钮启动摄像头进行人脸识别</p>
    </div>

    <div v-else class="face-recognition-capture">
      <div class="video-container">
        <video ref="videoRef" autoplay playsinline class="video-stream"></video>
        <canvas ref="canvasRef" class="video-canvas"></canvas>

        <div v-if="recognizing" class="recognition-overlay">
          <div class="recognition-animation">
            <div class="scan-line"></div>
            <div class="corners">
              <span class="corner corner-tl"></span>
              <span class="corner corner-tr"></span>
              <span class="corner corner-bl"></span>
              <span class="corner corner-br"></span>
            </div>
          </div>
          <p class="recognition-text">正在识别人脸...</p>
        </div>

        <div v-if="capturedImage" class="capture-success">
          <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
          <p>人脸采集成功</p>
        </div>
      </div>

      <div class="face-recognition-actions">
        <el-button type="success" :icon="Camera" :loading="capturing" @click="capturePhoto">
          {{ capturing ? '采集中...' : '采集人脸' }}
        </el-button>
        <el-button type="primary" :icon="Check" :loading="recognizing" :disabled="!capturedImage" @click="recognize">
          {{ recognizing ? '识别中...' : '开始识别' }}
        </el-button>
        <el-button :icon="RefreshRight" @click="resetCapture">重新采集</el-button>
        <el-button :icon="Close" @click="stopCapture">关闭摄像头</el-button>
      </div>

      <div class="face-recognition-tips">
        <el-alert type="info" :closable="false">
          <template #title>
            <div>识别提示：</div>
            <ul>
              <li>请保持面部正对摄像头</li>
              <li>确保光线充足，避免逆光</li>
              <li>请摘下眼镜、口罩等遮挡物</li>
              <li>保持表情自然，不要移动</li>
            </ul>
          </template>
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  // 是否自动识别
  autoRecognize: {
    type: Boolean,
    default: false
  },
  // 视频宽度
  videoWidth: {
    type: Number,
    default: 640
  },
  // 视频高度
  videoHeight: {
    type: Number,
    default: 480
  }
})

const emit = defineEmits(['capture', 'recognize', 'success', 'error'])

// 状态
const isCapturing = ref(false)
const capturing = ref(false)
const recognizing = ref(false)
const capturedImage = ref(null)

// DOM引用
const videoRef = ref(null)
const canvasRef = ref(null)

// 媒体流
let mediaStream = null

// 启动摄像头
const startCapture = async () => {
  try {
    // 检查浏览器是否支持
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      ElMessage.error('当前浏览器不支持摄像头功能')
      return
    }

    // 请求摄像头权限
    const stream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: props.videoWidth,
        height: props.videoHeight,
        facingMode: 'user' // 前置摄像头
      },
      audio: false
    })

    mediaStream = stream
    isCapturing.value = true

    // 等待DOM更新后设置视频流
    await new Promise(resolve => setTimeout(resolve, 100))

    if (videoRef.value) {
      videoRef.value.srcObject = stream
    }

    ElMessage.success('摄像头启动成功')
  } catch (error) {
    console.error('Failed to start camera:', error)

    let errorMessage = '启动摄像头失败'
    if (error.name === 'NotAllowedError') {
      errorMessage = '摄像头权限被拒绝，请在浏览器设置中允许访问摄像头'
    } else if (error.name === 'NotFoundError') {
      errorMessage = '未检测到摄像头设备'
    }

    ElMessage.error(errorMessage)
    emit('error', error)
  }
}

// 关闭摄像头
const stopCapture = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }

  if (videoRef.value) {
    videoRef.value.srcObject = null
  }

  isCapturing.value = false
  capturedImage.value = null

  ElMessage.info('摄像头已关闭')
}

// 拍照
const capturePhoto = async () => {
  if (!videoRef.value || !canvasRef.value) {
    ElMessage.warning('摄像头未就绪')
    return
  }

  try {
    capturing.value = true

    const video = videoRef.value
    const canvas = canvasRef.value

    canvas.width = video.videoWidth
    canvas.height = video.videoHeight

    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

    // 获取图片数据
    const imageData = canvas.toDataURL('image/jpeg', 0.8)
    capturedImage.value = imageData

    // 模拟延迟
    await new Promise(resolve => setTimeout(resolve, 500))

    ElMessage.success('人脸采集成功')
    emit('capture', imageData)

    // 如果开启自动识别
    if (props.autoRecognize) {
      setTimeout(() => {
        recognize()
      }, 500)
    }
  } catch (error) {
    console.error('Failed to capture photo:', error)
    ElMessage.error('采集失败，请重试')
    emit('error', error)
  } finally {
    capturing.value = false
  }
}

// 识别人脸
const recognize = async () => {
  if (!capturedImage.value) {
    ElMessage.warning('请先采集人脸')
    return
  }

  try {
    recognizing.value = true

    // TODO: 调用后端API进行人脸识别
    // const result = await api.post('/api/face/recognize', {
    //   image: capturedImage.value
    // })

    // Mock识别过程
    await new Promise(resolve => setTimeout(resolve, 2000))

    // Mock识别结果
    const mockResult = {
      success: Math.random() > 0.3, // 70%成功率
      confidence: 0.85 + Math.random() * 0.15, // 85-100%
      user: {
        id: 'U20250001',
        name: '张三',
        type: 'student',
        studentNo: 'S20250001'
      }
    }

    if (mockResult.success) {
      ElMessage.success(`识别成功！欢迎您，${mockResult.user.name}`)
      emit('recognize', mockResult)
      emit('success', mockResult)

      // 识别成功后自动关闭
      setTimeout(() => {
        stopCapture()
      }, 1500)
    } else {
      ElMessage.error('识别失败，未找到匹配的人脸信息')
      emit('error', new Error('Face not recognized'))
    }
  } catch (error) {
    console.error('Failed to recognize face:', error)
    ElMessage.error('识别失败，请重试')
    emit('error', error)
  } finally {
    recognizing.value = false
  }
}

// 重新采集
const resetCapture = () => {
  capturedImage.value = null
  recognizing.value = false

  if (canvasRef.value) {
    const ctx = canvasRef.value.getContext('2d')
    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  }

  ElMessage.info('已清除，请重新采集')
}

// 组件卸载时清理
onBeforeUnmount(() => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
  }
})

// 暴露方法给父组件
defineExpose({
  startCapture,
  stopCapture,
  capturePhoto,
  recognize,
  resetCapture
})
</script>

<style lang="scss" scoped>
.face-recognition {
  width: 100%;

  &-start {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 60px 20px;

    .hint {
      margin-top: 16px;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.45);
    }
  }

  &-capture {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  &-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  &-tips {
    ul {
      margin: 8px 0 0;
      padding-left: 20px;

      li {
        margin: 4px 0;
        font-size: 13px;
        line-height: 1.6;
      }
    }
  }
}

.video-container {
  position: relative;
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);

  .video-stream {
    width: 100%;
    height: auto;
    display: block;
  }

  .video-canvas {
    display: none;
  }
}

.recognition-overlay {
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
  backdrop-filter: blur(4px);

  .recognition-animation {
    position: relative;
    width: 200px;
    height: 240px;
  }

  .scan-line {
    position: absolute;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, #1890ff, transparent);
    animation: scan 2s ease-in-out infinite;
  }

  .corners {
    position: absolute;
    inset: 0;

    .corner {
      position: absolute;
      width: 30px;
      height: 30px;
      border: 3px solid #1890ff;

      &-tl {
        top: 0;
        left: 0;
        border-right: none;
        border-bottom: none;
      }

      &-tr {
        top: 0;
        right: 0;
        border-left: none;
        border-bottom: none;
      }

      &-bl {
        bottom: 0;
        left: 0;
        border-right: none;
        border-top: none;
      }

      &-br {
        bottom: 0;
        right: 0;
        border-left: none;
        border-top: none;
      }
    }
  }

  .recognition-text {
    margin-top: 24px;
    color: #fff;
    font-size: 16px;
    font-weight: 500;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  }
}

.capture-success {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(82, 196, 26, 0.9);
  color: #fff;
  border-radius: 8px;
  font-size: 14px;
  animation: slideInRight 0.3s ease-out;

  .success-icon {
    font-size: 20px;
  }

  p {
    margin: 0;
    font-weight: 500;
  }
}

@keyframes scan {
  0% {
    top: 0;
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    top: 100%;
    opacity: 0;
  }
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
