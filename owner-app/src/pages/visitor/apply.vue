<template>
  <view class="visitor-apply-container">
    <!-- 月度配额展示 -->
    <view class="quota-card" v-if="quota">
      <text class="quota-title">本月 Visitor 配额</text>
      <view class="quota-bar-wrapper">
        <view class="quota-bar" :style="{ width: quotaPercent + '%' }"></view>
      </view>
      <view class="quota-info">
        <text>已使用 {{ quota.usedHours || 0 }} 小时</text>
        <text>剩余 {{ quota.remainingHours || 0 }} 小时</text>
      </view>
    </view>

    <!-- 选择车牌 -->
    <view class="section-title">选择车牌</view>
    <view class="vehicle-list">
      <view
        v-for="item in normalVehicles"
        :key="item.id"
        class="vehicle-item"
        :class="{ selected: selectedVehicleId === item.id }"
        @click="selectedVehicleId = item.id"
      >
        <text class="plate">{{ item.carNumber }}</text>
        <text v-if="selectedVehicleId === item.id" class="check">✓</text>
      </view>
    </view>

    <view v-if="normalVehicles.length === 0 && !loading" class="empty-tip">
      <text>暂无可申请的普通车辆</text>
    </view>

    <!-- 申请原因 -->
    <view class="input-group">
      <text class="label">申请原因（选填）</text>
      <textarea
        v-model="reason"
        placeholder="请输入申请原因"
        class="textarea"
        maxlength="200"
      />
    </view>

    <button
      class="submit-btn"
      :disabled="!selectedVehicleId"
      :loading="submitting"
      @click="handleApply"
    >
      提交申请
    </button>
  </view>
</template>

<script>
import { getVehicleList } from '@/api/vehicle'
import { applyVisitor, getQuota } from '@/api/visitor'

export default {
  data() {
    return {
      vehicleList: [],
      normalVehicles: [],
      selectedVehicleId: null,
      reason: '',
      quota: null,
      loading: false,
      submitting: false
    }
  },
  computed: {
    quotaPercent() {
      if (!this.quota || !this.quota.totalHours) return 0
      return Math.min(100, (this.quota.usedHours / this.quota.totalHours) * 100)
    }
  },
  onShow() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const [vehicles, quota] = await Promise.all([
          getVehicleList(),
          getQuota()
        ])
        this.vehicleList = vehicles || []
        // 只显示普通车辆（非 Primary、非禁用、非删除）
        this.normalVehicles = this.vehicleList.filter(
          v => v.status === 'normal'
        )
        this.quota = quota
      } catch (e) {
        // 错误已在 request 中处理
      } finally {
        this.loading = false
      }
    },

    async handleApply() {
      if (!this.selectedVehicleId) return

      const selected = this.vehicleList.find(v => v.id === this.selectedVehicleId)
      this.submitting = true
      try {
        await applyVisitor({
          vehicleId: this.selectedVehicleId,
          carNumber: selected.carNumber,
          reason: this.reason
        })
        uni.showToast({ title: '申请已提交', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 500)
      } catch (e) {
        // 错误已在 request 中处理
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.visitor-apply-container {
  min-height: 100vh;
  padding: 24rpx;
  background: #f5f5f5;
}
.quota-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}
.quota-title {
  display: block;
  font-size: 28rpx;
  color: #333;
  font-weight: bold;
  margin-bottom: 20rpx;
}
.quota-bar-wrapper {
  height: 16rpx;
  background: #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
  margin-bottom: 12rpx;
}
.quota-bar {
  height: 100%;
  background: #1890ff;
  border-radius: 8rpx;
  transition: width 0.3s;
}
.quota-info {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: #999;
}
.section-title {
  font-size: 28rpx;
  color: #333;
  font-weight: bold;
  margin-bottom: 16rpx;
}
.vehicle-list {
  margin-bottom: 24rpx;
}
.vehicle-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 12rpx;
  padding: 28rpx 32rpx;
  margin-bottom: 12rpx;
  border: 2rpx solid transparent;
}
.vehicle-item.selected {
  border-color: #1890ff;
  background: #f0f8ff;
}
.plate {
  font-size: 30rpx;
  color: #333;
}
.check {
  font-size: 32rpx;
  color: #1890ff;
  font-weight: bold;
}
.empty-tip {
  text-align: center;
  padding: 40rpx 0;
  color: #999;
  font-size: 28rpx;
}
.input-group {
  margin-bottom: 24rpx;
}
.label {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 12rpx;
}
.textarea {
  width: 100%;
  height: 160rpx;
  border: 1rpx solid #ddd;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  background: #fff;
}
.submit-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  background: #1890ff;
  color: #fff;
  font-size: 32rpx;
  border-radius: 12rpx;
  margin-top: 20rpx;
}
.submit-btn[disabled] {
  background: #ccc;
}
</style>
