<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">系统配置</h1>
      <p class="page-header-description">管理系统参数和业务规则</p>
    </div>

    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- 借阅规则 -->
      <el-tab-pane label="借阅规则" name="borrow">
        <div class="card">
          <div class="card-content">
            <el-form ref="borrowFormRef" :model="borrowConfig" label-width="150px">
              <el-divider content-position="left">学生借阅规则</el-divider>

              <el-form-item label="最大借阅数量">
                <el-input-number v-model="borrowConfig.student.maxBooks" :min="1" :max="20" />
                <span class="form-tip">本</span>
              </el-form-item>

              <el-form-item label="借阅期限">
                <el-input-number v-model="borrowConfig.student.borrowDays" :min="1" :max="90" />
                <span class="form-tip">天</span>
              </el-form-item>

              <el-form-item label="可续借次数">
                <el-input-number v-model="borrowConfig.student.renewTimes" :min="0" :max="5" />
                <span class="form-tip">次</span>
              </el-form-item>

              <el-divider content-position="left">教师借阅规则</el-divider>

              <el-form-item label="最大借阅数量">
                <el-input-number v-model="borrowConfig.teacher.maxBooks" :min="1" :max="50" />
                <span class="form-tip">本</span>
              </el-form-item>

              <el-form-item label="借阅期限">
                <el-input-number v-model="borrowConfig.teacher.borrowDays" :min="1" :max="180" />
                <span class="form-tip">天</span>
              </el-form-item>

              <el-form-item label="可续借次数">
                <el-input-number v-model="borrowConfig.teacher.renewTimes" :min="0" :max="5" />
                <span class="form-tip">次</span>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleSave('borrow')">保存配置</el-button>
                <el-button @click="handleReset('borrow')">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>

      <!-- 罚款规则 -->
      <el-tab-pane label="罚款规则" name="fine">
        <div class="card">
          <div class="card-content">
            <el-form ref="fineFormRef" :model="fineConfig" label-width="150px">
              <el-form-item label="逾期宽限期">
                <el-input-number v-model="fineConfig.graceDays" :min="0" :max="7" />
                <span class="form-tip">天（宽限期内不计罚款）</span>
              </el-form-item>

              <el-form-item label="每日罚款金额">
                <el-input-number
                  v-model="fineConfig.finePerDay"
                  :min="0"
                  :max="10"
                  :precision="2"
                  :step="0.1"
                />
                <span class="form-tip">元/天</span>
              </el-form-item>

              <el-form-item label="最高罚款金额">
                <el-input-number
                  v-model="fineConfig.maxFine"
                  :min="0"
                  :max="1000"
                  :precision="2"
                  :step="1"
                />
                <span class="form-tip">元</span>
              </el-form-item>

              <el-form-item label="欠款限制借阅">
                <el-switch v-model="fineConfig.blockBorrowOnDebt" />
                <span class="form-tip">开启后，有欠款读者无法借阅</span>
              </el-form-item>

              <el-form-item label="限制借阅金额">
                <el-input-number
                  v-model="fineConfig.debtThreshold"
                  :min="0"
                  :max="500"
                  :precision="2"
                  :step="1"
                  :disabled="!fineConfig.blockBorrowOnDebt"
                />
                <span class="form-tip">元（欠款超过此金额限制借阅）</span>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleSave('fine')">保存配置</el-button>
                <el-button @click="handleReset('fine')">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>

      <!-- 预约规则 -->
      <el-tab-pane label="预约规则" name="reservation">
        <div class="card">
          <div class="card-content">
            <el-form ref="reservationFormRef" :model="reservationConfig" label-width="150px">
              <el-form-item label="最大预约数量">
                <el-input-number v-model="reservationConfig.maxReservations" :min="1" :max="10" />
                <span class="form-tip">本</span>
              </el-form-item>

              <el-form-item label="预约保留期限">
                <el-input-number v-model="reservationConfig.holdDays" :min="1" :max="14" />
                <span class="form-tip">天（图书到馆后保留天数）</span>
              </el-form-item>

              <el-form-item label="自动通知">
                <el-switch v-model="reservationConfig.autoNotify" />
                <span class="form-tip">图书到馆后自动发送通知</span>
              </el-form-item>

              <el-form-item label="通知方式">
                <el-checkbox-group v-model="reservationConfig.notifyMethods">
                  <el-checkbox value="sms">短信通知</el-checkbox>
                  <el-checkbox value="email">邮件通知</el-checkbox>
                  <el-checkbox value="wechat">微信通知</el-checkbox>
                </el-checkbox-group>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleSave('reservation')">保存配置</el-button>
                <el-button @click="handleReset('reservation')">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>

      <!-- 系统设置 -->
      <el-tab-pane label="系统设置" name="system">
        <div class="card">
          <div class="card-content">
            <el-form ref="systemFormRef" :model="systemConfig" label-width="150px">
              <el-form-item label="图书馆名称">
                <el-input v-model="systemConfig.libraryName" placeholder="请输入图书馆名称" />
              </el-form-item>

              <el-form-item label="联系电话">
                <el-input v-model="systemConfig.contactPhone" placeholder="请输入联系电话" />
              </el-form-item>

              <el-form-item label="联系邮箱">
                <el-input v-model="systemConfig.contactEmail" placeholder="请输入联系邮箱" />
              </el-form-item>

              <el-form-item label="开馆时间">
                <el-time-picker
                  v-model="systemConfig.openTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="选择开馆时间"
                />
              </el-form-item>

              <el-form-item label="闭馆时间">
                <el-time-picker
                  v-model="systemConfig.closeTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="选择闭馆时间"
                />
              </el-form-item>

              <el-form-item label="休息日">
                <el-checkbox-group v-model="systemConfig.closedDays">
                  <el-checkbox value="0">周日</el-checkbox>
                  <el-checkbox value="1">周一</el-checkbox>
                  <el-checkbox value="2">周二</el-checkbox>
                  <el-checkbox value="3">周三</el-checkbox>
                  <el-checkbox value="4">周四</el-checkbox>
                  <el-checkbox value="5">周五</el-checkbox>
                  <el-checkbox value="6">周六</el-checkbox>
                </el-checkbox-group>
              </el-form-item>

              <el-form-item label="会话超时时间">
                <el-input-number v-model="systemConfig.sessionTimeout" :min="10" :max="1440" />
                <span class="form-tip">分钟</span>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleSave('system')">保存配置</el-button>
                <el-button @click="handleReset('system')">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, saveSystemConfig } from '@/api/system'

const activeTab = ref('borrow')

// 借阅规则配置 — keys map to backend config keys
const borrowConfig = reactive({
  student: {
    maxBooks: 5,
    borrowDays: 30,
    renewTimes: 2
  },
  teacher: {
    maxBooks: 10,
    borrowDays: 60,
    renewTimes: 3
  }
})

// 罚款规则配置
const fineConfig = reactive({
  graceDays: 3,
  finePerDay: 0.1,
  maxFine: 30,
  blockBorrowOnDebt: true,
  debtThreshold: 10
})

// 预约规则配置
const reservationConfig = reactive({
  maxReservations: 3,
  holdDays: 5,
  autoNotify: true,
  notifyMethods: ['sms', 'email']
})

// 系统设置
const systemConfig = reactive({
  libraryName: '国创睿峰智能图书馆',
  contactPhone: '400-xxx-xxxx',
  contactEmail: 'library@example.com',
  openTime: '08:00',
  closeTime: '22:00',
  closedDays: ['0'],
  sessionTimeout: 30
})

/**
 * Apply a flat config map returned by the backend to the local reactive objects.
 * Backend keys follow snake_case convention.
 */
const applyConfig = (cfg) => {
  // borrow rules
  if (cfg.student_max_borrow !== undefined)
    borrowConfig.student.maxBooks = Number(cfg.student_max_borrow)
  if (cfg.student_borrow_days !== undefined)
    borrowConfig.student.borrowDays = Number(cfg.student_borrow_days)
  if (cfg.student_renew_times !== undefined)
    borrowConfig.student.renewTimes = Number(cfg.student_renew_times)
  if (cfg.teacher_max_borrow !== undefined)
    borrowConfig.teacher.maxBooks = Number(cfg.teacher_max_borrow)
  if (cfg.teacher_borrow_days !== undefined)
    borrowConfig.teacher.borrowDays = Number(cfg.teacher_borrow_days)
  if (cfg.teacher_renew_times !== undefined)
    borrowConfig.teacher.renewTimes = Number(cfg.teacher_renew_times)

  // fine rules
  if (cfg.fine_grace_days !== undefined) fineConfig.graceDays = Number(cfg.fine_grace_days)
  if (cfg.fine_per_day !== undefined) fineConfig.finePerDay = Number(cfg.fine_per_day)
  if (cfg.fine_max !== undefined) fineConfig.maxFine = Number(cfg.fine_max)
  if (cfg.fine_block_on_debt !== undefined)
    fineConfig.blockBorrowOnDebt = cfg.fine_block_on_debt === 'true'
  if (cfg.fine_debt_threshold !== undefined)
    fineConfig.debtThreshold = Number(cfg.fine_debt_threshold)

  // reservation rules
  if (cfg.reservation_max !== undefined)
    reservationConfig.maxReservations = Number(cfg.reservation_max)
  if (cfg.reservation_hold_days !== undefined)
    reservationConfig.holdDays = Number(cfg.reservation_hold_days)
  if (cfg.reservation_auto_notify !== undefined)
    reservationConfig.autoNotify = cfg.reservation_auto_notify === 'true'
  if (cfg.reservation_notify_methods !== undefined) {
    try {
      reservationConfig.notifyMethods = JSON.parse(cfg.reservation_notify_methods)
    } catch {
      reservationConfig.notifyMethods = cfg.reservation_notify_methods.split(',')
    }
  }

  // system settings
  if (cfg.library_name !== undefined) systemConfig.libraryName = cfg.library_name
  if (cfg.contact_phone !== undefined) systemConfig.contactPhone = cfg.contact_phone
  if (cfg.contact_email !== undefined) systemConfig.contactEmail = cfg.contact_email
  if (cfg.open_time !== undefined) systemConfig.openTime = cfg.open_time
  if (cfg.close_time !== undefined) systemConfig.closeTime = cfg.close_time
  if (cfg.closed_days !== undefined) {
    try {
      systemConfig.closedDays = JSON.parse(cfg.closed_days)
    } catch {
      systemConfig.closedDays = cfg.closed_days.split(',')
    }
  }
  if (cfg.session_timeout !== undefined) systemConfig.sessionTimeout = Number(cfg.session_timeout)
}

// 加载配置
const loadConfig = async () => {
  try {
    const res = await getSystemConfig()
    if (res.code === 200 && res.data) {
      applyConfig(res.data)
    }
  } catch (error) {
    ElMessage.error('加载配置失败')
  }
}

/**
 * Build a flat kv map for the given tab and send to backend.
 */
const buildPayload = (type) => {
  if (type === 'borrow') {
    return {
      student_max_borrow: String(borrowConfig.student.maxBooks),
      student_borrow_days: String(borrowConfig.student.borrowDays),
      student_renew_times: String(borrowConfig.student.renewTimes),
      teacher_max_borrow: String(borrowConfig.teacher.maxBooks),
      teacher_borrow_days: String(borrowConfig.teacher.borrowDays),
      teacher_renew_times: String(borrowConfig.teacher.renewTimes)
    }
  }
  if (type === 'fine') {
    return {
      fine_grace_days: String(fineConfig.graceDays),
      fine_per_day: String(fineConfig.finePerDay),
      fine_max: String(fineConfig.maxFine),
      fine_block_on_debt: String(fineConfig.blockBorrowOnDebt),
      fine_debt_threshold: String(fineConfig.debtThreshold)
    }
  }
  if (type === 'reservation') {
    return {
      reservation_max: String(reservationConfig.maxReservations),
      reservation_hold_days: String(reservationConfig.holdDays),
      reservation_auto_notify: String(reservationConfig.autoNotify),
      reservation_notify_methods: JSON.stringify(reservationConfig.notifyMethods)
    }
  }
  if (type === 'system') {
    return {
      library_name: systemConfig.libraryName,
      contact_phone: systemConfig.contactPhone,
      contact_email: systemConfig.contactEmail,
      open_time: systemConfig.openTime,
      close_time: systemConfig.closeTime,
      closed_days: JSON.stringify(systemConfig.closedDays),
      session_timeout: String(systemConfig.sessionTimeout)
    }
  }
  return {}
}

// 保存配置
const handleSave = async (type) => {
  try {
    const data = buildPayload(type)
    const res = await saveSystemConfig(data)
    if (res.code === 200) {
      ElMessage.success('配置保存成功')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 重置配置（从服务器重新加载）
const handleReset = async (type) => {
  await loadConfig()
  ElMessage.info('配置已重置')
}

onMounted(loadConfig)
</script>

<style lang="scss" scoped>
.config-tabs {
  :deep(.el-tabs__content) {
    padding-top: 16px;
  }
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
