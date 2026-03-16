<template>
  <div class="vehicle-list-container">
    <h2 style="margin-bottom: 16px">车辆列表</h2>

    <!-- 搜索栏 -->
    <div class="search-bar" style="margin-bottom: 16px">
      <a-space>
        <a-input
          v-model:value="searchForm.houseNo"
          placeholder="房屋号"
          allow-clear
          style="width: 160px"
        />
        <a-input
          v-model:value="searchForm.carNumber"
          placeholder="车牌号"
          allow-clear
          style="width: 160px"
        />
        <a-button type="primary" @click="handleSearch">搜索</a-button>
        <a-button @click="handleReset">重置</a-button>
      </a-space>
    </div>

    <!-- 车辆列表表格 -->
    <a-table
      :columns="columns"
      :data-source="vehicleList"
      :loading="tableLoading"
      :pagination="pagination"
      row-key="vehicleId"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- 品牌型号列 -->
        <template v-if="column.dataIndex === 'brandModel'">
          {{ [record.carBrand, record.carModel].filter(Boolean).join(' ') || '—' }}
        </template>

        <!-- 业主手机号列：脱敏显示 -->
        <template v-else-if="column.dataIndex === 'ownerPhone'">
          {{ maskPhone(record.ownerPhone) }}
        </template>

        <!-- 状态列 -->
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag v-if="record.status === 'primary'" color="green">Primary</a-tag>
          <a-tag v-else color="blue">普通</a-tag>
        </template>

        <!-- 操作列 -->
        <template v-else-if="column.dataIndex === 'action'">
          <a-button type="link" size="small" @click="openDetail(record)">
            查看详情
          </a-button>
        </template>
      </template>
    </a-table>

    <!-- 车辆详情抽屉 -->
    <a-drawer
      title="车辆详情"
      :visible="drawerVisible"
      :width="400"
      @close="drawerVisible = false"
    >
      <a-descriptions :column="1" bordered size="small" v-if="currentVehicle">
        <a-descriptions-item label="车牌号">{{ currentVehicle.carNumber }}</a-descriptions-item>
        <a-descriptions-item label="品牌">{{ currentVehicle.carBrand || '—' }}</a-descriptions-item>
        <a-descriptions-item label="型号">{{ currentVehicle.carModel || '—' }}</a-descriptions-item>
        <a-descriptions-item label="颜色">{{ currentVehicle.carColor || '—' }}</a-descriptions-item>
        <a-descriptions-item label="房屋号">{{ currentVehicle.houseNo }}</a-descriptions-item>
        <a-descriptions-item label="业主手机号">{{ maskPhone(currentVehicle.ownerPhone) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag v-if="currentVehicle.status === 'primary'" color="green">Primary</a-tag>
          <a-tag v-else color="blue">普通</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="注册时间">{{ currentVehicle.createTime }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getVehicleList } from '@/api/vehicle'

const authStore = useAuthStore()

// ========== 表格列定义 ==========
const columns = [
  { title: '车牌号', dataIndex: 'carNumber', width: 140 },
  { title: '品牌型号', dataIndex: 'brandModel', width: 140 },
  { title: '颜色', dataIndex: 'carColor', width: 80 },
  { title: '房屋号', dataIndex: 'houseNo', width: 120 },
  { title: '业主手机号', dataIndex: 'ownerPhone', width: 140 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '注册时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', dataIndex: 'action', width: 100, fixed: 'right' }
]

// ========== 搜索表单 ==========
const searchForm = reactive({
  houseNo: '',
  carNumber: ''
})

// ========== 数据与加载状态 ==========
const vehicleList = ref([])
const tableLoading = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

// ========== 详情抽屉 ==========
const drawerVisible = ref(false)
const currentVehicle = ref(null)

// ========== 手机号脱敏 ==========
function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || '—'
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

// ========== 数据加载 ==========
async function fetchVehicleList() {
  tableLoading.value = true
  try {
    const params = {
      communityId: authStore.communityId,
      page: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.houseNo) {
      params.houseNo = searchForm.houseNo
    }
    if (searchForm.carNumber) {
      params.carNumber = searchForm.carNumber
    }
    const data = await getVehicleList(params)
    vehicleList.value = data.records || []
    pagination.total = data.total || 0
  } catch (err) {
    // 错误已在 request.js 拦截器中统一提示
  } finally {
    tableLoading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pagination.current = 1
  fetchVehicleList()
}

/** 重置搜索条件 */
function handleReset() {
  searchForm.houseNo = ''
  searchForm.carNumber = ''
  pagination.current = 1
  fetchVehicleList()
}

/** 表格分页变化 */
function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchVehicleList()
}

/** 打开详情抽屉 */
function openDetail(record) {
  currentVehicle.value = record
  drawerVisible.value = true
}

// ========== 初始化加载 ==========
fetchVehicleList()
</script>

<style scoped>
.vehicle-list-container {
  padding: 0;
}
</style>
