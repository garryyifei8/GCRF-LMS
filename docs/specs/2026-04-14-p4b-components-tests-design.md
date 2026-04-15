# P4B: 11 个 Vue 组件单元测试设计

**日期：** 2026-04-14
**状态：** Approved
**优先级：** P4B（前端组件层测试）
**背景：** P4A 完成了 stores + utils 的单测。P4B 覆盖 `web-admin/src/components/` 下的 11 个 Vue 组件。

---

## 目标

为 11 个 Vue 组件创建单元测试，覆盖 props、events、v-model、slots、条件渲染逻辑。

---

## 测试风格

### 标准模板

```javascript
import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import ElementPlus from "element-plus";
import Button from "@/components/ui/Button/Button.vue";

describe("Button", () => {
  const factory = (props = {}, slots = {}) =>
    mount(Button, {
      props,
      slots,
      global: { plugins: [ElementPlus] },
    });

  it("click_whenEnabled_shouldEmitClick", async () => {
    const wrapper = factory();
    await wrapper.trigger("click");
    expect(wrapper.emitted("click")).toHaveLength(1);
  });
});
```

### 通用约定

- 用 `@vue/test-utils` 的 `mount()` 做完整渲染
- `global.plugins: [ElementPlus]` 注册 Element Plus（避免每次 stub）
- Mock 浏览器 API（FileReader, getUserMedia）用 `global` 或 `Object.defineProperty`
- v-model 组件用 `props: { modelValue }` + 断言 `wrapper.emitted('update:modelValue')`

---

## 11 个测试文件

### 1. Button.test.js（~6 tests）

**路径：** `web-admin/src/components/ui/Button/__tests__/Button.test.js`

| #   | 测试                                                |
| --- | --------------------------------------------------- |
| 1   | `renders_withDefaultProps_shouldHavePrimaryVariant` |
| 2   | `renders_withLoading_shouldShowSpinner`             |
| 3   | `click_whenEnabled_shouldEmitClick`                 |
| 4   | `click_whenDisabled_shouldNotEmit`                  |
| 5   | `click_whenLoading_shouldNotEmit`                   |
| 6   | `renders_withIconRight_shouldApplyCorrectClass`     |

### 2. Card.test.js（~5 tests）

**路径：** `web-admin/src/components/ui/Card/__tests__/Card.test.js`

| #   | 测试                                                 |
| --- | ---------------------------------------------------- |
| 1   | `renders_withDefaultVariant_shouldApplyDefaultClass` |
| 2   | `renders_withHeaderSlot_shouldShowHeader`            |
| 3   | `renders_withFooterSlot_shouldShowFooter`            |
| 4   | `click_whenClickable_shouldEmitClick`                |
| 5   | `click_whenNotClickable_shouldNotEmit`               |

### 3. StatCard.test.js（~5 tests）

**路径：** `web-admin/src/components/ui/Card/__tests__/StatCard.test.js`

| #   | 测试                                              |
| --- | ------------------------------------------------- |
| 1   | `renders_requiredProps_shouldShowTitleAndValue`   |
| 2   | `renders_trendUp_shouldShowArrowUpIcon`           |
| 3   | `renders_trendDown_shouldShowArrowDownIcon`       |
| 4   | `renders_trendFlat_shouldShowMinusIcon`           |
| 5   | `renders_colorVariants_shouldApplyCorrectClasses` |

### 4. AICard.test.js（~5 tests）

**路径：** `web-admin/src/components/ui/Card/__tests__/AICard.test.js`

| #   | 测试                                     |
| --- | ---------------------------------------- |
| 1   | `renders_requiredProps_shouldShowTitle`  |
| 2   | `renders_withTags_shouldRenderAllTags`   |
| 3   | `renders_withScore_shouldShowScore`      |
| 4   | `click_shouldEmitClickEvent`             |
| 5   | `renders_withoutBadge_shouldHideAIBadge` |

### 5. Input.test.js（~8 tests）

**路径：** `web-admin/src/components/ui/Input/__tests__/Input.test.js`

| #   | 测试                                             |
| --- | ------------------------------------------------ |
| 1   | `vModel_typing_shouldEmitUpdateModelValue`       |
| 2   | `renders_withErrorStatus_shouldApplyErrorStyles` |
| 3   | `renders_withErrorText_shouldShowErrorMessage`   |
| 4   | `clear_whenClearableAndValue_shouldEmitClear`    |
| 5   | `renders_withShowCount_shouldShowCharacterCount` |
| 6   | `renders_withMaxlength_shouldLimitInput`         |
| 7   | `focus_shouldEmitFocus`                          |
| 8   | `renders_whenDisabled_shouldDisableInput`        |

### 6. SkeletonLoader.test.js（~5 tests）

**路径：** `web-admin/src/components/Loading/__tests__/SkeletonLoader.test.js`

| #   | 测试                                         |
| --- | -------------------------------------------- |
| 1   | `renders_typeTable_shouldShowTableTemplate`  |
| 2   | `renders_typeCard_shouldShowCardTemplate`    |
| 3   | `renders_withRows_shouldRenderCorrectCount`  |
| 4   | `renders_animated_shouldApplyAnimationClass` |
| 5   | `renders_customType_shouldRenderSlot`        |

### 7. ButtonLoading.test.js（~3 tests）

**路径：** `web-admin/src/components/Loading/__tests__/ButtonLoading.test.js`

| #   | 测试                                              |
| --- | ------------------------------------------------- |
| 1   | `renders_withDefaultSize_shouldApplyDefaultClass` |
| 2   | `renders_withLargeSize_shouldApplyLargeClass`     |
| 3   | `renders_withText_shouldShowText`                 |

### 8. PageLoading.test.js（~4 tests）

**路径：** `web-admin/src/components/Loading/__tests__/PageLoading.test.js`

| #   | 测试                                            |
| --- | ----------------------------------------------- |
| 1   | `renders_fullscreen_shouldApplyFullscreenClass` |
| 2   | `renders_withMask_shouldShowMask`               |
| 3   | `renders_withCustomText_shouldDisplayText`      |
| 4   | `renders_withCustomIcon_shouldRenderIcon`       |

### 9. AvatarUpload.test.js（~7 tests）

**路径：** `web-admin/src/components/__tests__/AvatarUpload.test.js`

**Mock：** `FileReader`, `URL.createObjectURL`, `navigator.mediaDevices`

| #   | 测试                                              |
| --- | ------------------------------------------------- |
| 1   | `renders_withoutImage_shouldShowPlaceholder`      |
| 2   | `renders_withModelValue_shouldShowPreview`        |
| 3   | `uploadFile_validFile_shouldEmitUpdateModelValue` |
| 4   | `uploadFile_oversizedFile_shouldEmitError`        |
| 5   | `uploadFile_invalidType_shouldEmitError`          |
| 6   | `clear_shouldEmitUpdateModelValueWithEmpty`       |
| 7   | `renders_withCircleShape_shouldApplyCircleClass`  |

### 10. FaceRecognition.test.js（~5 tests）

**路径：** `web-admin/src/components/__tests__/FaceRecognition.test.js`

**Mock：** `navigator.mediaDevices.getUserMedia`, `HTMLCanvasElement`

| #   | 测试                                                |
| --- | --------------------------------------------------- |
| 1   | `mount_shouldInitializeWithoutError`                |
| 2   | `startCapture_whenNotSupported_shouldShowError`     |
| 3   | `startCapture_whenPermissionDenied_shouldShowError` |
| 4   | `startCapture_whenSuccess_shouldShowVideo`          |
| 5   | `capturePhoto_shouldEmitCaptureEvent`               |

### 11. LibraryUIComponents.test.js（~3 tests）

**路径：** `web-admin/src/components/__tests__/LibraryUIComponents.test.js`

**Mock：** `echarts`

| #   | 测试                                                          |
| --- | ------------------------------------------------------------- |
| 1   | `mount_shouldRenderWithoutError`                              |
| 2   | `mount_shouldInitializeCharts` — verify `echarts.init` called |
| 3   | `renders_statsCards_shouldShowAllStats`                       |

---

## 复杂组件 Mock 策略

### AvatarUpload

```javascript
// global setup
global.FileReader = class {
  readAsDataURL(file) {
    setTimeout(
      () => this.onload({ target: { result: "data:image/png;base64,MOCK" } }),
      0,
    );
  }
};
global.URL.createObjectURL = vi.fn(() => "blob:mock");
```

### FaceRecognition

```javascript
Object.defineProperty(navigator, "mediaDevices", {
  writable: true,
  value: {
    getUserMedia: vi.fn().mockResolvedValue({
      getTracks: () => [{ stop: vi.fn() }],
    }),
  },
});
HTMLCanvasElement.prototype.getContext = vi.fn(() => ({
  drawImage: vi.fn(),
  fillRect: vi.fn(),
}));
HTMLCanvasElement.prototype.toDataURL = vi.fn(
  () => "data:image/png;base64,MOCK",
);
```

### LibraryUIComponents

```javascript
vi.mock("echarts", () => ({
  init: vi.fn(() => ({
    setOption: vi.fn(),
    resize: vi.fn(),
    dispose: vi.fn(),
  })),
}));
```

---

## 验证

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npx vitest run src/components 2>&1 | tail -10
```

**预期：** ~56 个测试通过。

---

## 修改文件清单

### 新建（11 个测试文件）

见上文表格。全部为纯新增，无源码修改。

### 无依赖新增

`element-plus`, `@vue/test-utils`, `jsdom` 已在 P2A/P4A 安装。
