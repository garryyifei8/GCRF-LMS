<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">角色管理</h1>
      <p class="page-header-description">管理系统角色和权限配置</p>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：角色列表 -->
      <el-col :xs="24" :md="8">
        <div class="card">
          <div class="card-header">
            <span>角色列表</span>
            <el-button type="primary" size="small" :icon="Plus" @click="handleAdd"
              >新增角色</el-button
            >
          </div>
          <div class="card-content">
            <el-table
              v-loading="loading"
              :data="roleList"
              highlight-current-row
              @current-change="handleSelectRole"
            >
              <el-table-column prop="name" label="角色名称" />
              <el-table-column label="用户数" width="80" align="center">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.userCount }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center">
                <template #default="{ row }">
                  <ActionIcons
                    :actions="[
                      { key: 'edit', label: '编辑', icon: Edit, variant: 'primary' },
                      { key: 'del', label: '删除', icon: Delete, variant: 'danger' }
                    ]"
                    @action="(k) => (k === 'edit' ? handleEdit(row) : handleDelete(row))"
                  />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>

      <!-- 右侧：权限配置 -->
      <el-col :xs="24" :md="16">
        <div class="card">
          <div class="card-header">
            <span>权限配置：{{ currentRole?.name || '请选择角色' }}</span>
            <el-button
              v-if="currentRole"
              type="primary"
              size="small"
              :icon="Check"
              @click="handleSavePermissions"
            >
              保存权限
            </el-button>
          </div>
          <div class="card-content">
            <el-empty v-if="!currentRole" description="请先选择一个角色进行权限配置" />
            <el-tree
              v-else
              ref="permissionTreeRef"
              :data="permissionTree"
              :props="{ label: 'label', children: 'children' }"
              node-key="id"
              show-checkbox
              default-expand-all
              :default-checked-keys="currentRole.permissions"
            >
              <template #default="{ node, data }">
                <span class="permission-node">
                  <el-icon v-if="data.icon"><component :is="data.icon" /></el-icon>
                  {{ node.label }}
                </span>
              </template>
            </el-tree>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 添加/编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleFormRules" label-width="100px">
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="roleForm.code" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>

        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="roleForm.sort" :min="0" style="width: 100%" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="roleForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Check } from '@element-plus/icons-vue'
import ActionIcons from '@/components/ActionIcons.vue'
import {
  getRoles,
  createRole,
  updateRole,
  deleteRole,
  assignPermissions,
  getPermissionTree
} from '@/api/system'

// 角色列表
const loading = ref(false)
const roleList = ref([])
const currentRole = ref(null)

// 权限树
const permissionTreeRef = ref(null)
const permissionTree = ref([])

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => (isEdit.value ? '编辑角色' : '新增角色'))

// 表单
const roleFormRef = ref(null)
const roleForm = reactive({
  id: null,
  code: '',
  name: '',
  sort: 0,
  remark: ''
})

const roleFormRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

// 加载权限树
const loadPermissionTree = async () => {
  try {
    const response = await getPermissionTree()
    if (response.code === 200 && response.data) {
      permissionTree.value = response.data
    } else {
      // 如果API未实现，使用默认权限树
      permissionTree.value = [
        {
          id: 'books',
          label: '图书管理',
          icon: 'Reading',
          children: [
            { id: 'books:list', label: '图书列表' },
            { id: 'books:add', label: '新增图书' },
            { id: 'books:edit', label: '编辑图书' },
            { id: 'books:delete', label: '删除图书' },
            { id: 'books:catalog', label: '图书编目' }
          ]
        },
        {
          id: 'readers',
          label: '读者管理',
          icon: 'User',
          children: [
            { id: 'readers:list', label: '读者列表' },
            { id: 'readers:add', label: '新增读者' },
            { id: 'readers:edit', label: '编辑读者' },
            { id: 'readers:delete', label: '删除读者' },
            { id: 'readers:import', label: '批量导入' }
          ]
        },
        {
          id: 'circulation',
          label: '流通管理',
          icon: 'Tickets',
          children: [
            { id: 'circulation:borrow', label: '图书借出' },
            { id: 'circulation:return', label: '图书归还' },
            { id: 'circulation:records', label: '流通记录' },
            { id: 'circulation:reservations', label: '预约管理' }
          ]
        },
        {
          id: 'system',
          label: '系统管理',
          icon: 'Setting',
          children: [
            { id: 'system:users', label: '用户管理' },
            { id: 'system:roles', label: '角色管理' },
            { id: 'system:config', label: '系统配置' },
            { id: 'system:backup', label: '数据备份' }
          ]
        }
      ]
    }
  } catch (error) {
    console.error('Failed to load permission tree:', error)
    // 使用默认权限树
    permissionTree.value = [
      {
        id: 'books',
        label: '图书管理',
        icon: 'Reading',
        children: [
          { id: 'books:list', label: '图书列表' },
          { id: 'books:add', label: '新增图书' },
          { id: 'books:edit', label: '编辑图书' },
          { id: 'books:delete', label: '删除图书' },
          { id: 'books:catalog', label: '图书编目' }
        ]
      },
      {
        id: 'readers',
        label: '读者管理',
        icon: 'User',
        children: [
          { id: 'readers:list', label: '读者列表' },
          { id: 'readers:add', label: '新增读者' },
          { id: 'readers:edit', label: '编辑读者' },
          { id: 'readers:delete', label: '删除读者' },
          { id: 'readers:import', label: '批量导入' }
        ]
      },
      {
        id: 'circulation',
        label: '流通管理',
        icon: 'Tickets',
        children: [
          { id: 'circulation:borrow', label: '图书借出' },
          { id: 'circulation:return', label: '图书归还' },
          { id: 'circulation:records', label: '流通记录' },
          { id: 'circulation:reservations', label: '预约管理' }
        ]
      },
      {
        id: 'system',
        label: '系统管理',
        icon: 'Setting',
        children: [
          { id: 'system:users', label: '用户管理' },
          { id: 'system:roles', label: '角色管理' },
          { id: 'system:config', label: '系统配置' },
          { id: 'system:backup', label: '数据备份' }
        ]
      }
    ]
  }
}

// 加载角色列表
const loadRoleList = async () => {
  loading.value = true
  try {
    const response = await getRoles({
      pageNum: 1,
      pageSize: 100 // 不分页，获取所有角色
    })

    if (response.code === 200 && response.data) {
      roleList.value = response.data.records || []

      // 自动选择第一个角色
      if (roleList.value.length > 0 && !currentRole.value) {
        currentRole.value = roleList.value[0]
      }
    } else {
      roleList.value = []
    }
  } catch (error) {
    console.error('Failed to load role list:', error)
    ElMessage.error('加载角色列表失败')
    roleList.value = []
  } finally {
    loading.value = false
  }
}

// 选择角色
const handleSelectRole = (role) => {
  currentRole.value = role
}

// 新增角色
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑角色
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(roleForm, row)
  dialogVisible.value = true
}

// 删除角色
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除角色"${row.name}"吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteRole(row.id)
        ElMessage.success('删除成功')

        // 如果删除的是当前选中的角色，清空选择
        if (currentRole.value?.id === row.id) {
          currentRole.value = null
        }

        await loadRoleList()
      } catch (error) {
        console.error('Failed to delete role:', error)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  try {
    await roleFormRef.value.validate()

    loading.value = true

    // 准备提交数据
    const submitData = {
      code: roleForm.code,
      name: roleForm.name,
      sort: roleForm.sort,
      remark: roleForm.remark
    }

    // 调用API
    if (isEdit.value) {
      await updateRole(roleForm.id, submitData)
      ElMessage.success('编辑成功')
    } else {
      await createRole(submitData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    await loadRoleList()
  } catch (error) {
    console.error('Failed to submit role:', error)
    if (error !== false) {
      ElMessage.error(isEdit.value ? '编辑失败' : '添加失败')
    }
  } finally {
    loading.value = false
  }
}

// 保存权限
const handleSavePermissions = async () => {
  if (!currentRole.value) {
    ElMessage.warning('请先选择角色')
    return
  }

  const checkedKeys = permissionTreeRef.value.getCheckedKeys()
  const halfCheckedKeys = permissionTreeRef.value.getHalfCheckedKeys()
  const permissions = [...checkedKeys, ...halfCheckedKeys]

  try {
    loading.value = true

    await assignPermissions(currentRole.value.id, permissions)

    // 更新本地数据
    currentRole.value.permissions = permissions

    ElMessage.success('权限保存成功')
  } catch (error) {
    console.error('Failed to save permissions:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

// 重置表单
const resetForm = () => {
  roleForm.id = null
  roleForm.code = ''
  roleForm.name = ''
  roleForm.sort = 0
  roleForm.remark = ''
  roleFormRef.value?.resetFields()
}

// 初始化
const init = async () => {
  await loadPermissionTree()
  await loadRoleList()
}

init()
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
}

.permission-node {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
