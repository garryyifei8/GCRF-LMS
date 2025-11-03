# Chrome 调试设置计划

## 任务列表

### 前端调试 (Vue/Vite)
- [x] 检查当前 Vite 配置是否启用 source maps
- [x] 为 Vite 开发服务器配置 Chrome DevTools 调试
- [x] 测试前端断点调试

### Chrome DevTools MCP 集成
- [x] 验证 Chrome DevTools MCP 服务器是否已安装
- [x] 测试通过 Claude 使用 Chrome DevTools 工具
- [x] 创建常用调试命令文档

## 完成的工作

### 1. Vite 配置更新
- 在 `web-admin/vite.config.js` 中启用了开发环境的 source maps
- 配置项: `server.sourcemap: true`

### 2. 开发服务器启动
- Vite 开发服务器成功启动在端口 3011
- URL: http://localhost:3011

### 3. Chrome DevTools MCP 演示
成功演示了以下功能:
- ✅ 打开页面: `mcp__chrome-devtools__new_page`
- ✅ 截图: `mcp__chrome-devtools__take_screenshot`
- ✅ 页面快照: `mcp__chrome-devtools__take_snapshot`
- ✅ 表单填写: `mcp__chrome-devtools__fill`
- ✅ 点击操作: `mcp__chrome-devtools__click`
- ✅ 性能追踪: `performance_start_trace` / `performance_stop_trace`
- ✅ 网络请求查看: `list_network_requests`
- ✅ JavaScript 执行: `evaluate_script`
- ✅ 控制台消息: `list_console_messages`

### 4. 性能测试结果
登录操作性能指标:
- INP: 32 ms (优秀)
- CLS: 0.00 (优秀)
- 检测到强制重排警告,已记录在文档中

### 5. 文档创建
创建了完整的 Chrome 调试指南:
- 文件位置: `doc/chrome-debugging-guide.md`
- 包含配置说明、使用方法、常用场景、故障排除等

## 使用示例

通过 Claude 进行调试只需自然语言交互:
```
"请打开 http://localhost:3011 并截图"
"请填写登录表单并点击登录"
"请开始性能追踪,然后点击按钮,再停止追踪"
"请列出所有网络请求"
```

## 相关文件
- `web-admin/vite.config.js` - 已更新配置
- `doc/chrome-debugging-guide.md` - 调试指南
- `mcp.json` - MCP 配置

## 备注
- Chrome DevTools MCP 已连接并可用 ✓
- Source maps 已启用,支持在原始代码中设置断点
- 前端调试环境配置完成
