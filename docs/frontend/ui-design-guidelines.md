# 国创睿峰智能图书馆管理系统 - UI设计规范

**版本**: v1.0
**设计日期**: 2025-10-10
**设计单位**: 国创睿峰科技有限公司
**文档状态**: 正式版

---

## 目录

1. [设计规范概述](#1-设计规范概述)
2. [色彩规范](#2-色彩规范)
3. [字体规范](#3-字体规范)
4. [图标规范](#4-图标规范)
5. [间距与布局规范](#5-间距与布局规范)
6. [组件规范](#6-组件规范)
7. [响应式设计规范](#7-响应式设计规范)
8. [暗黑模式规范](#8-暗黑模式规范)

---

## 1. 设计规范概述

### 1.1 设计原则

| 原则 | 说明 | 体现 |
|------|------|------|
| **简洁优先** | 界面简洁,去除冗余元素 | 扁平化设计,留白充足 |
| **一致性** | 跨平台视觉语言统一 | 统一色彩、字体、组件 |
| **可读性** | 文字清晰易读,信息层级分明 | 高对比度,合理字号 |
| **智慧科技感** | 体现AI智能特性 | 蓝色主色调,圆角设计 |
| **亲和力** | 面向教育场景,温和友好 | 圆润图标,柔和色彩 |

### 1.2 适用范围

- **Web管理端**: PC浏览器 (Chrome、Firefox、Safari、Edge)
- **小程序端**: 微信小程序 (iOS、Android)
- **自助借还机**: 触摸屏终端 (Windows系统)

### 1.3 设计工具

- **设计软件**: Figma (推荐) / Sketch / Adobe XD
- **原型工具**: Figma / Axure RP
- **图标库**: Ant Design Icons / Material Icons
- **插图**: unDraw / Blush / Humaaans
- **版本管理**: Figma版本控制 / Git LFS

---

## 2. 色彩规范

### 2.1 品牌色

#### 2.1.1 主色 (Primary Color)

**蓝色 Blue** - 智慧、科技、信任

```
主色: #1890FF (Ant Design Blue)
RGB: (24, 144, 255)
HSB: (210°, 91%, 100%)

色板:
Blue-1 (最浅): #E6F7FF  hover背景
Blue-2:        #BAE7FF
Blue-3:        #91D5FF
Blue-4:        #69C0FF
Blue-5:        #40A9FF  禁用态
Blue-6:        #1890FF  ★ 主色
Blue-7:        #096DD9  hover态
Blue-8:        #0050B3  active态
Blue-9:        #003A8C
Blue-10 (最深): #002766 深色背景
```

**使用场景**:
- 主要按钮背景色
- 链接文字颜色
- 选中/激活状态
- 数据可视化主色调
- Logo主色

#### 2.1.2 辅助色 (Secondary Colors)

**绿色 Green** - 成功、积极、正常

```
绿色: #52C41A
RGB: (82, 196, 26)

色板:
Green-1: #F6FFED
Green-6: #52C41A  ★ 成功色
Green-7: #389E0D  hover态
```

**使用场景**:
- 成功提示
- 在架状态标识
- 正向数据增长
- 成功按钮

**橙色 Orange** - 警告、提醒

```
橙色: #FA8C16
RGB: (250, 140, 22)

色板:
Orange-1: #FFF7E6
Orange-6: #FA8C16  ★ 警告色
Orange-7: #D46B08  hover态
```

**使用场景**:
- 警告提示
- 即将逾期
- 预约状态
- 重要提醒

**红色 Red** - 错误、逾期、危险

```
红色: #F5222D
RGB: (245, 34, 45)

色板:
Red-1: #FFF1F0
Red-6: #F5222D  ★ 错误色
Red-7: #CF1322  hover态
```

**使用场景**:
- 错误提示
- 逾期标识
- 删除/剔旧操作
- 危险按钮

### 2.2 中性色 (Neutral Colors)

**灰色系** - 文字、边框、背景

```
文字颜色:
Neutral-1 (标题): #000000  rgba(0,0,0,0.85)
Neutral-2 (正文): #595959  rgba(0,0,0,0.65)
Neutral-3 (辅助): #8C8C8C  rgba(0,0,0,0.45)
Neutral-4 (禁用): #BFBFBF  rgba(0,0,0,0.25)

边框颜色:
Border-1 (深边框): #D9D9D9  rgba(0,0,0,0.15)
Border-2 (浅边框): #F0F0F0  rgba(0,0,0,0.06)

背景颜色:
BG-1 (页面背景): #F5F5F5
BG-2 (组件背景): #FAFAFA
BG-3 (白色背景): #FFFFFF
BG-4 (悬停背景): #F5F5F5
```

**使用场景**:
- Neutral-1: 页面标题、卡片标题、重要文字
- Neutral-2: 正文、表格内容、表单标签
- Neutral-3: 次要信息、辅助文字、placeholder
- Neutral-4: 禁用文字、禁用按钮

### 2.3 功能色

| 功能 | 颜色 | 色值 | 使用场景 |
|------|------|------|----------|
| **信息 Info** | 蓝色 | #1890FF | 提示信息、通知 |
| **成功 Success** | 绿色 | #52C41A | 成功提示、正常状态 |
| **警告 Warning** | 橙色 | #FA8C16 | 警告提示、即将逾期 |
| **错误 Error** | 红色 | #F5222D | 错误提示、逾期 |
| **链接 Link** | 蓝色 | #1890FF | 超链接、可点击文字 |

### 2.4 色彩应用示例

#### 2.4.1 状态标识色

```css
/* 图书状态 */
.status-available { color: #52C41A; }  /* 在架 - 绿色 */
.status-borrowed  { color: #8C8C8C; }  /* 借出 - 灰色 */
.status-reserved  { color: #FA8C16; }  /* 预约 - 橙色 */
.status-overdue   { color: #F5222D; }  /* 逾期 - 红色 */
.status-removed   { color: #BFBFBF; }  /* 剔旧 - 浅灰 */
```

#### 2.4.2 数据可视化色板

**连续色板** (用于热力图、渐变):
```
Blue Scale:
#E6F7FF → #BAE7FF → #69C0FF → #1890FF → #096DD9→ #003A8C

Green to Red (数据对比):
#52C41A → #FFC53D → #FA8C16 → #F5222D
```

**分类色板** (用于饼图、柱状图):
```
Category-1: #1890FF  蓝色
Category-2: #52C41A  绿色
Category-3: #FA8C16  橙色
Category-4: #9254DE  紫色
Category-5: #13C2C2  青色
Category-6: #FA541C  深橙
Category-7: #2F54EB  深蓝
Category-8: #EB2F96  粉色
```

### 2.5 色彩对比度规范

**WCAG 2.1 AA级标准**:

| 文字类型 | 对比度要求 | 示例 |
|---------|-----------|------|
| **大文字** (≥18px 粗体/≥24px) | ≥3:1 | 标题与背景 |
| **普通文字** (<18px 粗体/<24px) | ≥4.5:1 | 正文与背景 |
| **图标、图形** | ≥3:1 | 图标与背景 |

**检测工具**: WebAIM Contrast Checker, Figma插件 Stark

---

## 3. 字体规范

### 3.1 字体家族

#### 3.1.1 中文字体

**优先级排序**:
```css
font-family:
  "PingFang SC",           /* macOS/iOS 苹方 */
  "Microsoft YaHei",       /* Windows 微软雅黑 */
  "Helvetica Neue",        /* macOS fallback */
  "Helvetica",             /* 通用 */
  "Arial",                 /* Windows fallback */
  sans-serif;              /* 系统默认 */
```

**字体特性**:
- **PingFang SC**: 苹果中文字体,清晰现代,适合屏幕显示
- **Microsoft YaHei**: Windows默认中文字体,兼容性好

#### 3.1.2 西文/数字字体

**优先级排序**:
```css
font-family:
  -apple-system,           /* macOS/iOS 系统字体 */
  BlinkMacSystemFont,      /* macOS Chrome */
  "Segoe UI",              /* Windows */
  Roboto,                  /* Android */
  "Helvetica Neue",
  Arial,
  sans-serif;
```

#### 3.1.3 等宽字体 (代码、数据)

```css
font-family:
  "SF Mono",               /* macOS */
  "Consolas",              /* Windows */
  "Monaco",
  "Courier New",
  monospace;
```

**使用场景**: 读者证号、ISBN、条码号、JSON数据

### 3.2 字号规范

#### 3.2.1 Web管理端字号

| 层级 | 字号 | 行高 | 使用场景 |
|------|------|------|----------|
| **H1 一级标题** | 24px | 32px | 页面主标题 |
| **H2 二级标题** | 20px | 28px | 卡片标题、模块标题 |
| **H3 三级标题** | 18px | 26px | 列表标题、表单分组 |
| **H4 四级标题** | 16px | 24px | 子标题 |
| **Body 正文** | 14px | 22px | 正文内容、表格 |
| **Caption 辅助文字** | 12px | 20px | 说明文字、时间戳 |
| **Small 小字** | 10px (谨慎使用) | 18px | 极小说明 |

#### 3.2.2 小程序端字号

| 层级 | 字号 (rpx) | 字号 (px) | 行高 | 使用场景 |
|------|-----------|-----------|------|----------|
| **Title 标题** | 36rpx | 18px | 48rpx | 页面标题 |
| **Subtitle 副标题** | 32rpx | 16px | 44rpx | 卡片标题 |
| **Body 正文** | 28rpx | 14px | 40rpx | 正文内容 |
| **Caption 辅助** | 24rpx | 12px | 36rpx | 辅助文字 |

### 3.3 字重规范

```css
/* 字重定义 */
font-weight: 300;  /* Light - 极少使用 */
font-weight: 400;  /* Regular/Normal - 正文默认 */
font-weight: 500;  /* Medium - 强调 */
font-weight: 600;  /* Semibold - 小标题 */
font-weight: 700;  /* Bold - 大标题 */
```

**使用建议**:
- **标题**: 700 (Bold) 或 600 (Semibold)
- **正文**: 400 (Regular)
- **强调文字**: 500 (Medium)

### 3.4 字体应用示例

```css
/* 页面标题 */
.page-title {
  font-size: 24px;
  font-weight: 700;
  line-height: 32px;
  color: rgba(0,0,0,0.85);
}

/* 卡片标题 */
.card-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 26px;
  color: rgba(0,0,0,0.85);
}

/* 正文 */
.body-text {
  font-size: 14px;
  font-weight: 400;
  line-height: 22px;
  color: rgba(0,0,0,0.65);
}

/* 辅助文字 */
.caption-text {
  font-size: 12px;
  font-weight: 400;
  line-height: 20px;
  color: rgba(0,0,0,0.45);
}

/* 数字/代码 */
.code-text {
  font-family: "SF Mono", "Consolas", monospace;
  font-size: 14px;
  color: #1890FF;
}
```

---

## 4. 图标规范

### 4.1 图标风格

**线性图标** - 统一使用Ant Design Icons

**特点**:
- 2px描边
- 圆角端点
- 24×24px 标准尺寸
- 简洁现代
- 识别性强

### 4.2 图标尺寸

| 场景 | 尺寸 | 说明 |
|------|------|------|
| **超小图标** | 12px | 表格内图标、标签图标 |
| **小图标** | 16px | 按钮图标、输入框图标 |
| **常规图标** | 20px | 菜单图标、Tab图标 |
| **标准图标** | 24px | 主要功能图标 |
| **大图标** | 32px | 空状态图标、卡片图标 |
| **超大图标** | 48px+ | 启动页、引导页 |

### 4.3 图标色彩

| 状态 | 颜色 | 色值 | 使用场景 |
|------|------|------|----------|
| **默认** | 深灰 | rgba(0,0,0,0.65) | 未激活状态 |
| **激活** | 主色蓝 | #1890FF | 选中/激活状态 |
| **悬停** | 深蓝 | #096DD9 | 鼠标悬停 |
| **禁用** | 浅灰 | rgba(0,0,0,0.25) | 禁用状态 |
| **成功** | 绿色 | #52C41A | 成功图标 |
| **警告** | 橙色 | #FA8C16 | 警告图标 |
| **错误** | 红色 | #F5222D | 错误图标 |

### 4.4 核心图标库

**导航类图标**:
```
首页:      home
图书管理:   book
流通管理:   swap
读者管理:   user / team
统计分析:   bar-chart / line-chart
系统管理:   setting
```

**操作类图标**:
```
新增:      plus / plus-circle
编辑:      edit
删除:      delete / close-circle
查看:      eye
搜索:      search
刷新:      reload
导出:      download
打印:      printer
```

**状态类图标**:
```
成功:      check-circle (绿色)
警告:      exclamation-circle (橙色)
错误:      close-circle (红色)
信息:      info-circle (蓝色)
加载:      loading / spin
```

**功能类图标**:
```
扫码:      scan / qrcode
人脸:      smile / user
语音:      audio
拍照:      camera
上传:      upload
下载:      download
```

### 4.5 图标应用规范

**规则**:
1. 图标与文字垂直居中对齐
2. 图标与文字间距 4-8px
3. 纯图标按钮需提供tooltip说明
4. 保持图标视觉重量一致
5. 避免过度使用图标

**示例**:
```html
<!-- 按钮图标 -->
<button>
  <Icon type="plus" size="16px" />
  <span>新增图书</span>
</button>

<!-- 状态图标 -->
<div class="status-tag">
  <Icon type="check-circle" color="#52C41A" size="14px" />
  <span>在架</span>
</div>

<!-- 纯图标按钮 -->
<button tooltip="刷新">
  <Icon type="reload" size="20px" />
</button>
```

---

## 5. 间距与布局规范

### 5.1 8px网格系统

**基础单位**: 8px

**间距规范**:
```
xs:   4px   (0.5倍)
sm:   8px   (1倍)  ★ 基础间距
md:   16px  (2倍)  ★ 常用间距
lg:   24px  (3倍)
xl:   32px  (4倍)
xxl:  48px  (6倍)
```

**使用场景**:
- **4px**: 紧密元素间距 (图标与文字、标签间距)
- **8px**: 组件内元素间距 (表单字段、按钮内边距)
- **16px**: 卡片内边距、列表项间距
- **24px**: 模块间距、卡片间距
- **32px**: 大模块间距、页面边距
- **48px**: 页面顶部/底部留白

### 5.2 Web管理端布局

#### 5.2.1 整体布局

```
┌─────────────────────────────────────────┐
│ Navbar (64px高)                         │
├──────┬──────────────────────────────────┤
│      │  Content (主内容区)              │
│Sider │  Padding: 24px                   │
│200px │                                  │
│      │  Min-width: 1024px               │
│      │                                  │
└──────┴──────────────────────────────────┘
```

**尺寸规范**:
- **顶部导航栏**: 64px
- **侧边栏**: 200px (展开) / 64px (收起)
- **主内容区边距**: 24px
- **页面最小宽度**: 1024px
- **页面最大宽度**: 1920px (超过居中)

#### 5.2.2 卡片布局

```css
.card {
  padding: 16px 24px;       /* 上下16px, 左右24px */
  margin-bottom: 16px;      /* 卡片间距 */
  border-radius: 4px;       /* 圆角 */
  box-shadow: 0 1px 2px rgba(0,0,0,0.06); /* 阴影 */
}

.card-title {
  margin-bottom: 16px;      /* 标题与内容间距 */
  font-size: 18px;
  font-weight: 600;
}

.card-content {
  /* 卡片内容区 */
}
```

#### 5.2.3 表单布局

```css
.form-item {
  margin-bottom: 24px;      /* 表单项间距 */
}

.form-label {
  margin-bottom: 8px;       /* 标签与输入框间距 */
  font-size: 14px;
  color: rgba(0,0,0,0.85);
}

.form-input {
  height: 32px;             /* 输入框高度 */
  padding: 4px 11px;        /* 输入框内边距 */
  border-radius: 4px;       /* 圆角 */
}
```

### 5.3 小程序端布局

#### 5.3.1 页面布局

```css
/* 页面容器 */
.page {
  padding: 32rpx;           /* 页面边距 (16px) */
  background: #F5F5F5;
}

/* 卡片 */
.card {
  padding: 24rpx 32rpx;     /* 上下24rpx, 左右32rpx */
  margin-bottom: 24rpx;     /* 卡片间距 */
  border-radius: 16rpx;     /* 圆角 (8px) */
  background: #FFFFFF;
}

/* 列表项 */
.list-item {
  padding: 24rpx 0;         /* 上下内边距 */
  border-bottom: 1px solid #F0F0F0;
}
```

#### 5.3.2 安全区适配

```css
/* 底部Tab栏适配刘海屏 */
.tab-bar {
  padding-bottom: env(safe-area-inset-bottom); /* 底部安全区 */
}

/* 顶部导航栏适配刘海屏 */
.nav-bar {
  padding-top: env(safe-area-inset-top);      /* 顶部安全区 */
}
```

### 5.4 栅格系统

**24栅格布局** (参考Ant Design Grid)

```html
<!-- 两列布局 (50%-50%) -->
<Row gutter="16">
  <Col span="12">左侧内容</Col>
  <Col span="12">右侧内容</Col>
</Row>

<!-- 三列布局 (33%-33%-33%) -->
<Row gutter="16">
  <Col span="8">左侧</Col>
  <Col span="8">中间</Col>
  <Col span="8">右侧</Col>
</Row>

<!-- 两列布局 (33%-67%) -->
<Row gutter="16">
  <Col span="8">侧边栏</Col>
  <Col span="16">主内容</Col>
</Row>
```

**gutter间距**: 8px, 16px, 24px, 32px

---

## 6. 组件规范

### 6.1 按钮 (Button)

#### 6.1.1 按钮类型

**主要按钮 (Primary Button)**:
```css
.btn-primary {
  background: #1890FF;
  color: #FFFFFF;
  border: none;
  border-radius: 4px;
  padding: 4px 15px;
  height: 32px;
  font-size: 14px;
  cursor: pointer;
}

.btn-primary:hover {
  background: #40A9FF;
}

.btn-primary:active {
  background: #096DD9;
}

.btn-primary:disabled {
  background: #F5F5F5;
  color: rgba(0,0,0,0.25);
  cursor: not-allowed;
}
```

**次要按钮 (Default Button)**:
```css
.btn-default {
  background: #FFFFFF;
  color: rgba(0,0,0,0.65);
  border: 1px solid #D9D9D9;
  border-radius: 4px;
  padding: 4px 15px;
  height: 32px;
}

.btn-default:hover {
  color: #40A9FF;
  border-color: #40A9FF;
}
```

**文字按钮 (Text Button)**:
```css
.btn-text {
  background: transparent;
  color: #1890FF;
  border: none;
  padding: 4px 0;
  height: auto;
}

.btn-text:hover {
  color: #40A9FF;
}
```

**危险按钮 (Danger Button)**:
```css
.btn-danger {
  background: #FF4D4F;
  color: #FFFFFF;
  border: none;
}

.btn-danger:hover {
  background: #FF7875;
}
```

#### 6.1.2 按钮尺寸

| 尺寸 | 高度 | 内边距 | 字号 | 使用场景 |
|------|------|--------|------|----------|
| **Large** | 40px | 7px 15px | 16px | 表单提交、主要操作 |
| **Default** | 32px | 4px 15px | 14px | 常规按钮 |
| **Small** | 24px | 0px 7px | 14px | 表格操作、紧凑场景 |

#### 6.1.3 按钮状态

```
默认 → 悬停 (hover) → 按下 (active) → 禁用 (disabled)
```

### 6.2 输入框 (Input)

#### 6.2.1 基础输入框

```css
.input {
  width: 100%;
  height: 32px;
  padding: 4px 11px;
  border: 1px solid #D9D9D9;
  border-radius: 4px;
  font-size: 14px;
  color: rgba(0,0,0,0.85);
  transition: all 0.3s;
}

.input::placeholder {
  color: rgba(0,0,0,0.25);
}

.input:hover {
  border-color: #40A9FF;
}

.input:focus {
  border-color: #40A9FF;
  box-shadow: 0 0 0 2px rgba(24,144,255,0.2);
  outline: none;
}

.input:disabled {
  background: #F5F5F5;
  color: rgba(0,0,0,0.25);
  cursor: not-allowed;
}

.input.input-error {
  border-color: #FF4D4F;
}
```

#### 6.2.2 输入框尺寸

| 尺寸 | 高度 | 内边距 | 字号 |
|------|------|--------|------|
| **Large** | 40px | 6px 11px | 16px |
| **Default** | 32px | 4px 11px | 14px |
| **Small** | 24px | 0px 7px | 14px |

#### 6.2.3 带图标输入框

```html
<!-- 前缀图标 -->
<div class="input-affix">
  <Icon type="search" class="input-prefix" />
  <input placeholder="搜索图书..." />
</div>

<!-- 后缀图标 -->
<div class="input-affix">
  <input placeholder="请输入密码" type="password" />
  <Icon type="eye" class="input-suffix" />
</div>
```

### 6.3 下拉选择 (Select)

```css
.select {
  height: 32px;
  padding: 4px 11px;
  border: 1px solid #D9D9D9;
  border-radius: 4px;
  background: #FFFFFF;
  cursor: pointer;
}

.select:hover {
  border-color: #40A9FF;
}

.select-dropdown {
  max-height: 256px;
  overflow-y: auto;
  border: 1px solid #F0F0F0;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.select-option {
  padding: 5px 12px;
  cursor: pointer;
}

.select-option:hover {
  background: #F5F5F5;
}

.select-option.selected {
  background: #E6F7FF;
  color: #1890FF;
}
```

### 6.4 表格 (Table)

```css
.table {
  width: 100%;
  border-collapse: collapse;
  background: #FFFFFF;
}

.table th {
  padding: 16px;
  background: #FAFAFA;
  font-weight: 600;
  font-size: 14px;
  color: rgba(0,0,0,0.85);
  text-align: left;
  border-bottom: 1px solid #F0F0F0;
}

.table td {
  padding: 16px;
  font-size: 14px;
  color: rgba(0,0,0,0.65);
  border-bottom: 1px solid #F0F0F0;
}

.table tr:hover {
  background: #FAFAFA;
}

/* 斑马纹 (可选) */
.table-striped tr:nth-child(even) {
  background: #FAFAFA;
}
```

**表格操作列**:
```html
<td class="table-actions">
  <a href="#">查看</a>
  <Divider type="vertical" />
  <a href="#">编辑</a>
  <Divider type="vertical" />
  <a href="#" class="text-danger">删除</a>
</td>
```

### 6.5 卡片 (Card)

```css
.card {
  background: #FFFFFF;
  border: 1px solid #F0F0F0;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
  overflow: hidden;
}

.card-head {
  padding: 16px 24px;
  border-bottom: 1px solid #F0F0F0;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0,0,0,0.85);
}

.card-body {
  padding: 24px;
}

.card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
```

### 6.6 标签 (Tag)

```css
.tag {
  display: inline-block;
  padding: 0 7px;
  height: 22px;
  line-height: 20px;
  border: 1px solid #D9D9D9;
  border-radius: 4px;
  font-size: 12px;
  color: rgba(0,0,0,0.65);
  background: #FAFAFA;
}

.tag-success {
  background: #F6FFED;
  border-color: #B7EB8F;
  color: #52C41A;
}

.tag-warning {
  background: #FFF7E6;
  border-color: #FFD591;
  color: #FA8C16;
}

.tag-error {
  background: #FFF1F0;
  border-color: #FFCCC7;
  color: #FF4D4F;
}

.tag-info {
  background: #E6F7FF;
  border-color: #91D5FF;
  color: #1890FF;
}
```

### 6.7 消息提示 (Message / Toast)

```css
.message {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 16px;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  background: #FFFFFF;
  display: flex;
  align-items: center;
  z-index: 1000;
}

.message-success {
  color: #52C41A;
}

.message-error {
  color: #FF4D4F;
}

.message-warning {
  color: #FA8C16;
}

.message-info {
  color: #1890FF;
}

.message-icon {
  margin-right: 8px;
  font-size: 16px;
}
```

### 6.8 分页 (Pagination)

```css
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
}

.pagination-item {
  min-width: 32px;
  height: 32px;
  padding: 0 6px;
  border: 1px solid #D9D9D9;
  border-radius: 4px;
  background: #FFFFFF;
  cursor: pointer;
  text-align: center;
  line-height: 30px;
}

.pagination-item:hover {
  color: #1890FF;
  border-color: #1890FF;
}

.pagination-item.active {
  background: #1890FF;
  color: #FFFFFF;
  border-color: #1890FF;
}

.pagination-item:disabled {
  color: rgba(0,0,0,0.25);
  cursor: not-allowed;
}
```

---

## 7. 响应式设计规范

### 7.1 断点定义

```css
/* 断点变量 */
$xs: 0;       /* 移动端 */
$sm: 576px;   /* 小平板 */
$md: 768px;   /* 平板 */
$lg: 992px;   /* 小桌面 */
$xl: 1200px;  /* 桌面 */
$xxl: 1600px; /* 大桌面 */

/* 媒体查询 */
@media (min-width: $sm) { /* 小平板及以上 */ }
@media (min-width: $md) { /* 平板及以上 */ }
@media (min-width: $lg) { /* 小桌面及以上 */ }
@media (min-width: $xl) { /* 桌面及以上 */ }
```

### 7.2 响应式布局策略

**移动优先 (Mobile First)**:
```css
/* 默认移动端样式 */
.container {
  padding: 16px;
}

/* 平板及以上 */
@media (min-width: 768px) {
  .container {
    padding: 24px;
  }
}

/* 桌面及以上 */
@media (min-width: 1200px) {
  .container {
    padding: 32px;
    max-width: 1200px;
    margin: 0 auto;
  }
}
```

### 7.3 小程序适配

**rpx单位**: 750rpx = 375px (设计稿基准)

```css
/* 设计稿375px宽度下的32px → 64rpx */
.card {
  padding: 64rpx;
}

/* 字号建议使用px (不随屏幕缩放) */
.title {
  font-size: 18px;
}
```

---

## 8. 暗黑模式规范

### 8.1 暗黑模式色板

```css
/* 暗黑模式主色 */
--dark-bg-1: #141414;      /* 页面背景 */
--dark-bg-2: #1F1F1F;      /* 卡片背景 */
--dark-bg-3: #2A2A2A;      /* 悬停背景 */
--dark-border: #434343;    /* 边框 */
--dark-text-1: rgba(255,255,255,0.85);  /* 标题 */
--dark-text-2: rgba(255,255,255,0.65);  /* 正文 */
--dark-text-3: rgba(255,255,255,0.45);  /* 辅助 */

/* 功能色保持不变 */
--primary: #1890FF;
--success: #52C41A;
--warning: #FA8C16;
--error: #FF4D4F;
```

### 8.2 暗黑模式切换

```css
/* 浅色模式 (默认) */
:root {
  --bg-color: #FFFFFF;
  --text-color: rgba(0,0,0,0.85);
}

/* 暗黑模式 */
[data-theme="dark"] {
  --bg-color: #141414;
  --text-color: rgba(255,255,255,0.85);
}

/* 应用变量 */
body {
  background: var(--bg-color);
  color: var(--text-color);
}
```

---

## 9. 设计交付规范

### 9.1 设计稿命名

```
格式: [平台]-[页面]-[状态].figma

示例:
Web-首页-默认.figma
Web-图书借出-操作中.figma
Mini-图书详情-在架.figma
Mini-图书详情-借出.figma
```

### 9.2 设计标注

**必须标注**:
- 字号、行高、字重
- 色值 (Hex)
- 间距 (padding、margin)
- 圆角半径
- 阴影参数
- 图标名称和尺寸

**工具**: Figma自带标注 / 蓝湖 / Zeplin

### 9.3 切图规范

**图片格式**:
- **PNG**: 图标、Logo (需要透明背景)
- **JPG**: 照片、封面图 (不需要透明)
- **SVG**: 矢量图标 (推荐)
- **WebP**: 高压缩率 (现代浏览器)

**倍图**:
- Web: @1x, @2x
- 小程序: @1x, @2x, @3x

**命名规范**:
```
图标: icon-[功能]-[尺寸].svg
示例: icon-search-24.svg

图片: img-[页面]-[描述]-[尺寸].png
示例: img-home-banner-1200x400.png
```

---

## 10. 附录

### 10.1 设计资源

**图标库**:
- Ant Design Icons: https://ant.design/components/icon-cn/
- Material Icons: https://fonts.google.com/icons
- Feather Icons: https://feathericons.com/

**插画库**:
- unDraw: https://undraw.co/
- Blush: https://blush.design/
- Humaaans: https://www.humaaans.com/

**配色工具**:
- Coolors: https://coolors.co/
- Adobe Color: https://color.adobe.com/
- Ant Design Colors: https://ant.design/docs/spec/colors-cn

**设计系统参考**:
- Ant Design: https://ant.design/
- Material Design: https://material.io/design
- Element Plus: https://element-plus.org/

### 10.2 相关文档

- [信息架构设计](./information-architecture.md)
- [交互原型文档](./interaction-prototype.md)
- [交互设计文档](./interaction-design.md)
- [PRD产品需求文档](./PRD.md)

---

**文档结束**
