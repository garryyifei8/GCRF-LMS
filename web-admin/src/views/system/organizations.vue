<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-header-title">组织架构管理</h1>
      <p class="page-header-description">教育局 → 学校 → 年级 → 班级 多级组织树</p>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：组织树 -->
      <el-col :xs="24" :md="10">
        <div class="card">
          <div class="card-header">
            <span>组织树</span>
            <div>
              <el-button size="small" type="primary" :icon="Plus" @click="onAddRoot"
                >新增根节点</el-button
              >
              <el-button size="small" :icon="Upload" @click="onImportClick">批量导入</el-button>
              <input ref="fileInput" type="file" accept=".xlsx" hidden @change="onImport" />
            </div>
          </div>
          <div class="card-content">
            <el-tree
              v-loading="loading"
              :data="tree"
              node-key="id"
              :props="treeProps"
              highlight-current
              lazy
              :load="loadChildren"
              @node-click="onSelect"
            >
              <template #default="{ node, data }">
                <span class="org-node-row">
                  <el-tag size="small" :type="tagType(data.type)">{{
                    typeLabel(data.type)
                  }}</el-tag>
                  <span class="org-node-name">{{ node.label }}</span>
                  <span class="org-node-code">[{{ data.code }}]</span>
                </span>
              </template>
            </el-tree>
          </div>
        </div>
      </el-col>

      <!-- 右侧：节点详情 -->
      <el-col :xs="24" :md="14">
        <div class="card">
          <div class="card-header">
            <span>{{ current ? '节点详情' : '请选择节点' }}</span>
            <div v-if="current">
              <el-button size="small" :icon="Plus" @click="onAddChild">添加子节点</el-button>
              <el-button v-if="canCreateSchool" size="small" type="success" @click="onCreateSchool"
                >建学校</el-button
              >
              <el-button size="small" type="danger" :icon="Delete" @click="onDelete"
                >删除</el-button
              >
            </div>
          </div>
          <div v-if="current" class="card-content">
            <el-form label-width="120px">
              <el-form-item label="ID">{{ current.id }}</el-form-item>
              <el-form-item label="类型">{{ typeLabel(current.type) }}</el-form-item>
              <el-form-item label="路径">{{ current.path }}</el-form-item>
              <el-form-item label="租户 schema">
                <el-tag v-if="current.tenantSchema">{{ current.tenantSchema }}</el-tag>
                <span v-else class="text-muted">—</span>
              </el-form-item>
              <el-form-item label="名称">
                <el-input v-model="editName" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="editStatus">
                  <el-option label="ACTIVE" value="ACTIVE" />
                  <el-option label="INACTIVE" value="INACTIVE" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onSave">保存</el-button>
              </el-form-item>
            </el-form>
          </div>
          <div v-else class="card-content empty-hint">
            <el-empty description="请从左侧选择一个节点" />
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 新增节点对话框 -->
    <el-dialog
      v-model="addVisible"
      :title="addParent ? `在 ${addParent.name} 下新增子节点` : '新增根节点'"
      width="500px"
    >
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="类型" required>
          <el-select v-model="addForm.type" placeholder="选择类型">
            <el-option v-for="t in availableTypes" :key="t" :label="typeLabel(t)" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="addForm.name" placeholder="请输入节点名称" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="addForm.code" placeholder="字母数字下划线" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="onAddSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 建学校对话框 -->
    <el-dialog v-model="schoolVisible" title="新建学校（自动建 schema）" width="500px">
      <el-form :model="schoolForm" label-width="100px">
        <el-form-item label="学校名称" required>
          <el-input v-model="schoolForm.name" placeholder="请输入学校名称" />
        </el-form-item>
        <el-form-item label="学校编码" required>
          <el-input v-model="schoolForm.code" placeholder="字母数字下划线" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="schoolVisible = false">取消</el-button>
        <el-button type="success" :loading="schoolLoading" @click="onSchoolSubmit"
          >建学校</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Upload } from '@element-plus/icons-vue'
import {
  listOrgNodes,
  createOrgNode,
  updateOrgNode,
  deleteOrgNode,
  createSchool,
  importOrgExcel
} from '@/api/org'

// ──────────────────────────────────────────────
// 类型定义
// ──────────────────────────────────────────────
const TYPE_LABELS = {
  REGION: '教育局',
  DISTRICT: '区县',
  SCHOOL: '学校',
  SUB_SCHOOL: '分校',
  BRANCH: '分馆',
  STAGE: '学段',
  GRADE: '年级',
  CLASS: '班级'
}

/**
 * 允许作为子节点的父类型约束
 * key = 子类型, value = 允许的父类型列表
 */
const PARENT_RULES = {
  REGION: [],
  DISTRICT: ['REGION'],
  SCHOOL: ['REGION', 'DISTRICT'],
  SUB_SCHOOL: ['SCHOOL'],
  BRANCH: ['SCHOOL', 'SUB_SCHOOL'],
  STAGE: ['SCHOOL', 'SUB_SCHOOL'],
  GRADE: ['SCHOOL', 'SUB_SCHOOL', 'STAGE'],
  CLASS: ['GRADE']
}

function typeLabel(t) {
  return TYPE_LABELS[t] || t
}

function tagType(t) {
  if (t === 'REGION') return 'danger'
  if (t === 'DISTRICT') return 'warning'
  if (t === 'SCHOOL') return 'success'
  return ''
}

// ──────────────────────────────────────────────
// 树状态
// ──────────────────────────────────────────────
const tree = ref([])
const loading = ref(false)

const treeProps = {
  label: 'name',
  children: 'children',
  isLeaf: 'leaf'
}

async function loadRoots() {
  loading.value = true
  try {
    const res = await listOrgNodes()
    const nodes = res.data || []
    tree.value = nodes.map((n) => ({
      ...n,
      leaf: false
    }))
  } catch (e) {
    ElMessage.error('加载组织树失败')
  } finally {
    loading.value = false
  }
}

/**
 * el-tree lazy load 回调
 */
async function loadChildren(node, resolve) {
  if (node.level === 0) {
    // 根节点由 loadRoots 负责，这里直接 resolve 空数组
    resolve([])
    return
  }
  try {
    const res = await listOrgNodes(node.data.id)
    const children = (res.data || []).map((n) => ({
      ...n,
      leaf: false
    }))
    resolve(children)
  } catch (e) {
    resolve([])
  }
}

// ──────────────────────────────────────────────
// 当前选中节点
// ──────────────────────────────────────────────
const current = ref(null)
const editName = ref('')
const editStatus = ref('ACTIVE')

function onSelect(data) {
  current.value = data
  editName.value = data.name
  editStatus.value = data.status || 'ACTIVE'
}

// ──────────────────────────────────────────────
// 新增节点对话框
// ──────────────────────────────────────────────
const addVisible = ref(false)
const addLoading = ref(false)
const addParent = ref(null)
const addForm = reactive({ type: '', name: '', code: '' })

const availableTypes = computed(() => {
  if (!addParent.value) return ['REGION']
  const parentType = addParent.value.type
  return Object.keys(PARENT_RULES).filter((t) => PARENT_RULES[t].includes(parentType))
})

function onAddRoot() {
  addParent.value = null
  addForm.type = 'REGION'
  addForm.name = ''
  addForm.code = ''
  addVisible.value = true
}

function onAddChild() {
  if (!current.value) return
  addParent.value = current.value
  addForm.type = availableTypes.value[0] || ''
  addForm.name = ''
  addForm.code = ''
  addVisible.value = true
}

async function onAddSubmit() {
  if (!addForm.type || !addForm.name || !addForm.code) {
    ElMessage.warning('请填写完整信息')
    return
  }
  addLoading.value = true
  try {
    await createOrgNode({
      parentId: addParent.value?.id ?? null,
      type: addForm.type,
      name: addForm.name,
      code: addForm.code
    })
    ElMessage.success('节点已创建')
    addVisible.value = false
    await loadRoots()
  } catch (e) {
    // error already shown by request interceptor
  } finally {
    addLoading.value = false
  }
}

// ──────────────────────────────────────────────
// 保存 / 删除
// ──────────────────────────────────────────────
async function onSave() {
  if (!current.value) return
  try {
    await updateOrgNode(current.value.id, {
      name: editName.value,
      status: editStatus.value
    })
    ElMessage.success('已保存')
    current.value.name = editName.value
    current.value.status = editStatus.value
  } catch (e) {
    // handled by interceptor
  }
}

async function onDelete() {
  if (!current.value) return
  try {
    await ElMessageBox.confirm(
      `确定删除节点 "${current.value.name}"？删除后子节点将一并移除，且不可恢复。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteOrgNode(current.value.id)
    ElMessage.success('已删除')
    current.value = null
    await loadRoots()
  } catch (e) {
    if (e !== 'cancel') {
      // handled by interceptor
    }
  }
}

// ──────────────────────────────────────────────
// 建学校（自动建 schema）
// ──────────────────────────────────────────────
const schoolVisible = ref(false)
const schoolLoading = ref(false)
const schoolForm = reactive({ name: '', code: '' })

const canCreateSchool = computed(
  () => current.value && (current.value.type === 'REGION' || current.value.type === 'DISTRICT')
)

function onCreateSchool() {
  schoolForm.name = ''
  schoolForm.code = ''
  schoolVisible.value = true
}

async function onSchoolSubmit() {
  if (!schoolForm.name || !schoolForm.code) {
    ElMessage.warning('请填写学校名称和编码')
    return
  }
  schoolLoading.value = true
  try {
    const res = await createSchool({
      parentId: current.value.id,
      name: schoolForm.name,
      code: schoolForm.code
    })
    ElMessage.success(`学校已创建，schema=${res.data?.tenantSchema || ''}`)
    schoolVisible.value = false
    await loadRoots()
  } catch (e) {
    // handled by interceptor
  } finally {
    schoolLoading.value = false
  }
}

// ──────────────────────────────────────────────
// 批量导入 Excel
// ──────────────────────────────────────────────
const fileInput = ref()

function onImportClick() {
  fileInput.value.click()
}

async function onImport(ev) {
  const file = ev.target.files[0]
  if (!file) return
  try {
    const res = await importOrgExcel(file)
    ElMessage.success(`导入完成：成功 ${res.data?.created ?? 0} / 失败 ${res.data?.failed ?? 0}`)
    await loadRoots()
  } catch (e) {
    // handled by interceptor
  } finally {
    ev.target.value = ''
  }
}

// ──────────────────────────────────────────────
// 初始化
// ──────────────────────────────────────────────
onMounted(loadRoots)
</script>

<style scoped>
.org-node-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.org-node-name {
  font-weight: 500;
}

.org-node-code {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.text-muted {
  color: var(--el-text-color-secondary);
}

.empty-hint {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}
</style>
