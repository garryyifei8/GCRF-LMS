<template>
  <div class="departments-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增部门
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="部门编码">
          <el-input
            v-model="queryParams.deptCode"
            placeholder="请输入部门编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="部门名称">
          <el-input
            v-model="queryParams.deptName"
            placeholder="请输入部门名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="正常" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetQuery">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table v-loading="loading" :data="departmentList" border stripe>
        <el-table-column prop="deptCode" label="部门编码" width="120" />
        <el-table-column prop="deptName" label="部门名称" width="150" />
        <el-table-column prop="deptLevel" label="层级" width="80" />
        <el-table-column prop="deptPath" label="部门路径" width="150" />
        <el-table-column prop="leaderName" label="负责人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="部门编码" prop="deptCode">
          <el-input
            v-model="formData.deptCode"
            placeholder="请输入部门编码"
            :disabled="formData.id"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="formData.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-select v-model="formData.parentId" placeholder="请选择上级部门" clearable>
            <el-option
              v-for="dept in parentDeptOptions"
              :key="dept.id"
              :label="dept.deptName"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import {
  getDepartmentList,
  createDepartment,
  updateDepartment,
  deleteDepartment
} from '@/api/department'

// 查询参数
const queryParams = reactive({
  deptCode: '',
  deptName: '',
  status: '',
  pageNum: 1,
  pageSize: 20
})

// 数据
const loading = ref(false)
const total = ref(0)
const departmentList = ref([])
const parentDeptOptions = ref([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const formData = reactive({
  id: null,
  deptCode: '',
  deptName: '',
  parentId: null,
  phone: '',
  email: '',
  description: ''
})

// 表单验证规则
const formRules = {
  deptCode: [
    { required: true, message: '请输入部门编码', trigger: 'blur' },
    { max: 50, message: '部门编码长度不能超过50个字符', trigger: 'blur' }
  ],
  deptName: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { max: 100, message: '部门名称长度不能超过100个字符', trigger: 'blur' }
  ]
}

// 查询部门列表
const fetchDepartmentList = async () => {
  loading.value = true
  try {
    const res = await getDepartmentList(queryParams)
    departmentList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('Failed to fetch departments:', error)
    ElMessage.error('获取部门列表失败')
  } finally {
    loading.value = false
  }
}

// 加载父级部门选项
const loadParentDeptOptions = async () => {
  try {
    const res = await getDepartmentList({ pageNum: 1, pageSize: 100 })
    parentDeptOptions.value = res.data.records || []
  } catch (error) {
    console.error('Failed to load parent departments:', error)
  }
}

// 查询按钮
const handleQuery = () => {
  queryParams.pageNum = 1
  fetchDepartmentList()
}

// 重置按钮
const resetQuery = () => {
  queryParams.deptCode = ''
  queryParams.deptName = ''
  queryParams.status = ''
  queryParams.pageNum = 1
  queryParams.pageSize = 20
  fetchDepartmentList()
}

// 新增按钮
const handleAdd = async () => {
  await loadParentDeptOptions()
  dialogTitle.value = '新增部门'
  dialogVisible.value = true
}

// 编辑按钮
const handleEdit = async (row) => {
  await loadParentDeptOptions()
  dialogTitle.value = '编辑部门'
  Object.assign(formData, {
    id: row.id,
    deptCode: row.deptCode,
    deptName: row.deptName,
    parentId: row.parentId,
    phone: row.phone,
    email: row.email,
    description: row.description
  })
  dialogVisible.value = true
}

// 删除按钮
const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除部门"${row.deptName}"吗?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteDepartment(row.id)
        ElMessage.success('删除成功')
        await fetchDepartmentList()
      } catch (error) {
        console.error('Failed to delete department:', error)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

// 提交表单
const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      if (formData.id) {
        await updateDepartment(formData)
        ElMessage.success('更新成功')
      } else {
        await createDepartment(formData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await fetchDepartmentList()
    } catch (error) {
      console.error('Failed to save department:', error)
      ElMessage.error('保存失败')
    }
  })
}

// 对话框关闭事件
const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: null,
    deptCode: '',
    deptName: '',
    parentId: null,
    phone: '',
    email: '',
    description: ''
  })
}

// 初始化
onMounted(() => {
  fetchDepartmentList()
})
</script>

<style scoped>
.departments-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
