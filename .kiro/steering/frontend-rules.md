---
inclusion: fileMatch
fileMatchPattern: "admin-portal/**/*.vue"
---

# Admin Portal 前端代码规范

## 组件库版本

- 使用 `ant-design-vue@3.x`（当前 3.2.20），API 以 3.x 文档为准。

## a-modal 弹窗显隐控制

- 必须使用 `v-model:visible` 控制 `a-modal` 的显示/隐藏。
- 禁止使用 `v-model:open` 或 `:open`，该属性属于 `ant-design-vue@4.x` API，在 3.x 中不生效，会导致弹窗无法弹出。

```vue
<!-- ✅ 正确 -->
<a-modal v-model:visible="modalVisible" title="标题">...</a-modal>

<!-- ❌ 错误（4.x API，3.x 不支持） -->
<a-modal v-model:open="modalVisible" title="标题">...</a-modal>
```
