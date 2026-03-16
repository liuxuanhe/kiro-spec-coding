<template>
  <div>
    <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center">
      <h3 style="margin: 0">小区管理</h3>
      <a-button type="primary" @click="showCreateModal">
        <template #icon><PlusOutlined /></template>
        创建小区
      </a-button>
    </div>

    <!-- 小区列表表格 -->
    <a-table
      :columns="columns"
      :data-source="communityList"
      :loading="loading"
      row-key="id"
      :pagination="{ pageSize: 10 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'active' ? 'green' : 'red'">
            {{ record.status === 'active' ? '正常' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
        </template>
      </template>
    </a-table>

    <!-- 创建/编辑小区弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑小区' : '创建小区'"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      @cancel="resetForm"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item label="小区名称" name="communityName">
          <a-input v-model:value="formState.communityName" placeholder="请输入小区名称" />
        </a-form-item>
        <a-form-item label="小区编码" name="communityCode">
          <a-input v-model:value="formState.communityCode" placeholder="请输入小区编码" :disabled="isEdit" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="省份" name="province">
              <a-input v-model:value="formState.province" placeholder="省份" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="城市" name="city">
              <a-input v-model:value="formState.city" placeholder="城市" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="区县" name="district">
              <a-input v-model:value="formState.district" placeholder="区县" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="详细地址" name="address">
          <a-input v-model:value="formState.address" placeholder="请输入详细地址" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="联系人" name="contactPerson">
              <a-input v-model:value="formState.contactPerson" placeholder="联系人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="contactPhone">
              <a-input v-model:value="formState.contactPhone" placeholder="联系电话" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getCommunityList, createCommunity, updateCommunity } from '@/api/community'

/** 表格列定义 */
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '小区名称', dataIndex: 'communityName', key: 'communityName' },
  { title: '小区编码', dataIndex: 'communityCode', key: 'communityCode' },
  { title: '省份', dataIndex: 'province', key: 'province' },
  { title: '城市', dataIndex: 'city', key: 'city' },
  { title: '区县', dataIndex: 'district', key: 'district' },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson' },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone' },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 80 }
]

const loading = ref(false)
const communityList = ref([])
const modalVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const formState = reactive({
  communityName: '',
  communityCode: '',
  province: '',
  city: '',
  district: '',
  address: '',
  contactPerson: '',
  contactPhone: ''
})

const formRules = {
  communityName: [{ required: true, message: '请输入小区名称' }],
  communityCode: [{ required: true, message: '请输入小区编码' }],
  province: [{ required: true, message: '请输入省份' }],
  city: [{ required: true, message: '请输入城市' }],
  district: [{ required: true, message: '请输入区县' }],
  address: [{ required: true, message: '请输入详细地址' }]
}

/** 加载小区列表 */
async function loadList() {
  loading.value = true
  try {
    const data = await getCommunityList()
    communityList.value = Array.isArray(data) ? data : []
  } catch (err) {
    // 错误已在 request.js 拦截器中统一提示
  } finally {
    loading.value = false
  }
}

/** 显示创建弹窗 */
function showCreateModal() {
  isEdit.value = false
  editId.value = null
  resetForm()
  modalVisible.value = true
}

/** 显示编辑弹窗 */
function showEditModal(record) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formState, {
    communityName: record.communityName,
    communityCode: record.communityCode,
    province: record.province,
    city: record.city,
    district: record.district,
    address: record.address,
    contactPerson: record.contactPerson || '',
    contactPhone: record.contactPhone || ''
  })
  modalVisible.value = true
}

/** 重置表单 */
function resetForm() {
  Object.assign(formState, {
    communityName: '', communityCode: '', province: '', city: '',
    district: '', address: '', contactPerson: '', contactPhone: ''
  })
  formRef.value?.resetFields()
}

/** 提交表单 */
async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateCommunity(editId.value, { ...formState })
      message.success('小区信息更新成功')
    } else {
      await createCommunity({ ...formState })
      message.success('小区创建成功')
    }
    modalVisible.value = false
    resetForm()
    loadList()
  } catch (err) {
    // 错误已在 request.js 拦截器中统一提示
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>
