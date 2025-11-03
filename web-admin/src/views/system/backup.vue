<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">数据备份</h1>
      <p class="page-header-description">数据库备份与恢复管理</p>
    </div>

    <!-- 备份操作 -->
    <div class="card mb-md">
      <div class="card-content">
        <div class="backup-actions">
          <el-button type="primary" :icon="Download" :loading="backupLoading" @click="handleBackup">
            立即备份
          </el-button>
          <el-button type="success" :icon="Upload" @click="handleShowRestore">还原备份</el-button>
          <el-button type="warning" :icon="Setting" @click="handleShowAutoBackup">自动备份设置</el-button>
        </div>

        <el-alert
          title="备份提示"
          type="info"
          :closable="false"
          style="margin-top: 16px"
        >
          <template #default>
            <ul class="backup-tips">
              <li>建议定期备份数据，确保数据安全</li>
              <li>备份文件包含所有业务数据，请妥善保管</li>
              <li>还原备份将覆盖当前数据，请谨慎操作</li>
              <li>可以设置自动备份，系统将按计划自动备份数据</li>
            </ul>
          </template>
        </el-alert>
      </div>
    </div>

    <!-- 备份记录 -->
    <div class="card">
      <div class="card-header">
        <span>备份记录</span>
        <el-button size="small" :icon="Refresh" @click="loadBackupList">刷新</el-button>
      </div>
      <div class="card-content">
        <el-table v-loading="loading" :data="backupList" stripe>
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="fileName" label="备份文件" show-overflow-tooltip min-width="250" />
          <el-table-column label="备份类型" width="120">
            <template #default="{ row }">
              <el-tag :type="row.type === 'auto' ? 'success' : ''">
                {{ row.type === 'auto' ? '自动备份' : '手动备份' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="文件大小" width="120" />
          <el-table-column prop="backupTime" label="备份时间" width="160" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :icon="Download" @click="handleDownload(row)">下载</el-button>
              <el-button type="success" link :icon="Upload" @click="handleRestore(row)">还原</el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 还原确认对话框 -->
    <el-dialog v-model="restoreDialogVisible" title="还原备份" width="500px">
      <el-alert title="警告" type="warning" :closable="false" style="margin-bottom: 16px">
        还原备份将覆盖当前所有数据，此操作不可恢复！请确认是否继续？
      </el-alert>

      <el-form label-width="100px">
        <el-form-item label="备份文件">
          <el-input v-model="currentBackup.fileName" disabled />
        </el-form-item>
        <el-form-item label="备份时间">
          <el-input v-model="currentBackup.backupTime" disabled />
        </el-form-item>
        <el-form-item label="验证码">
          <el-input
            v-model="restoreCode"
            placeholder="请输入验证码: RESTORE"
            clearable
          />
          <div class="form-tip">请输入"RESTORE"确认还原操作</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="restoreDialogVisible = false">取消</el-button>
        <el-button type="danger" :disabled="restoreCode !== 'RESTORE'" @click="confirmRestore">
          确认还原
        </el-button>
      </template>
    </el-dialog>

    <!-- 自动备份设置对话框 -->
    <el-dialog v-model="autoBackupDialogVisible" title="自动备份设置" width="500px">
      <el-form :model="autoBackupConfig" label-width="120px">
        <el-form-item label="启用自动备份">
          <el-switch v-model="autoBackupConfig.enabled" />
        </el-form-item>

        <el-form-item label="备份频率">
          <el-select v-model="autoBackupConfig.frequency" :disabled="!autoBackupConfig.enabled" style="width: 100%">
            <el-option label="每天" value="daily" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
          </el-select>
        </el-form-item>

        <el-form-item label="备份时间">
          <el-time-picker
            v-model="autoBackupConfig.time"
            format="HH:mm"
            value-format="HH:mm"
            :disabled="!autoBackupConfig.enabled"
            placeholder="选择备份时间"
          />
        </el-form-item>

        <el-form-item label="保留数量">
          <el-input-number
            v-model="autoBackupConfig.keepCount"
            :min="1"
            :max="30"
            :disabled="!autoBackupConfig.enabled"
          />
          <span class="form-tip">份（超过将自动删除最旧的备份）</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="autoBackupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAutoBackup">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 表格数据
const loading = ref(false)
const backupList = ref([])
const backupLoading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 还原对话框
const restoreDialogVisible = ref(false)
const restoreCode = ref('')
const currentBackup = ref({})

// 自动备份设置
const autoBackupDialogVisible = ref(false)
const autoBackupConfig = reactive({
  enabled: true,
  frequency: 'daily',
  time: '02:00',
  keepCount: 7
})

// 加载备份列表
const loadBackupList = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取备份列表
    await new Promise((resolve) => setTimeout(resolve, 500))

    const mockData = []
    for (let i = 1; i <= pagination.pageSize; i++) {
      const id = (pagination.page - 1) * pagination.pageSize + i
      const date = new Date()
      date.setDate(date.getDate() - i)

      mockData.push({
        id,
        fileName: `backup_${date.getFullYear()}${(date.getMonth() + 1).toString().padStart(2, '0')}${date
          .getDate()
          .toString()
          .padStart(2, '0')}_${date.getHours().toString().padStart(2, '0')}${date
          .getMinutes()
          .toString()
          .padStart(2, '0')}.sql`,
        type: i % 3 === 0 ? 'auto' : 'manual',
        fileSize: `${(Math.random() * 50 + 10).toFixed(2)} MB`,
        backupTime: date.toLocaleString('zh-CN')
      })
    }

    backupList.value = mockData
    pagination.total = 45
  } catch (error) {
    ElMessage.error('加载备份列表失败')
  } finally {
    loading.value = false
  }
}

// 分页
const handlePageChange = (page) => {
  pagination.page = page
  loadBackupList()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadBackupList()
}

// 立即备份
const handleBackup = async () => {
  backupLoading.value = true
  try {
    // TODO: 调用API执行备份
    await new Promise((resolve) => setTimeout(resolve, 2000))

    ElMessage.success('备份成功')
    loadBackupList()
  } catch (error) {
    ElMessage.error('备份失败')
  } finally {
    backupLoading.value = false
  }
}

// 显示还原对话框
const handleShowRestore = () => {
  if (backupList.value.length === 0) {
    ElMessage.warning('暂无备份文件')
    return
  }
  // 默认选择最新的备份
  currentBackup.value = backupList.value[0]
  restoreCode.value = ''
  restoreDialogVisible.value = true
}

// 还原备份
const handleRestore = (row) => {
  currentBackup.value = row
  restoreCode.value = ''
  restoreDialogVisible.value = true
}

// 确认还原
const confirmRestore = async () => {
  try {
    // TODO: 调用API还原备份
    await new Promise((resolve) => setTimeout(resolve, 2000))

    ElMessage.success('还原成功，系统将在3秒后刷新页面')
    restoreDialogVisible.value = false

    setTimeout(() => {
      window.location.reload()
    }, 3000)
  } catch (error) {
    ElMessage.error('还原失败')
  }
}

// 下载备份
const handleDownload = (row) => {
  // TODO: 实现文件下载
  ElMessage.success(`开始下载：${row.fileName}`)
}

// 删除备份
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除备份文件"${row.fileName}"吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      // TODO: 调用API删除备份
      await new Promise((resolve) => setTimeout(resolve, 500))
      ElMessage.success('删除成功')
      loadBackupList()
    })
    .catch(() => {})
}

// 显示自动备份设置
const handleShowAutoBackup = () => {
  autoBackupDialogVisible.value = true
}

// 保存自动备份设置
const handleSaveAutoBackup = async () => {
  try {
    // TODO: 调用API保存自动备份配置
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success('自动备份设置已保存')
    autoBackupDialogVisible.value = false
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 初始化
loadBackupList()
</script>

<style lang="scss" scoped>
.backup-actions {
  display: flex;
  gap: 12px;
}

.backup-tips {
  margin: 0;
  padding-left: 20px;

  li {
    margin: 4px 0;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
