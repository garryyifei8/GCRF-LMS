# Pinia 持久化插件安装配置指南

## 当前状态

项目中的Store已经配置了persist选项，但**pinia-plugin-persistedstate插件尚未安装**。

## 检查是否需要持久化

虽然各Store已配置persist，但实际上Pinia本身**不支持**持久化，需要安装插件才能生效。

### 测试持久化是否生效

在浏览器控制台运行：

```javascript
// 设置一些数据
import { useBookStore } from '@/stores'
const bookStore = useBookStore()
bookStore.addSearchHistory('测试')

// 刷新页面
location.reload()

// 再次检查
const bookStore = useBookStore()
console.log(bookStore.searchHistory) // 如果为空，说明持久化未生效
```

---

## 安装持久化插件

### 方案1: pinia-plugin-persistedstate (推荐)

这是最流行的Pinia持久化插件，功能强大且易用。

#### 1. 安装插件

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npm install pinia-plugin-persistedstate
```

#### 2. 配置插件

修改 `src/main.js`:

```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'

// 样式导入
import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'
import './styles/index.scss'

// Element Plus
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 创建并配置Vue应用
function mountApp() {
  const app = createApp(App)

  // 创建Pinia实例并配置持久化插件
  const pinia = createPinia()
  pinia.use(piniaPluginPersistedstate)

  // 注册 Pinia 和 Router
  app.use(pinia)
  app.use(router)

  // 注册 Element Plus
  app.use(ElementPlus, {
    locale: zhCn,
    size: 'default'
  })

  // 注册 Element Plus 图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.mount('#app')
  console.log('[App] Vue application mounted with Pinia persistence enabled')
}

// 直接挂载应用 - 使用真实后端 API
console.log('[App] MSW disabled - using real backend API')
mountApp()
```

#### 3. 验证配置

启动开发服务器：

```bash
npm run dev
```

打开浏览器DevTools，在Application → Local Storage中应该能看到存储的数据：

- `user`: 用户信息
- `book`: 最近访问、搜索历史
- `reader`: 搜索历史、最近操作
- `system`: 系统配置

在Session Storage中应该能看到：

- `circulation`: 借阅购物车、归还购物车

---

### 方案2: 自定义持久化 (备用方案)

如果不想安装第三方插件，可以自己实现简单的持久化。

#### 1. 创建持久化插件

创建 `src/plugins/pinia-persist.js`:

```javascript
/**
 * 自定义Pinia持久化插件
 */
export function createPersistedStatePlugin() {
  return (context) => {
    const { store, options } = context

    // 检查是否配置了持久化
    if (!options.persist?.enabled) return

    const strategies = options.persist.strategies || []

    strategies.forEach((strategy) => {
      const { key, storage, paths } = strategy
      const storageInstance = storage === sessionStorage ? sessionStorage : localStorage

      // 从存储中恢复数据
      const savedState = storageInstance.getItem(key)
      if (savedState) {
        try {
          const parsed = JSON.parse(savedState)

          // 如果指定了paths，只恢复这些字段
          if (paths && paths.length > 0) {
            paths.forEach((path) => {
              if (parsed[path] !== undefined) {
                store.$state[path] = parsed[path]
              }
            })
          } else {
            // 否则恢复所有字段
            store.$patch(parsed)
          }
        } catch (error) {
          console.error(`Failed to restore state for ${key}:`, error)
        }
      }

      // 监听状态变化并保存
      store.$subscribe(
        (mutation, state) => {
          const toSave = {}

          // 如果指定了paths，只保存这些字段
          if (paths && paths.length > 0) {
            paths.forEach((path) => {
              if (state[path] !== undefined) {
                toSave[path] = state[path]
              }
            })
          } else {
            // 否则保存所有字段
            Object.assign(toSave, state)
          }

          try {
            storageInstance.setItem(key, JSON.stringify(toSave))
          } catch (error) {
            console.error(`Failed to persist state for ${key}:`, error)
          }
        },
        { detached: true }
      )
    })
  }
}
```

#### 2. 在main.js中使用

```javascript
import { createPinia } from 'pinia'
import { createPersistedStatePlugin } from './plugins/pinia-persist'

const pinia = createPinia()
pinia.use(createPersistedStatePlugin())

app.use(pinia)
```

---

## Store配置说明

所有Store的persist配置已经就绪，无需修改。配置格式如下：

### user.js (完整持久化)

```javascript
{
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'user',
        storage: localStorage
        // 未指定paths，持久化所有字段
      }
    ]
  }
}
```

### book.js (部分持久化)

```javascript
{
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'book',
        storage: localStorage,
        paths: ['recentBooks', 'searchHistory'] // 只持久化这两个字段
      }
    ]
  }
}
```

### reader.js (部分持久化)

```javascript
{
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'reader',
        storage: localStorage,
        paths: ['searchHistory', 'recentReaders']
      }
    ]
  }
}
```

### circulation.js (会话持久化)

```javascript
{
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'circulation',
        storage: sessionStorage, // 使用sessionStorage
        paths: ['borrowCart', 'returnCart']
      }
    ]
  }
}
```

### system.js (配置持久化)

```javascript
{
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'system',
        storage: localStorage,
        paths: ['config'] // 只持久化系统配置
      }
    ]
  }
}
```

---

## 持久化策略设计理念

### 为什么不持久化所有数据？

1. **缓存数据不持久化**
   - 如 `categories`, `readerTypes`, `roles` 等
   - 理由：这些数据会变化，应该每次从服务器获取最新数据
   - 在内存中缓存一段时间（5-30分钟），但不跨会话

2. **临时状态不持久化**
   - 如 `currentReader`, `borrowOperation`, `returnOperation`
   - 理由：这些是操作过程中的临时状态，不应该跨会话保留

3. **用户历史持久化**
   - 如 `recentBooks`, `searchHistory`, `recentReaders`
   - 理由：这些是用户的操作历史，持久化可以提升体验

4. **购物车使用sessionStorage**
   - 如 `borrowCart`, `returnCart`
   - 理由：借还书是临时操作，关闭标签页应该清空，避免数据残留

### localStorage vs sessionStorage

| 特性     | localStorage   | sessionStorage   |
| -------- | -------------- | ---------------- |
| 生命周期 | 永久保存       | 关闭标签页清除   |
| 作用域   | 同源所有标签页 | 当前标签页       |
| 容量     | 5-10MB         | 5-10MB           |
| 用途     | 用户偏好、历史 | 临时状态、购物车 |

---

## 测试持久化

### 1. 测试localStorage持久化

```javascript
// 在浏览器控制台
import { useBookStore } from '@/stores'

// 添加数据
const bookStore = useBookStore()
bookStore.addSearchHistory('JavaScript')
bookStore.addSearchHistory('Vue3')

// 刷新页面
location.reload()

// 检查数据是否保留
const bookStore = useBookStore()
console.log(bookStore.searchHistory) // 应该包含 ['Vue3', 'JavaScript']
```

### 2. 测试sessionStorage持久化

```javascript
// 在浏览器控制台
import { useCirculationStore } from '@/stores'

// 添加数据
const circulationStore = useCirculationStore()
circulationStore.addToBorrowCart({
  id: 1,
  title: '测试图书',
  author: '测试作者'
})

// 刷新页面（同一标签页）
location.reload()

// 数据应该保留
const circulationStore = useCirculationStore()
console.log(circulationStore.borrowCart) // 应该包含测试图书

// 关闭标签页，重新打开
// 数据应该清空
```

### 3. 测试部分持久化

```javascript
import { useReaderStore } from '@/stores'

// 添加多种数据
const readerStore = useReaderStore()
readerStore.addSearchHistory('张三')
readerStore.loadReaderTypes() // 这会加载读者类型

// 刷新页面
location.reload()

// searchHistory应该保留
console.log(readerStore.searchHistory) // ['张三']

// readerTypes应该清空（因为没有持久化）
console.log(readerStore.readerTypes) // []
```

---

## 清除持久化数据

### 在代码中清除

```javascript
// 清除单个Store
localStorage.removeItem('book')
sessionStorage.removeItem('circulation')

// 清空所有
localStorage.clear()
sessionStorage.clear()
```

### 在浏览器中清除

1. 打开DevTools (F12)
2. Application → Storage
3. Local Storage / Session Storage
4. 右键 → Clear

---

## 常见问题

### Q1: 持久化失败，数据没有保存？

A: 检查以下几点：

1. 是否安装了 `pinia-plugin-persistedstate`？
2. 是否在main.js中注册了插件？
3. 浏览器是否禁用了localStorage？
4. 是否超出存储容量限制（5-10MB）？

### Q2: 如何禁用某个Store的持久化？

A: 修改Store配置，设置 `enabled: false`:

```javascript
{
  persist: {
    enabled: false // 禁用持久化
  }
}
```

### Q3: 如何清除所有持久化数据？

A: 在浏览器控制台执行：

```javascript
localStorage.clear()
sessionStorage.clear()
location.reload()
```

### Q4: 持久化数据过期了怎么办？

A: Store中的缓存数据有时间戳，会自动判断是否过期：

```javascript
// 示例：分类缓存过期检查
const isCategoriesValid = computed(() => {
  if (!categoriesLoadedAt.value) return false
  return Date.now() - categoriesLoadedAt.value < CACHE_DURATION
})

// 加载时检查过期
if (!isCategoriesValid.value) {
  await loadCategories() // 重新加载
}
```

### Q5: 如何在生产环境禁用持久化？

A: 不推荐在生产环境禁用，但如果需要：

```javascript
// main.js
const pinia = createPinia()

// 只在开发环境启用持久化
if (import.meta.env.DEV) {
  pinia.use(piniaPluginPersistedstate)
}

app.use(pinia)
```

---

## 推荐方案

**建议使用方案1 (pinia-plugin-persistedstate)**，因为：

1. 功能完善，支持多种配置
2. 社区活跃，维护良好
3. 性能优化，自动序列化
4. 与现有配置完全兼容

执行以下命令即可：

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npm install pinia-plugin-persistedstate
```

然后修改 `src/main.js`，添加插件注册即可。

---

## 下一步

1. 决定是否需要持久化功能
2. 如果需要，安装 `pinia-plugin-persistedstate`
3. 修改 `main.js` 注册插件
4. 测试持久化是否生效
5. 在生产环境测试

所有Store的配置已经就绪，只需要完成插件安装即可。
