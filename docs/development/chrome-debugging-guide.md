# Chrome 调试指南

## 概述

本项目已配置完整的 Chrome 调试环境,支持通过 Chrome DevTools 和 Claude 的 MCP 工具进行前端调试。

## 配置说明

### 1. Vite 配置

已在 `web-admin/vite.config.js` 中启用开发环境的 source maps:

```javascript
server: {
  port: 3000,
  open: true,
  // 开发环境启用 source maps 以便调试
  sourcemap: true,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

### 2. Chrome DevTools MCP

Chrome DevTools MCP 已安装并连接,可以通过 Claude 直接控制浏览器。

检查 MCP 状态:
```bash
claude mcp list
```

## 使用方法

### 启动开发服务器

```bash
cd web-admin
npm run dev
```

服务器将自动在可用端口上启动(默认 3000,如果被占用会自动寻找下一个可用端口)。

### 通过 Claude 进行调试

#### 1. 打开页面

```
请通过 Chrome 打开 http://localhost:3011
```

Claude 会使用 `mcp__chrome-devtools__new_page` 工具打开页面。

#### 2. 截图查看

```
请截图当前页面
```

使用 `mcp__chrome-devtools__take_screenshot` 工具。

#### 3. 获取页面快照

```
请获取页面元素快照
```

使用 `mcp__chrome-devtools__take_snapshot` 工具,返回页面元素树结构和 uid。

#### 4. 与页面交互

填写表单:
```
请在用户名输入框填写 admin
```

点击按钮:
```
请点击登录按钮
```

#### 5. 查看网络请求

```
请列出所有网络请求
```

使用 `mcp__chrome-devtools__list_network_requests` 工具。

#### 6. 执行 JavaScript

```
请执行以下 JavaScript: 返回页面标题和 URL
```

使用 `mcp__chrome-devtools__evaluate_script` 工具。

#### 7. 性能分析

开始性能追踪:
```
请开始性能追踪
```

停止并查看结果:
```
请停止性能追踪并显示结果
```

性能追踪会提供:
- INP (Interaction to Next Paint) 指标
- CLS (Cumulative Layout Shift) 指标
- 强制重排检测
- 性能优化建议

#### 8. 查看控制台消息

```
请显示控制台消息
```

使用 `mcp__chrome-devtools__list_console_messages` 工具。

## 常用调试场景

### 场景 1: 调试登录功能

1. 打开登录页面
2. 获取页面快照,找到输入框的 uid
3. 填写用户名和密码
4. 开始性能追踪
5. 点击登录按钮
6. 停止性能追踪
7. 查看网络请求和控制台消息

### 场景 2: 性能优化

1. 开始性能追踪
2. 执行要测试的操作
3. 停止追踪
4. 分析 INP 和 CLS 指标
5. 查看性能建议(如强制重排警告)
6. 根据建议优化代码

### 场景 3: 网络请求调试

1. 清除网络日志
2. 执行操作
3. 列出所有网络请求
4. 查看特定请求的详细信息
5. 检查请求状态、响应时间等

### 场景 4: UI 交互测试

1. 获取页面快照
2. 通过 uid 与元素交互(点击、填写、悬停等)
3. 截图验证 UI 变化
4. 检查控制台错误

## 浏览器 DevTools 手动调试

除了通过 Claude 调试外,也可以直接使用 Chrome DevTools:

1. 在 Chrome 中打开应用: http://localhost:3011
2. 按 F12 或右键 -> 检查,打开 DevTools
3. 使用以下面板:
   - **Elements**: 查看和修改 DOM
   - **Console**: 查看日志和执行 JavaScript
   - **Sources**: 设置断点调试源代码(支持 source maps)
   - **Network**: 查看网络请求
   - **Performance**: 录制性能追踪
   - **Application**: 查看存储、缓存等

### 设置断点

由于已启用 source maps,可以在原始 Vue 代码中设置断点:

1. 打开 DevTools -> Sources 面板
2. 在左侧文件树中找到你的 `.vue` 或 `.js` 文件
3. 点击行号设置断点
4. 刷新页面或触发相关操作
5. 代码会在断点处暂停,可以查看变量、调用栈等

## 性能测试结果示例

最近一次测试(登录操作):

```
URL: http://localhost:3011/login?redirect=/dashboard
Metrics:
  - INP: 32 ms (优秀)
  - CLS: 0.00 (优秀)

性能洞察:
  - 检测到强制重排(forced reflow)
  - 建议: 避免在样式失效后查询几何属性
  - 相关时间范围: 1351398195907 - 1351398388251
```

## 故障排除

### 端口被占用

如果默认端口 3000 被占用,Vite 会自动寻找下一个可用端口。检查终端输出获取实际端口:

```
VITE v5.4.20  ready in 260 ms
➜  Local:   http://localhost:3011/
```

### Source Maps 不工作

1. 确认 `vite.config.js` 中 `server.sourcemap: true`
2. 清除浏览器缓存
3. 重启开发服务器

### MCP 连接失败

检查 MCP 状态:
```bash
claude mcp list
```

如果 chrome-devtools 显示 ✗ Failed to connect,尝试:
1. 重启 Chrome
2. 检查是否安装了 chrome-devtools-mcp
3. 查看 `mcp.json` 配置

## 相关文件

- `web-admin/vite.config.js` - Vite 配置文件
- `mcp.json` - MCP 服务器配置
- 本文档: `doc/chrome-debugging-guide.md`
