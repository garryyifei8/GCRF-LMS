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
            <el-button type="primary" size="small" :icon="Plus" @click="handleAdd">新增角色</el-button>
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
              <el-table-column label="操作" width="140" align="center">
                <template #default="{ row }">
                  <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
                  <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

// 角色列表
const loading = ref(false)
const roleList = ref([])
const currentRole = ref(null)

// 权限树
const permissionTreeRef = ref(null)
const permissionTree = ref([
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
])

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

// 加载角色列表
const loadRoleList = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取角色列表
    await new Promise((resolve) => setTimeout(resolve, 500))

    roleList.value = [
      {
        id: 1,
        code: 'admin',
        name: '超级管理员',
        userCount: 2,
        permissions: [
          'books',
          'books:list',
          'books:add',
          'books:edit',
          'books:delete',
          'books:catalog',
          'readers',
          'readers:list',
          'readers:add',
          'readers:edit',
          'readers:delete',
          'readers:import',
          'circulation',
          'circulation:borrow',
          'circulation:return',
          'circulation:records',
          'circulation:reservations',
          'system',
          'system:users',
          'system:roles',
          'system:config',
          'system:backup'
        ],
        sort: 1
      },
      {
        id: 2,
        code: 'manager',
        name: '管理员',
        userCount: 5,
        permissions: [
          'books',
          'books:list',
          'books:add',
          'books:edit',
          'books:catalog',
          'readers',
          'readers:list',
          'readers:add',
          'readers:edit',
          'circulation',
          'circulation:borrow',
          'circulation:return',
          'circulation:records',
          'circulation:reservations'
        ],
        sort: 2
      },
      {
        id: 3,
        code: 'librarian',
        name: '图书管理员',
        userCount: 10,
        permissions: [
          'books',
          'books:list',
          'circulation',
          'circulation:borrow',
          'circulation:return',
          'circulation:records'
        ],
        sort: 3
      }
    ]

    if (roleList.value.length > 0) {
      currentRole.value = roleList.value[0]
    }
  } catch (error) {
    ElMessage.error('加载角色列表失败')
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
      // TODO: 调用API删除角色
      await new Promise((resolve) => setTimeout(resolve, 500))
      ElMessage.success('删除成功')
      loadRoleList()
    })
    .catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  try {
    await roleFormRef.value.validate()

    // TODO: 调用API保存角色
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
    dialogVisible.value = false
    loadRoleList()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('操作失败')
    }
  }
}

// 保存权限
const handleSavePermissions = async () => {
  const checkedKeys = permissionTreeRef.value.getCheckedKeys()
  const halfCheckedKeys = permissionTreeRef.value.getHalfCheckedKeys()
  const permissions = [...checkedKeys, ...halfCheckedKeys]

  try {
    // TODO: 调用API保存权限
    await new Promise((resolve) => setTimeout(resolve, 500))

    currentRole.value.permissions = permissions
    ElMessage.success('权限保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
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
loadRoleList()
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
