# 国创睿峰智能图书馆管理系统 - 前端设计规范 V2.0
## 基于 Modernize Tailwind 设计系统

> 更新日期: 2025-01-14
> 设计理念: 现代化、专业、智能、响应式
> 技术栈: Vue 3 + Tailwind CSS + Element Plus

---

## 📋 目录

1. [设计哲学](#设计哲学)
2. [色彩系统](#色彩系统)
3. [排版系统](#排版系统)
4. [间距与布局](#间距与布局)
5. [组件库规范](#组件库规范)
6. [深色模式](#深色模式)
7. [响应式设计](#响应式设计)
8. [AI功能视觉语言](#ai功能视觉语言)

---

## 设计哲学

### 核心原则

#### 1. **简洁专业 (Clean & Professional)**
- 干净的视觉层次
- 充足的留白空间
- 清晰的信息架构
- 避免过度装饰

#### 2. **智能现代 (Smart & Modern)**
- AI功能视觉差异化
- 流畅的交互动画
- 渐变与光效点缀
- 前沿的视觉风格

#### 3. **灵活适配 (Flexible & Adaptive)**
- 响应式布局系统
- 深色/浅色模式切换
- 多设备完美呈现
- 可定制的主题系统

#### 4. **教育友好 (Education-Friendly)**
- 适合中小学场景
- 清晰易读的字体
- 活力而不失专业
- 考虑可访问性

---

## 色彩系统

### 主色板 (Primary Colors)

#### 品牌主色 - Indigo
```css
/* Tailwind: indigo-600 */
--color-primary: #4F46E5;
--color-primary-hover: #4338CA;
--color-primary-light: #EEF2FF;
--color-primary-dark: #3730A3;

/* 渐变 */
--gradient-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

**使用场景:**
- 主要按钮
- 重要链接
- 导航激活态
- 关键信息标记

#### 辅助主色 - Purple
```css
/* Tailwind: purple-600 */
--color-secondary: #9333EA;
--color-secondary-hover: #7E22CE;
--color-secondary-light: #FAF5FF;
--color-secondary-dark: #6B21A8;
```

**使用场景:**
- AI功能标识
- 智能推荐模块
- 特殊功能入口
- 渐变搭配色

### 语义色板 (Semantic Colors)

#### 成功 Success
```css
/* Tailwind: green-600 */
--color-success: #16A34A;
--color-success-hover: #15803D;
--color-success-light: #F0FDF4;
--color-success-dark: #166534;
```

#### 警告 Warning
```css
/* Tailwind: amber-500 */
--color-warning: #F59E0B;
--color-warning-hover: #D97706;
--color-warning-light: #FFFBEB;
--color-warning-dark: #B45309;
```

#### 错误 Error
```css
/* Tailwind: red-600 */
--color-error: #DC2626;
--color-error-hover: #B91C1C;
--color-error-light: #FEF2F2;
--color-error-dark: #991B1B;
```

#### 信息 Info
```css
/* Tailwind: blue-600 */
--color-info: #2563EB;
--color-info-hover: #1D4ED8;
--color-info-light: #EFF6FF;
--color-info-dark: #1E40AF;
```

### 中性色板 (Neutral Colors)

```css
/* Gray Scale - Tailwind: slate */
--color-gray-50: #F8FAFC;
--color-gray-100: #F1F5F9;
--color-gray-200: #E2E8F0;
--color-gray-300: #CBD5E1;
--color-gray-400: #94A3B8;
--color-gray-500: #64748B;
--color-gray-600: #475569;
--color-gray-700: #334155;
--color-gray-800: #1E293B;
--color-gray-900: #0F172A;

/* 文本颜色 */
--text-primary: #0F172A;    /* gray-900 */
--text-secondary: #475569;  /* gray-600 */
--text-tertiary: #94A3B8;   /* gray-400 */
--text-disabled: #CBD5E1;   /* gray-300 */

/* 边框颜色 */
--border-default: #E2E8F0;  /* gray-200 */
--border-light: #F1F5F9;    /* gray-100 */
--border-dark: #CBD5E1;     /* gray-300 */

/* 背景颜色 */
--bg-primary: #FFFFFF;
--bg-secondary: #F8FAFC;    /* gray-50 */
--bg-tertiary: #F1F5F9;     /* gray-100 */
```

### AI功能专用色

```css
/* AI 渐变色 */
--ai-gradient-1: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
--ai-gradient-2: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
--ai-gradient-3: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);

/* AI 光效 */
--ai-glow: 0 0 20px rgba(102, 126, 234, 0.5);
--ai-shimmer: linear-gradient(90deg,
  transparent 0%,
  rgba(255,255,255,0.3) 50%,
  transparent 100%);
```

### 色彩使用规则

1. **主色使用**: 不超过页面20%面积
2. **对比度要求**: 文字与背景对比度 ≥ 4.5:1
3. **渐变使用**: 仅用于AI功能、卡片装饰、按钮特效
4. **语义色一致性**: 成功用绿色、错误用红色、警告用橙色

---

## 排版系统

### 字体家族

#### 中文字体
```css
--font-sans-cn: -apple-system, BlinkMacSystemFont,
                "Segoe UI", "PingFang SC",
                "Hiragino Sans GB", "Microsoft YaHei",
                "Helvetica Neue", Helvetica, Arial,
                sans-serif;
```

#### 英文/数字字体
```css
--font-sans-en: "Inter", -apple-system, BlinkMacSystemFont,
                "Segoe UI", sans-serif;

--font-mono: "JetBrains Mono", "Fira Code",
             "Consolas", "Monaco", monospace;
```

### 字体大小

使用 Tailwind 默认尺寸体系：

```css
/* 标题 */
--text-5xl: 3rem;      /* 48px - 主标题 */
--text-4xl: 2.25rem;   /* 36px - 页面标题 */
--text-3xl: 1.875rem;  /* 30px - 区块标题 */
--text-2xl: 1.5rem;    /* 24px - 卡片标题 */
--text-xl: 1.25rem;    /* 20px - 小标题 */

/* 正文 */
--text-lg: 1.125rem;   /* 18px - 大正文 */
--text-base: 1rem;     /* 16px - 标准正文 */
--text-sm: 0.875rem;   /* 14px - 小正文 */
--text-xs: 0.75rem;    /* 12px - 辅助文字 */
```

### 字重

```css
--font-thin: 100;
--font-extralight: 200;
--font-light: 300;
--font-normal: 400;     /* 正文 */
--font-medium: 500;     /* 强调 */
--font-semibold: 600;   /* 小标题 */
--font-bold: 700;       /* 标题 */
--font-extrabold: 800;
--font-black: 900;
```

### 行高

```css
--leading-none: 1;
--leading-tight: 1.25;
--leading-snug: 1.375;
--leading-normal: 1.5;   /* 正文推荐 */
--leading-relaxed: 1.625;
--leading-loose: 2;
```

### 排版示例

```html
<!-- 页面主标题 -->
<h1 class="text-4xl font-bold text-gray-900 dark:text-white">
  工作台
</h1>

<!-- 区块标题 -->
<h2 class="text-2xl font-semibold text-gray-800 dark:text-gray-100">
  今日借阅统计
</h2>

<!-- 卡片标题 -->
<h3 class="text-xl font-semibold text-gray-800 dark:text-gray-100">
  热门图书
</h3>

<!-- 正文内容 -->
<p class="text-base text-gray-600 dark:text-gray-300 leading-relaxed">
  这是正文内容，使用标准字号和行高。
</p>

<!-- 辅助说明 -->
<span class="text-sm text-gray-500 dark:text-gray-400">
  辅助说明文字
</span>
```

---

## 间距与布局

### 间距系统

使用 Tailwind 默认间距刻度（基于 0.25rem / 4px）：

```css
/* 常用间距 */
--spacing-0: 0px;
--spacing-1: 0.25rem;   /* 4px */
--spacing-2: 0.5rem;    /* 8px */
--spacing-3: 0.75rem;   /* 12px */
--spacing-4: 1rem;      /* 16px - 最常用 */
--spacing-5: 1.25rem;   /* 20px */
--spacing-6: 1.5rem;    /* 24px */
--spacing-8: 2rem;      /* 32px */
--spacing-10: 2.5rem;   /* 40px */
--spacing-12: 3rem;     /* 48px */
--spacing-16: 4rem;     /* 64px */
--spacing-20: 5rem;     /* 80px */
```

### 间距使用规则

#### 组件内边距
```html
<!-- 卡片内边距 -->
<div class="p-6">          <!-- 24px -->
<div class="p-4">          <!-- 16px - 小卡片 -->
<div class="px-6 py-4">    <!-- 水平24px 垂直16px -->

<!-- 按钮内边距 -->
<button class="px-4 py-2">  <!-- 标准按钮 -->
<button class="px-6 py-3">  <!-- 大按钮 -->
<button class="px-3 py-1.5"><!-- 小按钮 -->
```

#### 组件间距
```html
<!-- 垂直间距 -->
<div class="space-y-4">    <!-- 子元素间隔16px -->
<div class="space-y-6">    <!-- 子元素间隔24px -->
<div class="mb-6">         <!-- 下边距24px -->

<!-- 水平间距 -->
<div class="space-x-3">    <!-- 子元素间隔12px -->
<div class="gap-4">        <!-- Grid/Flex间隔16px -->
```

### 容器宽度

```html
<!-- 最大宽度容器 -->
<div class="container mx-auto max-w-7xl px-4">
  <!-- 内容 -->
</div>

<!-- 固定宽度断点 -->
<div class="max-w-sm">    <!-- 384px -->
<div class="max-w-md">    <!-- 448px -->
<div class="max-w-lg">    <!-- 512px -->
<div class="max-w-xl">    <!-- 576px -->
<div class="max-w-2xl">   <!-- 672px -->
<div class="max-w-4xl">   <!-- 896px -->
<div class="max-w-6xl">   <!-- 1152px -->
<div class="max-w-7xl">   <!-- 1280px - 推荐主容器 -->
```

### 网格系统

```html
<!-- 12列网格 -->
<div class="grid grid-cols-12 gap-6">
  <div class="col-span-12 lg:col-span-8">主内容</div>
  <div class="col-span-12 lg:col-span-4">侧边栏</div>
</div>

<!-- 响应式卡片网格 -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
  <!-- 卡片 -->
</div>

<!-- 统计卡片网格 -->
<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
  <!-- 统计卡片 -->
</div>
```

### 圆角系统

```css
--rounded-none: 0px;
--rounded-sm: 0.125rem;    /* 2px */
--rounded: 0.25rem;        /* 4px - 小元素 */
--rounded-md: 0.375rem;    /* 6px */
--rounded-lg: 0.5rem;      /* 8px - 卡片推荐 */
--rounded-xl: 0.75rem;     /* 12px - 大卡片 */
--rounded-2xl: 1rem;       /* 16px - 弹窗 */
--rounded-3xl: 1.5rem;     /* 24px */
--rounded-full: 9999px;    /* 圆形 */
```

### 阴影系统

```css
/* Tailwind 阴影 */
--shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
--shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
--shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
--shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
--shadow-xl: 0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1);
--shadow-2xl: 0 25px 50px -12px rgb(0 0 0 / 0.25);

/* 使用示例 */
.card { @apply shadow-md; }
.card:hover { @apply shadow-lg; }
.modal { @apply shadow-2xl; }
```

---

## 组件库规范

### 1. 按钮 (Button)

#### 主要按钮 (Primary)
```html
<!-- 标准主按钮 -->
<button class="
  px-4 py-2
  bg-indigo-600 hover:bg-indigo-700
  text-white font-medium
  rounded-lg
  transition-colors duration-200
  focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2
">
  确认
</button>

<!-- 大按钮 -->
<button class="px-6 py-3 text-lg ...">确认</button>

<!-- 小按钮 -->
<button class="px-3 py-1.5 text-sm ...">确认</button>
```

#### 次要按钮 (Secondary)
```html
<button class="
  px-4 py-2
  bg-white hover:bg-gray-50
  text-gray-700 font-medium
  border border-gray-300
  rounded-lg
  transition-colors duration-200
">
  取消
</button>
```

#### 危险按钮 (Danger)
```html
<button class="
  px-4 py-2
  bg-red-600 hover:bg-red-700
  text-white font-medium
  rounded-lg
  transition-colors duration-200
">
  删除
</button>
```

#### 文字按钮 (Text)
```html
<button class="
  px-2 py-1
  text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50
  font-medium
  rounded
  transition-colors duration-200
">
  了解更多
</button>
```

#### AI特殊按钮
```html
<button class="
  px-6 py-3
  bg-gradient-to-r from-indigo-600 to-purple-600
  hover:from-indigo-700 hover:to-purple-700
  text-white font-semibold
  rounded-lg
  shadow-lg hover:shadow-xl
  transition-all duration-300
  flex items-center gap-2
">
  <svg class="w-5 h-5">...</svg>
  AI 智能推荐
</button>
```

### 2. 卡片 (Card)

#### 基础卡片
```html
<div class="
  bg-white dark:bg-gray-800
  rounded-lg
  shadow-md hover:shadow-lg
  p-6
  transition-shadow duration-300
">
  <!-- 卡片标题 -->
  <h3 class="text-xl font-semibold text-gray-900 dark:text-white mb-4">
    卡片标题
  </h3>

  <!-- 卡片内容 -->
  <p class="text-gray-600 dark:text-gray-300">
    卡片内容...
  </p>

  <!-- 卡片操作 -->
  <div class="mt-4 flex justify-end gap-2">
    <button>...</button>
  </div>
</div>
```

#### 统计卡片
```html
<div class="
  bg-white dark:bg-gray-800
  rounded-lg
  shadow-md
  p-6
  border-l-4 border-indigo-600
">
  <div class="flex items-center justify-between">
    <!-- 图标 -->
    <div class="
      p-3
      bg-indigo-100 dark:bg-indigo-900
      rounded-full
    ">
      <svg class="w-8 h-8 text-indigo-600 dark:text-indigo-400">...</svg>
    </div>

    <!-- 趋势 -->
    <span class="text-sm text-green-600 flex items-center gap-1">
      <svg class="w-4 h-4">↑</svg>
      +12%
    </span>
  </div>

  <!-- 数据 -->
  <div class="mt-4">
    <p class="text-sm text-gray-500 dark:text-gray-400">图书总量</p>
    <p class="text-3xl font-bold text-gray-900 dark:text-white">12,456</p>
  </div>
</div>
```

#### AI推荐卡片
```html
<div class="
  bg-gradient-to-br from-indigo-50 to-purple-50
  dark:from-indigo-900/20 dark:to-purple-900/20
  rounded-xl
  p-6
  border border-indigo-200 dark:border-indigo-800
  relative overflow-hidden
">
  <!-- AI标识 -->
  <div class="absolute top-4 right-4">
    <span class="
      px-3 py-1
      bg-gradient-to-r from-indigo-600 to-purple-600
      text-white text-xs font-semibold
      rounded-full
      flex items-center gap-1
    ">
      <svg class="w-3 h-3">✨</svg>
      AI推荐
    </span>
  </div>

  <!-- 内容 -->
  <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-2">
    为您推荐
  </h3>
  <p class="text-gray-600 dark:text-gray-300 text-sm mb-4">
    基于您的阅读偏好和行为分析
  </p>

  <!-- 推荐列表 -->
  <div class="space-y-3">
    <!-- 推荐项 -->
  </div>
</div>
```

### 3. 表格 (Table)

#### 基础表格
```html
<div class="overflow-x-auto">
  <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
    <!-- 表头 -->
    <thead class="bg-gray-50 dark:bg-gray-800">
      <tr>
        <th class="
          px-6 py-3
          text-left text-xs font-medium
          text-gray-500 dark:text-gray-400
          uppercase tracking-wider
        ">
          姓名
        </th>
        <th>学号</th>
        <th>状态</th>
        <th>操作</th>
      </tr>
    </thead>

    <!-- 表体 -->
    <tbody class="
      bg-white dark:bg-gray-900
      divide-y divide-gray-200 dark:divide-gray-700
    ">
      <tr class="hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
        <td class="px-6 py-4 whitespace-nowrap">
          <div class="flex items-center">
            <img class="h-10 w-10 rounded-full" src="..." alt="">
            <div class="ml-4">
              <div class="text-sm font-medium text-gray-900 dark:text-white">
                张三
              </div>
            </div>
          </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
          2024001
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
          <span class="
            px-2 py-1
            inline-flex text-xs leading-5 font-semibold
            rounded-full
            bg-green-100 text-green-800
            dark:bg-green-900/30 dark:text-green-400
          ">
            正常
          </span>
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
          <button class="text-indigo-600 hover:text-indigo-900 dark:text-indigo-400">
            编辑
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</div>
```

### 4. 表单 (Form)

#### 输入框
```html
<!-- 文本输入 -->
<div class="mb-4">
  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
    姓名
  </label>
  <input
    type="text"
    class="
      w-full
      px-4 py-2
      border border-gray-300 dark:border-gray-600
      rounded-lg
      bg-white dark:bg-gray-800
      text-gray-900 dark:text-white
      focus:ring-2 focus:ring-indigo-500 focus:border-transparent
      transition-colors
    "
    placeholder="请输入姓名"
  >
</div>

<!-- 错误状态 -->
<input class="
  ...
  border-red-300 dark:border-red-600
  focus:ring-red-500
">
<p class="mt-1 text-sm text-red-600 dark:text-red-400">
  此字段为必填项
</p>
```

#### 选择框
```html
<select class="
  w-full
  px-4 py-2
  border border-gray-300 dark:border-gray-600
  rounded-lg
  bg-white dark:bg-gray-800
  text-gray-900 dark:text-white
  focus:ring-2 focus:ring-indigo-500
  transition-colors
">
  <option>请选择</option>
  <option>选项1</option>
  <option>选项2</option>
</select>
```

#### 开关
```html
<button
  type="button"
  class="
    relative inline-flex h-6 w-11
    flex-shrink-0 cursor-pointer
    rounded-full border-2 border-transparent
    bg-gray-200 dark:bg-gray-700
    transition-colors duration-200 ease-in-out
    focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2
  "
  role="switch"
  aria-checked="false"
>
  <span class="
    pointer-events-none
    inline-block h-5 w-5
    transform rounded-full
    bg-white shadow
    ring-0 transition duration-200 ease-in-out
    translate-x-0
  "></span>
</button>

<!-- 激活状态: bg-indigo-600, translate-x-5 -->
```

### 5. 对话框 (Modal)

```html
<!-- 遮罩层 -->
<div class="
  fixed inset-0
  bg-gray-500 bg-opacity-75 dark:bg-gray-900 dark:bg-opacity-75
  transition-opacity
  z-40
"></div>

<!-- 对话框 -->
<div class="
  fixed inset-0 z-50
  overflow-y-auto
">
  <div class="
    flex min-h-full items-end justify-center
    p-4 text-center
    sm:items-center sm:p-0
  ">
    <div class="
      relative
      transform overflow-hidden
      rounded-lg
      bg-white dark:bg-gray-800
      px-4 pb-4 pt-5
      text-left
      shadow-xl
      transition-all
      sm:my-8 sm:w-full sm:max-w-lg sm:p-6
    ">
      <!-- 标题 -->
      <div class="mb-4">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
          对话框标题
        </h3>
      </div>

      <!-- 内容 -->
      <div class="mb-6">
        <p class="text-sm text-gray-500 dark:text-gray-400">
          对话框内容...
        </p>
      </div>

      <!-- 底部按钮 -->
      <div class="flex justify-end gap-3">
        <button class="px-4 py-2 ...">取消</button>
        <button class="px-4 py-2 ...">确认</button>
      </div>
    </div>
  </div>
</div>
```

### 6. 标签 (Badge/Tag)

```html
<!-- 状态标签 -->
<span class="
  inline-flex items-center
  px-2.5 py-0.5
  rounded-full
  text-xs font-medium
  bg-green-100 text-green-800
  dark:bg-green-900/30 dark:text-green-400
">
  正常
</span>

<span class="... bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400">
  逾期
</span>

<span class="... bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
  待审核
</span>

<!-- 数字徽章 -->
<span class="
  inline-flex items-center justify-center
  w-6 h-6
  text-xs font-bold
  text-white
  bg-red-600
  rounded-full
">
  3
</span>

<!-- AI标签 -->
<span class="
  inline-flex items-center gap-1
  px-3 py-1
  rounded-full
  text-xs font-semibold
  bg-gradient-to-r from-indigo-600 to-purple-600
  text-white
">
  <svg class="w-3 h-3">✨</svg>
  AI生成
</span>
```

### 7. 导航 (Navigation)

#### 顶部导航
```html
<nav class="
  bg-white dark:bg-gray-800
  border-b border-gray-200 dark:border-gray-700
  fixed top-0 left-0 right-0 z-30
">
  <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
    <div class="flex h-16 justify-between items-center">
      <!-- Logo -->
      <div class="flex items-center gap-2">
        <img class="h-8 w-auto" src="/logo.svg" alt="Logo">
        <span class="text-xl font-bold text-gray-900 dark:text-white">
          智慧图书馆
        </span>
      </div>

      <!-- 搜索 -->
      <div class="flex-1 max-w-2xl mx-8">
        <input
          type="search"
          class="w-full px-4 py-2 rounded-lg ..."
          placeholder="搜索图书、读者..."
        >
      </div>

      <!-- 右侧操作 -->
      <div class="flex items-center gap-4">
        <!-- 通知 -->
        <button class="relative p-2 text-gray-400 hover:text-gray-500 dark:text-gray-300">
          <svg class="w-6 h-6">🔔</svg>
          <span class="absolute top-1 right-1 w-2 h-2 bg-red-600 rounded-full"></span>
        </button>

        <!-- AI助手 -->
        <button class="p-2 text-indigo-600 hover:text-indigo-700">
          <svg class="w-6 h-6">🤖</svg>
        </button>

        <!-- 用户菜单 -->
        <div class="flex items-center gap-2">
          <img class="h-8 w-8 rounded-full" src="..." alt="">
          <span class="text-sm font-medium text-gray-700 dark:text-gray-300">张老师</span>
        </div>
      </div>
    </div>
  </div>
</nav>
```

#### 侧边栏导航
```html
<aside class="
  w-64
  bg-white dark:bg-gray-800
  border-r border-gray-200 dark:border-gray-700
  fixed left-0 top-16 bottom-0
  overflow-y-auto
">
  <nav class="p-4 space-y-1">
    <!-- 导航项 - 激活态 -->
    <a href="#" class="
      flex items-center gap-3
      px-4 py-3
      text-sm font-medium
      text-white
      bg-indigo-600
      rounded-lg
    ">
      <svg class="w-5 h-5">📊</svg>
      工作台
    </a>

    <!-- 导航项 - 默认态 -->
    <a href="#" class="
      flex items-center gap-3
      px-4 py-3
      text-sm font-medium
      text-gray-700 dark:text-gray-300
      hover:bg-gray-100 dark:hover:bg-gray-700
      rounded-lg
      transition-colors
    ">
      <svg class="w-5 h-5">📚</svg>
      图书管理
    </a>

    <!-- 可折叠分组 -->
    <div class="space-y-1">
      <button class="
        w-full
        flex items-center justify-between
        px-4 py-3
        text-sm font-medium
        text-gray-700 dark:text-gray-300
        hover:bg-gray-100 dark:hover:bg-gray-700
        rounded-lg
      ">
        <span class="flex items-center gap-3">
          <svg class="w-5 h-5">👥</svg>
          读者管理
        </span>
        <svg class="w-4 h-4 transition-transform">▼</svg>
      </button>

      <!-- 子菜单 -->
      <div class="ml-8 space-y-1">
        <a href="#" class="
          block
          px-4 py-2
          text-sm
          text-gray-600 dark:text-gray-400
          hover:text-gray-900 dark:hover:text-white
          rounded-lg
        ">
          学生读者
        </a>
        <a href="#" class="...">教师读者</a>
      </div>
    </div>

    <!-- AI功能区 - 特殊样式 -->
    <div class="
      mt-6 pt-6
      border-t border-gray-200 dark:border-gray-700
    ">
      <a href="#" class="
        flex items-center gap-3
        px-4 py-3
        text-sm font-medium
        text-indigo-600 dark:text-indigo-400
        bg-indigo-50 dark:bg-indigo-900/20
        rounded-lg
        border border-indigo-200 dark:border-indigo-800
      ">
        <svg class="w-5 h-5">✨</svg>
        AI 智能推荐
      </a>
    </div>
  </nav>
</aside>
```

### 8. 分页 (Pagination)

```html
<nav class="flex items-center justify-between border-t border-gray-200 dark:border-gray-700 px-4 py-3">
  <!-- 左侧信息 -->
  <div class="hidden sm:block">
    <p class="text-sm text-gray-700 dark:text-gray-300">
      显示 <span class="font-medium">1</span> 到 <span class="font-medium">10</span> 条，
      共 <span class="font-medium">97</span> 条结果
    </p>
  </div>

  <!-- 右侧分页 -->
  <div class="flex-1 flex justify-between sm:justify-end">
    <button class="
      relative inline-flex items-center
      px-4 py-2
      text-sm font-medium
      text-gray-700 dark:text-gray-300
      bg-white dark:bg-gray-800
      border border-gray-300 dark:border-gray-600
      rounded-lg
      hover:bg-gray-50 dark:hover:bg-gray-700
      disabled:opacity-50 disabled:cursor-not-allowed
    ">
      上一页
    </button>

    <div class="hidden md:flex gap-2 mx-4">
      <button class="
        px-4 py-2
        text-sm font-medium
        text-white
        bg-indigo-600
        rounded-lg
      ">
        1
      </button>
      <button class="
        px-4 py-2
        text-sm font-medium
        text-gray-700 dark:text-gray-300
        bg-white dark:bg-gray-800
        border border-gray-300 dark:border-gray-600
        rounded-lg
        hover:bg-gray-50 dark:hover:bg-gray-700
      ">
        2
      </button>
      <button>3</button>
      <span class="px-4 py-2 text-gray-500">...</span>
      <button>10</button>
    </div>

    <button class="...">
      下一页
    </button>
  </div>
</nav>
```

### 9. 通知/警告 (Alert)

```html
<!-- 成功提示 -->
<div class="
  flex items-start gap-3
  p-4
  rounded-lg
  bg-green-50 dark:bg-green-900/20
  border border-green-200 dark:border-green-800
">
  <svg class="w-5 h-5 text-green-600 dark:text-green-400 flex-shrink-0 mt-0.5">✓</svg>
  <div class="flex-1">
    <h4 class="text-sm font-medium text-green-800 dark:text-green-400">
      操作成功
    </h4>
    <p class="mt-1 text-sm text-green-700 dark:text-green-300">
      数据已成功保存
    </p>
  </div>
  <button class="text-green-600 dark:text-green-400 hover:text-green-700">
    <svg class="w-5 h-5">×</svg>
  </button>
</div>

<!-- 错误提示 -->
<div class="... bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800">
  <svg class="... text-red-600 dark:text-red-400">⚠</svg>
  <div class="...">
    <h4 class="... text-red-800 dark:text-red-400">错误</h4>
    <p class="... text-red-700 dark:text-red-300">操作失败，请重试</p>
  </div>
</div>

<!-- 警告提示 -->
<div class="... bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-800">
  ...
</div>

<!-- 信息提示 -->
<div class="... bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
  ...
</div>
```

### 10. 加载状态 (Loading)

```html
<!-- 旋转加载器 -->
<div class="flex items-center justify-center p-8">
  <svg class="animate-spin h-8 w-8 text-indigo-600" fill="none" viewBox="0 0 24 24">
    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
  </svg>
</div>

<!-- 骨架屏 -->
<div class="animate-pulse space-y-4">
  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
  <div class="space-y-3">
    <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded"></div>
    <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-5/6"></div>
  </div>
</div>

<!-- 进度条 -->
<div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2.5">
  <div class="
    bg-indigo-600 h-2.5 rounded-full
    transition-all duration-300
  " style="width: 45%"></div>
</div>
```

---

## 深色模式

### 实现方式

#### 1. Tailwind 配置
```javascript
// tailwind.config.js
module.exports = {
  darkMode: 'class', // 使用 class 策略
  // ...
}
```

#### 2. 主题切换
```javascript
// 切换主题
function toggleTheme() {
  if (document.documentElement.classList.contains('dark')) {
    document.documentElement.classList.remove('dark')
    localStorage.theme = 'light'
  } else {
    document.documentElement.classList.add('dark')
    localStorage.theme = 'dark'
  }
}

// 初始化主题
if (localStorage.theme === 'dark' ||
    (!('theme' in localStorage) &&
     window.matchMedia('(prefers-color-scheme: dark)').matches)) {
  document.documentElement.classList.add('dark')
}
```

### 深色模式配色

```css
/* 背景色 */
.bg-white → .dark:bg-gray-900
.bg-gray-50 → .dark:bg-gray-800
.bg-gray-100 → .dark:bg-gray-700

/* 文字色 */
.text-gray-900 → .dark:text-white
.text-gray-700 → .dark:text-gray-200
.text-gray-600 → .dark:text-gray-300
.text-gray-500 → .dark:text-gray-400

/* 边框色 */
.border-gray-200 → .dark:border-gray-700
.border-gray-300 → .dark:border-gray-600

/* 组件适配 */
.hover:bg-gray-50 → .dark:hover:bg-gray-800
.focus:ring-indigo-500 → .dark:focus:ring-indigo-400
```

### 深色模式最佳实践

1. **始终同时定义浅色和深色样式**
2. **避免使用绝对黑色(#000)和绝对白色(#FFF)**
3. **保持足够的对比度**
4. **测试所有交互状态**
5. **图片使用时考虑深色背景**

---

## 响应式设计

### 断点系统

```css
/* Tailwind 默认断点 */
sm: 640px   /* 手机横屏 */
md: 768px   /* 平板 */
lg: 1024px  /* 小型桌面 */
xl: 1280px  /* 桌面 */
2xl: 1536px /* 大屏 */
```

### 响应式使用示例

```html
<!-- 响应式文字大小 -->
<h1 class="text-2xl md:text-4xl lg:text-5xl">标题</h1>

<!-- 响应式间距 -->
<div class="p-4 md:p-6 lg:p-8">内容</div>

<!-- 响应式网格 -->
<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
  <!-- 卡片 -->
</div>

<!-- 响应式显示/隐藏 -->
<div class="hidden md:block">桌面显示</div>
<div class="block md:hidden">移动显示</div>

<!-- 响应式布局切换 -->
<div class="flex flex-col md:flex-row gap-4">
  <!-- 移动端垂直，桌面端水平 -->
</div>
```

### 移动端优化

#### 1. 触摸友好
```html
<!-- 最小触摸区域 44px × 44px -->
<button class="min-h-[44px] min-w-[44px] ...">
  按钮
</button>
```

#### 2. 底部导航(移动端)
```html
<nav class="
  fixed bottom-0 left-0 right-0
  bg-white dark:bg-gray-800
  border-t border-gray-200 dark:border-gray-700
  md:hidden
  z-50
">
  <div class="grid grid-cols-4 h-16">
    <button class="flex flex-col items-center justify-center gap-1">
      <svg class="w-6 h-6">📊</svg>
      <span class="text-xs">工作台</span>
    </button>
    <!-- 更多导航项 -->
  </div>
</nav>
```

#### 3. 抽屉式侧边栏
```html
<!-- 移动端: 抽屉式滑出 -->
<aside class="
  fixed inset-y-0 left-0
  w-64
  bg-white dark:bg-gray-800
  transform -translate-x-full md:translate-x-0
  transition-transform duration-300
  z-40
">
  <!-- 侧边栏内容 -->
</aside>

<!-- 遮罩层 -->
<div class="
  fixed inset-0
  bg-black bg-opacity-50
  md:hidden
  z-30
"></div>
```

---

## AI功能视觉语言

### AI模块标识系统

#### 1. AI图标
```html
<!-- 渐变图标 -->
<svg class="w-6 h-6" viewBox="0 0 24 24">
  <defs>
    <linearGradient id="ai-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#667eea"/>
      <stop offset="100%" style="stop-color:#764ba2"/>
    </linearGradient>
  </defs>
  <path fill="url(#ai-gradient)" d="..."/>
</svg>

<!-- 闪电图标 + 渐变 -->
<svg class="w-5 h-5 text-indigo-600">
  <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
</svg>
```

#### 2. AI徽章
```html
<span class="
  inline-flex items-center gap-1
  px-2 py-1
  text-xs font-semibold
  bg-gradient-to-r from-indigo-600 to-purple-600
  text-white
  rounded-full
">
  <svg class="w-3 h-3">✨</svg>
  AI
</span>
```

#### 3. AI卡片装饰
```html
<div class="relative overflow-hidden rounded-xl bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-indigo-900/20 dark:to-purple-900/20">
  <!-- 背景光效 -->
  <div class="absolute top-0 right-0 w-64 h-64 bg-gradient-radial from-indigo-400/20 to-transparent rounded-full blur-3xl"></div>

  <!-- 内容 -->
  <div class="relative z-10 p-6">
    ...
  </div>
</div>
```

### AI动画效果

#### 1. 脉冲动画
```html
<div class="relative">
  <div class="absolute inset-0 bg-indigo-600 rounded-full opacity-75 animate-ping"></div>
  <div class="relative bg-indigo-600 rounded-full p-2">
    <svg class="w-4 h-4 text-white">✨</svg>
  </div>
</div>
```

#### 2. 扫光动画
```css
@keyframes shimmer {
  0% {
    background-position: -1000px 0;
  }
  100% {
    background-position: 1000px 0;
  }
}

.ai-shimmer {
  background-image: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.3) 50%,
    transparent 100%
  );
  background-size: 1000px 100%;
  animation: shimmer 2s infinite;
}
```

#### 3. 打字机效果
```javascript
// AI回复打字机效果
function typeWriter(text, element, speed = 50) {
  let i = 0;
  function type() {
    if (i < text.length) {
      element.textContent += text.charAt(i);
      i++;
      setTimeout(type, speed);
    }
  }
  type();
}
```

### AI功能配色方案

```css
/* AI主题渐变 */
--ai-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
--ai-secondary: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
--ai-accent: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);

/* AI功能背景 */
--ai-bg-light: linear-gradient(135deg, #f5f7fa 0%, #e8eaf6 100%);
--ai-bg-dark: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);

/* AI光效 */
--ai-glow: 0 0 20px rgba(102, 126, 234, 0.5);
--ai-glow-strong: 0 0 40px rgba(102, 126, 234, 0.8);
```

---

## 实战示例

### 完整页面示例: 工作台

```html
<!DOCTYPE html>
<html lang="zh-CN" class="light">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>工作台 - 智慧图书馆</title>
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-50 dark:bg-gray-900">

  <!-- 顶部导航 -->
  <nav class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 fixed top-0 left-0 right-0 z-30">
    <div class="mx-auto max-w-7xl px-4">
      <div class="flex h-16 justify-between items-center">
        <!-- Logo -->
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-gradient-to-br from-indigo-600 to-purple-600 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
            </svg>
          </div>
          <span class="text-xl font-bold text-gray-900 dark:text-white">智慧图书馆</span>
        </div>

        <!-- 搜索 -->
        <div class="flex-1 max-w-2xl mx-8">
          <div class="relative">
            <input
              type="search"
              class="w-full px-4 py-2 pl-10 bg-gray-100 dark:bg-gray-700 border-0 rounded-lg text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:ring-2 focus:ring-indigo-500"
              placeholder="搜索图书、读者..."
            >
            <svg class="absolute left-3 top-2.5 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
          </div>
        </div>

        <!-- 右侧操作 -->
        <div class="flex items-center gap-4">
          <!-- 通知 -->
          <button class="relative p-2 text-gray-400 hover:text-gray-500 dark:text-gray-300 dark:hover:text-gray-200 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
            </svg>
            <span class="absolute top-1 right-1 w-2 h-2 bg-red-600 rounded-full"></span>
          </button>

          <!-- AI助手 -->
          <button class="p-2 text-indigo-600 dark:text-indigo-400 rounded-lg hover:bg-indigo-50 dark:hover:bg-indigo-900/20">
            <svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
              <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
            </svg>
          </button>

          <!-- 主题切换 -->
          <button class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700">
            <svg class="w-6 h-6 hidden dark:block" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"/>
            </svg>
            <svg class="w-6 h-6 dark:hidden" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"/>
            </svg>
          </button>

          <!-- 用户菜单 -->
          <div class="flex items-center gap-2 cursor-pointer">
            <img class="h-8 w-8 rounded-full" src="https://ui-avatars.com/api/?name=Zhang+Teacher&background=4F46E5&color=fff" alt="">
            <span class="text-sm font-medium text-gray-700 dark:text-gray-300">张老师</span>
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
            </svg>
          </div>
        </div>
      </div>
    </div>
  </nav>

  <!-- 主内容区 -->
  <div class="flex pt-16">

    <!-- 侧边栏 -->
    <aside class="w-64 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 fixed left-0 top-16 bottom-0 overflow-y-auto">
      <nav class="p-4 space-y-1">
        <!-- 激活态导航 -->
        <a href="#" class="flex items-center gap-3 px-4 py-3 text-sm font-medium text-white bg-indigo-600 rounded-lg">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
          </svg>
          工作台
        </a>

        <!-- 默认态导航 -->
        <a href="#" class="flex items-center gap-3 px-4 py-3 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
          </svg>
          图书管理
        </a>

        <a href="#" class="flex items-center gap-3 px-4 py-3 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
          </svg>
          读者管理
        </a>

        <!-- AI功能区 -->
        <div class="mt-6 pt-6 border-t border-gray-200 dark:border-gray-700">
          <p class="px-4 mb-2 text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase">AI 功能</p>

          <a href="#" class="flex items-center gap-3 px-4 py-3 text-sm font-medium text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-900/20 rounded-lg border border-indigo-200 dark:border-indigo-800">
            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
            </svg>
            智能推荐
          </a>
        </div>
      </nav>
    </aside>

    <!-- 主内容 -->
    <main class="flex-1 ml-64 p-8">
      <!-- 欢迎区域 -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white mb-2">
          欢迎回来，张老师 👋
        </h1>
        <p class="text-gray-600 dark:text-gray-400">
          今天是 2025年1月14日，星期二
        </p>
      </div>

      <!-- 统计卡片 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <!-- 卡片1 -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border-l-4 border-indigo-600">
          <div class="flex items-center justify-between mb-4">
            <div class="p-3 bg-indigo-100 dark:bg-indigo-900/30 rounded-full">
              <svg class="w-8 h-8 text-indigo-600 dark:text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
              </svg>
            </div>
            <span class="text-sm text-green-600 dark:text-green-400 flex items-center gap-1 font-medium">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"/>
              </svg>
              +12%
            </span>
          </div>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-1">图书总量</p>
          <p class="text-3xl font-bold text-gray-900 dark:text-white">12,456</p>
        </div>

        <!-- 卡片2 -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border-l-4 border-green-600">
          <div class="flex items-center justify-between mb-4">
            <div class="p-3 bg-green-100 dark:bg-green-900/30 rounded-full">
              <svg class="w-8 h-8 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </div>
            <span class="text-sm text-green-600 dark:text-green-400 flex items-center gap-1 font-medium">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"/>
              </svg>
              +8%
            </span>
          </div>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-1">今日借阅</p>
          <p class="text-3xl font-bold text-gray-900 dark:text-white">342</p>
        </div>

        <!-- 卡片3 -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border-l-4 border-amber-600">
          <div class="flex items-center justify-between mb-4">
            <div class="p-3 bg-amber-100 dark:bg-amber-900/30 rounded-full">
              <svg class="w-8 h-8 text-amber-600 dark:text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
              </svg>
            </div>
            <span class="text-sm text-green-600 dark:text-green-400 flex items-center gap-1 font-medium">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"/>
              </svg>
              +23%
            </span>
          </div>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-1">活跃读者</p>
          <p class="text-3xl font-bold text-gray-900 dark:text-white">2,845</p>
        </div>

        <!-- 卡片4 -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border-l-4 border-red-600">
          <div class="flex items-center justify-between mb-4">
            <div class="p-3 bg-red-100 dark:bg-red-900/30 rounded-full">
              <svg class="w-8 h-8 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </div>
            <span class="text-sm text-red-600 dark:text-red-400 flex items-center gap-1 font-medium">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3"/>
              </svg>
              -5%
            </span>
          </div>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-1">超期未还</p>
          <p class="text-3xl font-bold text-gray-900 dark:text-white">48</p>
        </div>
      </div>

      <!-- AI推荐模块 -->
      <div class="mb-8">
        <div class="bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-indigo-900/20 dark:to-purple-900/20 rounded-xl p-6 border border-indigo-200 dark:border-indigo-800 relative overflow-hidden">
          <!-- 背景装饰 -->
          <div class="absolute top-0 right-0 w-64 h-64 bg-gradient-radial from-indigo-400/10 to-transparent rounded-full blur-3xl"></div>

          <!-- 内容 -->
          <div class="relative z-10">
            <div class="flex items-center justify-between mb-4">
              <div>
                <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-1">
                  AI 智能推荐
                </h2>
                <p class="text-gray-600 dark:text-gray-300 text-sm">
                  基于借阅数据和用户行为的智能推荐
                </p>
              </div>
              <span class="px-3 py-1 bg-gradient-to-r from-indigo-600 to-purple-600 text-white text-xs font-semibold rounded-full flex items-center gap-1">
                <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
                AI驱动
              </span>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <!-- 推荐卡片 -->
              <div class="bg-white dark:bg-gray-800 rounded-lg p-4 hover:shadow-lg transition-shadow">
                <div class="flex gap-3">
                  <img src="https://via.placeholder.com/80x120" alt="Book Cover" class="w-20 h-28 rounded object-cover">
                  <div class="flex-1">
                    <h3 class="font-semibold text-gray-900 dark:text-white text-sm mb-1">人工智能简史</h3>
                    <p class="text-xs text-gray-500 dark:text-gray-400 mb-2">尼克·博斯特罗姆</p>
                    <div class="flex items-center gap-1 mb-2">
                      <svg class="w-4 h-4 text-yellow-400" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                      </svg>
                      <span class="text-xs text-gray-600 dark:text-gray-400">4.8</span>
                    </div>
                    <div class="flex items-center gap-1 text-xs text-indigo-600 dark:text-indigo-400">
                      <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
                      </svg>
                      匹配度 92%
                    </div>
                  </div>
                </div>
              </div>

              <!-- 更多推荐卡片... -->
            </div>
          </div>
        </div>
      </div>

      <!-- 最近活动 -->
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-xl font-semibold text-gray-900 dark:text-white">
            最近借阅记录
          </h2>
        </div>

        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead class="bg-gray-50 dark:bg-gray-900/50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">读者</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">图书</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">借阅时间</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">应还时间</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">状态</th>
              </tr>
            </thead>
            <tbody class="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
              <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="flex items-center">
                    <img class="h-10 w-10 rounded-full" src="https://ui-avatars.com/api/?name=Zhang+San&background=4F46E5&color=fff" alt="">
                    <div class="ml-4">
                      <div class="text-sm font-medium text-gray-900 dark:text-white">张三</div>
                      <div class="text-sm text-gray-500 dark:text-gray-400">2024001</div>
                    </div>
                  </div>
                </td>
                <td class="px-6 py-4">
                  <div class="text-sm text-gray-900 dark:text-white font-medium">人工智能简史</div>
                  <div class="text-sm text-gray-500 dark:text-gray-400">尼克·博斯特罗姆</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                  2025-01-14
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                  2025-02-13
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span class="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">
                    借阅中
                  </span>
                </td>
              </tr>
              <!-- 更多行... -->
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div class="bg-white dark:bg-gray-800 px-4 py-3 border-t border-gray-200 dark:border-gray-700 sm:px-6">
          <div class="flex items-center justify-between">
            <div class="flex-1 flex justify-between sm:hidden">
              <button class="relative inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-sm font-medium rounded-md text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700">
                上一页
              </button>
              <button class="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-sm font-medium rounded-md text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700">
                下一页
              </button>
            </div>
            <div class="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
              <div>
                <p class="text-sm text-gray-700 dark:text-gray-300">
                  显示 <span class="font-medium">1</span> 到 <span class="font-medium">10</span> 条，
                  共 <span class="font-medium">97</span> 条结果
                </p>
              </div>
              <div>
                <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                  <button class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-sm font-medium text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700">
                    <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                  </button>
                  <button class="relative inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 bg-indigo-600 text-sm font-medium text-white">
                    1
                  </button>
                  <button class="relative inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700">
                    2
                  </button>
                  <button class="relative inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700">
                    3
                  </button>
                  <span class="relative inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-sm font-medium text-gray-700 dark:text-gray-300">
                    ...
                  </span>
                  <button class="relative inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700">
                    10
                  </button>
                  <button class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-sm font-medium text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700">
                    <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
                    </svg>
                  </button>
                </nav>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>

</body>
</html>
```

---

## 附录

### Tailwind CSS CDN 引入

```html
<!-- 开发环境 -->
<script src="https://cdn.tailwindcss.com"></script>

<!-- 自定义配置 -->
<script>
  tailwind.config = {
    darkMode: 'class',
    theme: {
      extend: {
        colors: {
          'ai-primary': '#667eea',
          'ai-secondary': '#764ba2',
        }
      }
    }
  }
</script>
```

### 常用 Tailwind 类速查

```text
/* 布局 */
container, flex, grid, block, inline-block, hidden
justify-center, items-center, gap-4

/* 尺寸 */
w-full, h-screen, max-w-7xl, min-h-full

/* 间距 */
p-4, px-6, py-3, m-4, mx-auto, space-y-4, gap-6

/* 文字 */
text-sm, text-xl, font-bold, text-center, text-gray-900

/* 颜色 */
bg-white, text-indigo-600, border-gray-200

/* 边框 */
border, border-2, rounded-lg, shadow-md

/* 交互 */
hover:bg-gray-50, focus:ring-2, transition-colors, cursor-pointer

/* 响应式 */
sm:text-lg, md:grid-cols-2, lg:flex, xl:max-w-7xl
```

---

**文档版本**: v2.0
**更新日期**: 2025-01-14
**设计系统**: 基于 Modernize Tailwind + AI 图书馆定制
**技术支持**: 国创睿峰前端团队
