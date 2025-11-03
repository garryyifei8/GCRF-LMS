/**
 * Input 组件类型定义
 */

export type InputType = 'text' | 'password' | 'email' | 'number' | 'tel' | 'url' | 'search'
export type InputSize = 'sm' | 'md' | 'lg'
export type InputStatus = 'default' | 'success' | 'error' | 'warning'

export interface InputProps {
  /** 输入框类型 */
  type?: InputType
  /** 输入框尺寸 */
  size?: InputSize
  /** 输入框值 (v-model) */
  modelValue?: string | number
  /** 占位符 */
  placeholder?: string
  /** 是否禁用 */
  disabled?: boolean
  /** 是否只读 */
  readonly?: boolean
  /** 是否必填 */
  required?: boolean
  /** 最大长度 */
  maxlength?: number
  /** 是否显示字数统计 */
  showCount?: boolean
  /** 是否可清空 */
  clearable?: boolean
  /** 前缀图标 (Element Plus 图标名称) */
  prefixIcon?: string
  /** 后缀图标 (Element Plus 图标名称) */
  suffixIcon?: string
  /** 状态 */
  status?: InputStatus
  /** 帮助文字 */
  helpText?: string
  /** 错误信息 */
  errorText?: string
  /** 标签 */
  label?: string
  /** 是否显示标签 */
  showLabel?: boolean
}

export interface InputEmits {
  (e: 'update:modelValue', value: string | number): void
  (e: 'input', value: string | number): void
  (e: 'change', value: string | number): void
  (e: 'blur', event: FocusEvent): void
  (e: 'focus', event: FocusEvent): void
  (e: 'clear'): void
}
