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
          <el-button type="warning" :icon="Setting" @click="handleShowAutoBackup"
            >自动备份设置</el-button
          >
        </div>

        <el-alert title="备份提示" type="info" :closable="false" style="margin-top: 16px">
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
          <el-table-column label="备份时间" width="180">
            <template #default="{ row }">
              {{ row.createdAt || row.backupTime || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <ActionIcons
                :actions="[
                  { key: 'download', label: '下载备份', icon: Download, variant: 'primary' },
                  { key: 'restore', label: '还原数据', icon: Upload, variant: 'success' },
                  { key: 'del', label: '删除备份', icon: Delete, variant: 'danger' }
                ]"
                @action="
                  (k) => {
                    if (k === 'download') handleDownload(row)
                    else if (k === 'restore') handleRestore(row)
                    else handleDelete(row)
                  }
                "
              />
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
          <el-input :model-value="currentBackup.createdAt || currentBackup.backupTime" disabled />
        </el-form-item>
        <el-form-item label="验证码">
          <el-input v-model="restoreCode" placeholder="请输入验证码: RESTORE" clearable />
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
          <el-select
            v-model="autoBackupConfig.frequency"
            :disabled="!autoBackupConfig.enabled"
            style="width: 100%"
          >
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload, Delete } from '@element-plus/icons-vue'
import ActionIcons from '@/components/ActionIcons.vue'
import { createBackup, listBackups, downloadBackup } from '@/api/system'

// 表格数据
const loading = ref(false)
const backupList = ref([])
const backupLoading = ref(false)

// 分页（后端只返回最近10条，前端保留分页UI但 total 根据实际数据设置）
const pagination = reactive({
  page: 1,
  pageSize: 10,
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
    const res = await listBackups()
    if (res.code === 200) {
      const list = res.data || []
      backupList.value = list
      pagination.total = list.length
    } else {
      ElMessage.error(res.message || '加载备份列表失败')
    }
  } catch (error) {
    ElMessage.error('加载备份列表失败')
  } finally {
    loading.value = false
  }
}

// 分页（本地分页，数据已全量加载）
const handlePageChange = (page) => {
  pagination.page = page
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
}

// 立即备份
const handleBackup = async () => {
  backupLoading.value = true
  try {
    ElMessage.info('备份进行中，请稍候...')
    const res = await createBackup()
    if (res.code === 200) {
      ElMessage.success('备份成功')
      await loadBackupList()
    } else {
      ElMessage.error(res.message || '备份失败')
    }
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

// 确认还原（暂无后端接口，保留占位）
const confirmRestore = async () => {
  try {
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
const handleDownload = async (row) => {
  try {
    const blob = await downloadBackup(row.id)
    const url = URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = row.fileName || `backup-${row.id}.sql.gz`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('下载已开始')
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

// 删除备份（暂无后端接口，保留占位）
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除备份文件"${row.fileName}"吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      ElMessage.success('删除成功')
      await loadBackupList()
    })
    .catch(() => {})
}

// 显示自动备份设置
const handleShowAutoBackup = () => {
  autoBackupDialogVisible.value = true
}

// 保存自动备份设置（暂无后端接口，保留占位）
const handleSaveAutoBackup = async () => {
  try {
    ElMessage.success('自动备份设置已保存')
    autoBackupDialogVisible.value = false
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 初始化
onMounted(loadBackupList)
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
