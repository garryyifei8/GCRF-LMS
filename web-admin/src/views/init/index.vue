<template>
  <div class="init-page">
    <div class="wizard-card">
      <h1>系统初始化向导</h1>
      <el-steps :active="step" finish-status="success" align-center class="steps">
        <el-step title="欢迎" />
        <el-step title="基本信息" />
        <el-step title="借阅规则" />
        <el-step title="完成" />
      </el-steps>

      <!-- Step 0: Welcome -->
      <div v-if="step === 0" class="step-content welcome">
        <el-icon :size="80" color="#409EFF"><Reading /></el-icon>
        <h2>欢迎使用 GCRF 智能图书馆管理系统</h2>
        <p>本向导将引导您完成系统的初始配置（约需 2 分钟）。</p>
        <ul class="feature-list">
          <li>📚 图书管理 — 编目、典藏、盘点、剔旧</li>
          <li>👥 读者管理 — 学生、教师分类管理</li>
          <li>🔄 流通管理 — 借还、续借、预约</li>
          <li>📊 数据分析 — 借阅趋势、热门图书</li>
          <li>🤖 AI 辅助 — 智能问答、个性化推荐</li>
        </ul>
        <el-button type="primary" size="large" @click="step++">开始配置</el-button>
      </div>

      <!-- Step 1: Library info -->
      <div v-else-if="step === 1" class="step-content">
        <h3>图书馆基本信息</h3>
        <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
          <el-form-item label="图书馆名称" prop="library_name" required>
            <el-input v-model="form.library_name" placeholder="如：国创睿峰图书馆" />
          </el-form-item>
          <el-form-item label="图书馆地址">
            <el-input v-model="form.library_address" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.library_phone" />
          </el-form-item>
        </el-form>
        <div class="actions">
          <el-button @click="step--">上一步</el-button>
          <el-button type="primary" @click="nextFromStep1">下一步</el-button>
        </div>
      </div>

      <!-- Step 2: Borrow rules -->
      <div v-else-if="step === 2" class="step-content">
        <h3>借阅规则</h3>
        <el-form :model="form" label-width="160px">
          <el-form-item label="学生最大借阅册数">
            <el-input-number v-model="form.student_max_borrow" :min="1" :max="50" />
          </el-form-item>
          <el-form-item label="教师最大借阅册数">
            <el-input-number v-model="form.teacher_max_borrow" :min="1" :max="100" />
          </el-form-item>
          <el-form-item label="借阅天数">
            <el-input-number v-model="form.borrow_days" :min="1" :max="365" />
            <span class="hint">天</span>
          </el-form-item>
          <el-form-item label="逾期罚金">
            <el-input-number v-model="form.fine_per_day" :min="0" :step="0.1" :precision="2" />
            <span class="hint">元/天</span>
          </el-form-item>
        </el-form>
        <div class="actions">
          <el-button @click="step--">上一步</el-button>
          <el-button type="primary" @click="step++">下一步</el-button>
        </div>
      </div>

      <!-- Step 3: Summary + finish -->
      <div v-else-if="step === 3" class="step-content">
        <h3>确认配置</h3>
        <el-descriptions border :column="1" size="default">
          <el-descriptions-item label="图书馆名称">{{ form.library_name }}</el-descriptions-item>
          <el-descriptions-item label="图书馆地址">{{
            form.library_address || '未填写'
          }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{
            form.library_phone || '未填写'
          }}</el-descriptions-item>
          <el-descriptions-item label="学生最大借阅册数"
            >{{ form.student_max_borrow }} 册</el-descriptions-item
          >
          <el-descriptions-item label="教师最大借阅册数"
            >{{ form.teacher_max_borrow }} 册</el-descriptions-item
          >
          <el-descriptions-item label="借阅天数">{{ form.borrow_days }} 天</el-descriptions-item>
          <el-descriptions-item label="逾期罚金"
            >{{ form.fine_per_day }} 元/天</el-descriptions-item
          >
        </el-descriptions>
        <div class="actions">
          <el-button @click="step--">上一步</el-button>
          <el-button type="primary" :loading="submitting" @click="finish">完成配置</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Reading } from '@element-plus/icons-vue'
import { initializeSystem } from '@/api/system'

const router = useRouter()
const step = ref(0)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  library_name: '国创睿峰图书馆',
  library_address: '',
  library_phone: '',
  student_max_borrow: 10,
  teacher_max_borrow: 20,
  borrow_days: 30,
  fine_per_day: 0.5
})

const rules = {
  library_name: [{ required: true, message: '请输入图书馆名称', trigger: 'blur' }]
}

const nextFromStep1 = async () => {
  try {
    await formRef.value?.validate()
    step.value++
  } catch {
    // validation failed, stay on step
  }
}

const finish = async () => {
  submitting.value = true
  try {
    const data = Object.fromEntries(Object.entries(form).map(([k, v]) => [k, String(v)]))
    const res = await initializeSystem(data)
    if (res.code === 200) {
      ElMessage.success('系统初始化完成')
      setTimeout(() => router.push('/dashboard'), 800)
    }
  } catch (e) {
    ElMessage.error('初始化失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.init-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.wizard-card {
  background: white;
  border-radius: 12px;
  padding: 40px;
  width: 100%;
  max-width: 720px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

h1 {
  text-align: center;
  margin-bottom: 32px;
  color: #303133;
  font-size: 24px;
}

.steps {
  margin-bottom: 32px;
}

.step-content {
  min-height: 360px;
  padding: 20px 0;

  h3 {
    font-size: 18px;
    color: #303133;
    margin-bottom: 24px;
  }
}

.welcome {
  text-align: center;

  h2 {
    margin: 16px 0 8px;
    font-size: 20px;
    color: #303133;
  }

  p {
    color: #606266;
    margin-bottom: 16px;
  }
}

.feature-list {
  text-align: left;
  max-width: 400px;
  margin: 24px auto;
  list-style: none;
  padding: 0;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px 20px;

  li {
    padding: 8px 0;
    color: #606266;
    border-bottom: 1px solid #ebeef5;

    &:last-child {
      border-bottom: none;
    }
  }
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.hint {
  margin-left: 8px;
  color: #999;
  font-size: 14px;
}
</style>
