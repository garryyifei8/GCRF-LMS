/**
 * Card 组件类型定义
 */

export type CardVariant = 'default' | 'bordered' | 'elevated' | 'flat'
export type CardPadding = 'none' | 'sm' | 'md' | 'lg'

export interface CardProps {
  /** 卡片变体 */
  variant?: CardVariant
  /** 内边距大小 */
  padding?: CardPadding
  /** 是否显示阴影 */
  shadow?: boolean
  /** 是否可悬停 */
  hoverable?: boolean
  /** 是否可点击 */
  clickable?: boolean
}

export interface CardEmits {
  (e: 'click', event: MouseEvent): void
}

export interface StatCardProps {
  /** 标题 */
  title: string
  /** 数值 */
  value: string | number
  /** 图标 (Element Plus 图标名称) */
  icon?: string
  /** 趋势 (up | down | flat) */
  trend?: 'up' | 'down' | 'flat'
  /** 趋势值 */
  trendValue?: string
  /** 趋势描述 */
  trendText?: string
  /** 主题色 */
  color?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

export interface AICardProps {
  /** 标题 */
  title: string
  /** 描述 */
  description?: string
  /** 图片URL */
  image?: string
  /** 标签列表 */
  tags?: string[]
  /** AI推荐分数 (0-100) */
  score?: number
  /** 是否显示AI徽章 */
  showAIBadge?: boolean
}

export interface AICardEmits {
  (e: 'click', event: MouseEvent): void
  (e: 'action', action: string): void
}
