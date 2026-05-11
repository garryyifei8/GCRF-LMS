<template>
  <el-drawer
    v-model="visible"
    :title="`分配角色 — ${user?.realName || user?.username || ''}`"
    size="40%"
  >
    <div v-loading="loading">
      <h3>当前角色</h3>
      <el-table :data="userRoles" stripe empty-text="尚未分配任何角色">
        <el-table-column prop="code" label="角色" width="160" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" link @click="onRevoke(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />

      <h3>新增角色</h3>
      <el-form :model="form" inline>
        <el-form-item label="角色">
          <el-select v-model="form.roleCode" placeholder="选择角色" style="width: 200px">
            <el-option v-for="r in availableRoles" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker v-model="form.expiresAt" type="datetime" placeholder="留空=永久" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="!form.roleCode" @click="onAssign">添加</el-button>
        </el-form-item>
      </el-form>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoles, getUserRoles, assignRole, revokeRole } from '@/api/role'

const props = defineProps({
  modelValue: Boolean,
  user: Object
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const loading = ref(false)
const allRoles = ref([])
const userRoles = ref([])
const form = ref({ roleCode: '', expiresAt: null })

const availableRoles = computed(() => {
  const assigned = new Set(userRoles.value.map((r) => r.code))
  return allRoles.value.filter((r) => !assigned.has(r.code))
})

const load = async () => {
  if (!props.user?.id) return
  loading.value = true
  try {
    const [rolesRes, userRolesRes] = await Promise.all([getRoles(), getUserRoles(props.user.id)])
    allRoles.value = rolesRes.code === 200 ? rolesRes.data || [] : []
    userRoles.value = userRolesRes.code === 200 ? userRolesRes.data || [] : []
  } catch (e) {
    ElMessage.error('加载角色失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => [visible.value, props.user?.id],
  () => {
    if (visible.value) load()
  }
)

const onAssign = async () => {
  try {
    await assignRole(props.user.id, {
      roleCode: form.value.roleCode,
      expiresAt: form.value.expiresAt
    })
    ElMessage.success('角色添加成功')
    form.value = { roleCode: '', expiresAt: null }
    load()
  } catch (e) {
    ElMessage.error('添加失败 — 需要 REGION_ADMIN 权限')
  }
}

const onRevoke = async (row) => {
  try {
    await ElMessageBox.confirm(`撤销 ${row.code} 吗？`, '提示', { type: 'warning' })
    await revokeRole(props.user.id, row.id, null)
    ElMessage.success('已撤销')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('撤销失败')
  }
}
</script>
