/**
 * Button 组件类型定义
 */

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'text' | 'ai'
export type ButtonSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl'
export type IconPosition = 'left' | 'right'

export interface ButtonProps {
  /** 按钮变体 */
  variant?: ButtonVariant
  /** 按钮尺寸 */
  size?: ButtonSize
  /** 加载状态 */
  loading?: boolean
  /** 禁用状态 */
  disabled?: boolean
  /** 全宽显示 */
  fullWidth?: boolean
  /** 图标 (Element Plus 图标名称) */
  icon?: string
  /** 图标位置 */
  iconPosition?: IconPosition
  /** HTML 按钮类型 */
  type?: 'button' | 'submit' | 'reset'
}

export interface ButtonEmits {
  (e: 'click', event: MouseEvent): void
}
