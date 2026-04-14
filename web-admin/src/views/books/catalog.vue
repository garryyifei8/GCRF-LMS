<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">图书编目</h1>
      <p class="page-header-description">添加新图书到馆藏系统</p>
    </div>

    <!-- 编目模式选择 -->
    <div class="card mb-md">
      <div class="card-content">
        <el-radio-group v-model="catalogMode" size="large" @change="handleModeChange">
          <el-radio-button value="smart">
            <el-icon><MagicStick /></el-icon>
            智能编目
          </el-radio-button>
          <el-radio-button value="manual">
            <el-icon><Edit /></el-icon>
            手工编目
          </el-radio-button>
        </el-radio-group>
        <div class="mode-description">
          <span v-if="catalogMode === 'smart'">
            <el-icon><InfoFilled /></el-icon>
            扫描或输入ISBN，系统自动获取图书信息
          </span>
          <span v-else>
            <el-icon><InfoFilled /></el-icon>
            手动填写完整的图书信息
          </span>
        </div>
      </div>
    </div>

    <!-- 智能编目：ISBN查询 -->
    <div v-if="catalogMode === 'smart'" class="card mb-md">
      <div class="card-content">
        <div class="isbn-search">
          <el-input
            v-model="isbnInput"
            placeholder="请扫描或输入ISBN号"
            clearable
            size="large"
            style="max-width: 400px"
            @keyup.enter="handleISBNSearch"
          >
            <template #prefix>
              <el-icon><Barcode /></el-icon>
            </template>
          </el-input>
          <el-button
            type="primary"
            size="large"
            :icon="Search"
            :loading="isbnSearching"
            :disabled="isbnSearching"
            @click="handleISBNSearch"
          >
            <ButtonLoading v-if="isbnSearching" text="查询中..." size="default" />
            <span v-else>查询图书信息</span>
          </el-button>
        </div>
        <div class="isbn-tip">
          <el-icon><Warning /></el-icon>
          支持ISBN-10和ISBN-13格式，系统将自动从国家图书馆、豆瓣等数据源获取图书信息
        </div>
      </div>
    </div>

    <!-- 图书信息表单 -->
    <div v-if="showForm" class="card">
      <div class="card-content">
        <el-form
          ref="bookFormRef"
          :model="bookForm"
          :rules="bookFormRules"
          label-width="120px"
          size="default"
        >
          <el-row :gutter="20">
            <!-- 左侧：基本信息 -->
            <el-col :xs="24" :md="16">
              <div class="form-section">
                <h3 class="form-section-title">基本信息</h3>

                <el-form-item label="ISBN" prop="isbn">
                  <el-input
                    v-model="bookForm.isbn"
                    placeholder="请输入ISBN号"
                    :disabled="catalogMode === 'smart' && isbnFetched"
                  >
                    <template #append>
                      <el-button
                        v-if="catalogMode === 'manual'"
                        :icon="Search"
                        @click="handleISBNSearch"
                      >
                        查询
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="书名" prop="title">
                  <el-input v-model="bookForm.title" placeholder="请输入书名" />
                </el-form-item>

                <el-form-item label="作者" prop="author">
                  <el-input v-model="bookForm.author" placeholder="请输入作者姓名" />
                </el-form-item>

                <el-form-item label="出版社" prop="publisher">
                  <el-input v-model="bookForm.publisher" placeholder="请输入出版社" />
                </el-form-item>

                <el-form-item label="出版日期" prop="publishDate">
                  <el-date-picker
                    v-model="bookForm.publishDate"
                    type="date"
                    placeholder="请选择出版日期"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>

                <el-form-item label="分类" prop="category">
                  <el-select
                    v-model="bookForm.category"
                    placeholder="请选择图书分类"
                    style="width: 100%"
                    :loading="categoryLoading"
                    filterable
                  >
                    <el-option
                      v-for="cat in categoryOptions"
                      :key="cat.value"
                      :label="cat.label"
                      :value="cat.value"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="定价" prop="price">
                  <el-input-number
                    v-model="bookForm.price"
                    :min="0"
                    :precision="2"
                    :step="1"
                    style="width: 100%"
                  />
                  <span class="form-tip">单位：元</span>
                </el-form-item>

                <el-form-item label="馆藏数量" prop="totalCount">
                  <el-input-number
                    v-model="bookForm.totalCount"
                    :min="1"
                    :max="999"
                    style="width: 100%"
                  />
                  <span class="form-tip">系统将自动生成相应数量的条码</span>
                </el-form-item>

                <el-form-item label="存放位置" prop="location">
                  <el-input v-model="bookForm.location" placeholder="例如：A区3层5排" />
                </el-form-item>

                <el-form-item label="图书简介" prop="summary">
                  <el-input
                    v-model="bookForm.summary"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入图书简介或内容摘要"
                    maxlength="500"
                    show-word-limit
                  />
                </el-form-item>
              </div>
            </el-col>

            <!-- 右侧：封面和额外信息 -->
            <el-col :xs="24" :md="8">
              <div class="form-section">
                <h3 class="form-section-title">图书封面</h3>

                <el-form-item>
                  <AvatarUpload
                    v-model="bookForm.coverUrl"
                    :size="200"
                    shape="square"
                    :enable-camera="false"
                    :show-actions="true"
                    :show-tips="true"
                    placeholder-text="上传封面"
                    :tips="['建议尺寸 400x600 像素', '支持 JPG、PNG 格式', '文件大小不超过 2MB']"
                  />
                </el-form-item>

                <h3 class="form-section-title" style="margin-top: 24px">附加信息</h3>

                <el-form-item label="页数">
                  <el-input-number v-model="bookForm.pages" :min="1" style="width: 100%" />
                </el-form-item>

                <el-form-item label="语言">
                  <el-select
                    v-model="bookForm.language"
                    placeholder="请选择语言"
                    style="width: 100%"
                  >
                    <el-option label="中文" value="zh" />
                    <el-option label="英文" value="en" />
                    <el-option label="日文" value="ja" />
                    <el-option label="其他" value="other" />
                  </el-select>
                </el-form-item>

                <el-form-item label="装帧">
                  <el-select
                    v-model="bookForm.binding"
                    placeholder="请选择装帧方式"
                    style="width: 100%"
                  >
                    <el-option label="平装" value="paperback" />
                    <el-option label="精装" value="hardcover" />
                    <el-option label="其他" value="other" />
                  </el-select>
                </el-form-item>
              </div>
            </el-col>
          </el-row>

          <!-- 表单操作按钮 -->
          <el-form-item>
            <div class="form-actions">
              <el-button
                type="primary"
                size="large"
                :icon="Check"
                :loading="submitting"
                :disabled="submitting"
                @click="handleSubmit"
              >
                <ButtonLoading v-if="submitting" text="保存中..." size="large" />
                <span v-else>保存图书</span>
              </el-button>
              <el-button size="large" :disabled="submitting" @click="handleSaveAndContinue">
                保存并继续添加
              </el-button>
              <el-button size="large" :disabled="submitting" @click="handleReset">
                重置表单
              </el-button>
              <el-button size="large" :disabled="submitting" @click="$router.push('/books/list')">
                返回列表
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import AvatarUpload from '@/components/AvatarUpload.vue'
import { SkeletonLoader, ButtonLoading } from '@/components/Loading'
import { searchBookByISBN, createBook, getCategoryTree } from '@/api/books'

const router = useRouter()

// 编目模式
const catalogMode = ref('smart') // smart: 智能编目, manual: 手工编目

// ISBN查询
const isbnInput = ref('')
const isbnSearching = ref(false)
const isbnFetched = ref(false)

// 表单显示控制
const showForm = computed(() => {
  return catalogMode.value === 'manual' || isbnFetched.value
})

// 表单引用
const bookFormRef = ref(null)

// 分类数据加载
const categoryLoading = ref(false)
const categoryOptions = ref([])

// 提交状态
const submitting = ref(false)

// 图书表单数据
const bookForm = reactive({
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  publishDate: '',
  category: '',
  price: 0,
  totalCount: 1,
  location: '',
  summary: '',
  coverUrl: '',
  pages: null,
  language: 'zh',
  binding: 'paperback'
})

// 表单验证规则
const bookFormRules = {
  isbn: [
    { required: true, message: '请输入ISBN号', trigger: 'blur' },
    {
      pattern: /^(97[89])?\d{9}(\d|X)$/,
      message: 'ISBN格式不正确',
      trigger: 'blur'
    }
  ],
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  publisher: [{ required: true, message: '请输入出版社', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入定价', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入馆藏数量', trigger: 'blur' }]
}

// 切换编目模式
const handleModeChange = (mode) => {
  if (mode === 'manual') {
    showForm.value = true
    resetForm()
  } else {
    isbnFetched.value = false
    isbnInput.value = ''
  }
}

// 加载分类数据
const loadCategories = async () => {
  try {
    categoryLoading.value = true
    const res = await getCategoryTree()

    if (res.code === 200 && res.data) {
      // 将分类树转换为平铺选项列表
      const flattenCategories = (categories, level = 0) => {
        let options = []
        categories.forEach((cat) => {
          options.push({
            label: `${'　'.repeat(level)}${cat.name}`,
            value: cat.code
          })
          if (cat.children && cat.children.length > 0) {
            options = options.concat(flattenCategories(cat.children, level + 1))
          }
        })
        return options
      }
      categoryOptions.value = flattenCategories(res.data)
    }
  } catch (error) {
    console.error('加载分类失败:', error)
    // 使用默认分类
    categoryOptions.value = [
      { label: '文学', value: 'literature' },
      { label: '历史', value: 'history' },
      { label: '科学', value: 'science' },
      { label: '艺术', value: 'art' },
      { label: '计算机', value: 'computer' },
      { label: '哲学', value: 'philosophy' },
      { label: '经济', value: 'economics' },
      { label: '教育', value: 'education' },
      { label: '其他', value: 'other' }
    ]
  } finally {
    categoryLoading.value = false
  }
}

// ISBN查询
const handleISBNSearch = async () => {
  const isbn = catalogMode.value === 'smart' ? isbnInput.value : bookForm.isbn

  if (!isbn) {
    ElMessage.warning('请输入ISBN号')
    return
  }

  // 简单验证ISBN格式
  if (!/^(97[89])?\d{9}(\d|X)$/i.test(isbn)) {
    ElMessage.error('ISBN格式不正确，请检查后重试')
    return
  }

  isbnSearching.value = true

  try {
    const res = await searchBookByISBN(isbn)

    if (res.code === 200 && res.data) {
      // 填充表单
      Object.assign(bookForm, {
        isbn: res.data.isbn || isbn,
        title: res.data.title || '',
        author: res.data.author || '',
        publisher: res.data.publisher || '',
        publishDate: res.data.publishDate || '',
        category: res.data.category || '',
        price: res.data.price || 0,
        summary: res.data.summary || res.data.description || '',
        coverUrl: res.data.coverUrl || res.data.cover || '',
        pages: res.data.pages || null,
        language: res.data.language || 'zh',
        binding: res.data.binding || 'paperback'
      })
      isbnFetched.value = true

      ElMessage.success('图书信息获取成功，请核对并补充完整信息')
    } else {
      throw new Error('未获取到图书信息')
    }
  } catch (error) {
    console.error('ISBN查询失败:', error)

    // 如果查询失败，询问是否手动编目
    ElMessageBox.confirm('未查询到图书信息，是否切换到手工编目模式？', '提示', {
      confirmButtonText: '手工编目',
      cancelButtonText: '重新查询',
      type: 'warning'
    })
      .then(() => {
        catalogMode.value = 'manual'
        bookForm.isbn = isbn
        isbnFetched.value = false
      })
      .catch(() => {
        // 用户选择重新查询，清空ISBN输入
        isbnInput.value = ''
      })
  } finally {
    isbnSearching.value = false
  }
}

// 提交表单
const handleSubmit = async (continueAdding = false) => {
  if (!bookFormRef.value) return

  try {
    await bookFormRef.value.validate()

    submitting.value = true

    // 准备提交数据
    const submitData = {
      isbn: bookForm.isbn,
      title: bookForm.title,
      author: bookForm.author,
      publisher: bookForm.publisher,
      publishDate: bookForm.publishDate,
      category: bookForm.category,
      price: bookForm.price,
      totalCopies: bookForm.totalCount,
      availableCopies: bookForm.totalCount,
      location: bookForm.location,
      description: bookForm.summary,
      coverUrl: bookForm.coverUrl,
      pages: bookForm.pages,
      language: bookForm.language,
      binding: bookForm.binding,
      status: 'available'
    }

    const res = await createBook(submitData)

    if (res.code === 200) {
      ElMessage.success({
        message: `图书《${bookForm.title}》添加成功，已生成${bookForm.totalCount}个条码`,
        duration: 3000
      })

      if (continueAdding) {
        // 继续添加
        resetForm()
        isbnInput.value = ''
        isbnFetched.value = false
        bookFormRef.value?.resetFields()
      } else {
        // 返回列表
        setTimeout(() => {
          router.push('/books/list')
        }, 1500)
      }
    }
  } catch (error) {
    // 错误已由errorHandler处理
    console.error('保存图书失败:', error)
  } finally {
    submitting.value = false
  }
}

// 保存并继续添加
const handleSaveAndContinue = () => {
  handleSubmit(true)
}

// 重置表单
const handleReset = () => {
  bookFormRef.value?.resetFields()
  resetForm()
  isbnInput.value = ''
  isbnFetched.value = false
}

// 重置表单数据
const resetForm = () => {
  bookForm.isbn = ''
  bookForm.title = ''
  bookForm.author = ''
  bookForm.publisher = ''
  bookForm.publishDate = ''
  bookForm.category = ''
  bookForm.price = 0
  bookForm.totalCount = 1
  bookForm.location = ''
  bookForm.summary = ''
  bookForm.coverUrl = ''
  bookForm.pages = null
  bookForm.language = 'zh'
  bookForm.binding = 'paperback'
}

// 组件挂载时加载分类
onMounted(() => {
  loadCategories()
})
</script>

<style lang="scss" scoped>
.mode-description {
  margin-top: 16px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 4px;
  color: #1890ff;
  font-size: 14px;

  .el-icon {
    margin-right: 8px;
  }
}

.isbn-search {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.isbn-tip {
  margin-top: 12px;
  padding: 8px 12px;
  background: #fffbe6;
  border-radius: 4px;
  color: #faad14;
  font-size: 13px;

  .el-icon {
    margin-right: 8px;
  }
}

.form-section {
  &-title {
    font-size: 16px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 16px;
    padding-bottom: 8px;
    border-bottom: 2px solid #f0f0f0;
  }
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

:deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
