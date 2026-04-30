<template>
  <el-dialog v-model="visible" title="按年级批量注销" width="500px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="年级">
        <el-select v-model="form.grade" placeholder="请选择年级" style="width: 200px">
          <el-option v-for="g in grades" :key="g" :label="g + '级'" :value="g" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.grade">
        <el-alert type="warning" :closable="false">
          <template #default>
            此操作将把 <strong>{{ form.grade }}级</strong> 学生读者状态批量改为「已注销」。<br />
            操作不可逆，请谨慎操作。
          </template>
        </el-alert>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="loading" :disabled="!form.grade" @click="confirm">
        确认注销
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchCancelByGrade } from '@/api/readers'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(props.modelValue)
const loading = ref(false)
const form = reactive({ grade: '' })

// Generate grades 2018-2026
const grades = Array.from({ length: 9 }, (_, i) => String(2018 + i))

watch(
  () => props.modelValue,
  (v) => {
    visible.value = v
    if (v) form.grade = ''
  }
)
watch(visible, (v) => emit('update:modelValue', v))

const confirm = async () => {
  try {
    await ElMessageBox.confirm(`确定将所有 ${form.grade}级 学生注销？此操作不可逆`, '二次确认', {
      type: 'warning',
      confirmButtonText: '确定注销',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await batchCancelByGrade({ grade: form.grade })
    if (res.code === 200) {
      ElMessage.success(`已注销 ${res.data?.cancelledCount ?? 0} 个读者`)
      visible.value = false
      emit('success')
    } else {
      ElMessage.error(res.message || '注销失败')
    }
  } catch (e) {
    ElMessage.error('注销失败')
  } finally {
    loading.value = false
  }
}
</script>
