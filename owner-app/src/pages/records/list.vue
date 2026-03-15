<template>
  <view class="records-container">
    <!-- 时间范围筛选 -->
    <view class="filter-section">
      <view class="date-picker">
        <text class="filter-label">开始日期</text>
        <picker mode="date" :value="startDate" @change="onStartDateChange">
          <view class="date-value">{{ startDate || '请选择' }}</view>
        </picker>
      </view>
      <view class="date-picker">
        <text class="filter-label">结束日期</text>
        <picker mode="date" :value="endDate" @change="onEndDateChange">
          <view class="date-value">{{ endDate || '请选择' }}</view>
        </picker>
      </view>
      <button class="search-btn" size="mini" @click="handleSearch">查询</button>
    </view>

    <!-- 记录列表 -->
    <view class="record-list">
      <view class="record-card" v-for="item in records" :key="item.id">
        <view class="record-header">
          <text class="car-number">{{ item.carNumber }}</text>
          <view class="status-tag" :class="item.status">
            {{ statusText(item.status) }}
          </view>
        </view>
        <view class="record-body">
          <view class="info-row">
            <text class="info-label">入场时间</text>
            <text class="info-value">{{ item.enterTime || '--' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">出场时间</text>
            <text class="info-value">{{ item.exitTime || '--' }}</text>
          </view>
          <view class="info-row" v-if="item.duration">
            <text class="info-label">停放时长</text>
            <text class="info-value">{{ formatDuration(item.duration) }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="records.length === 0 && !loading" class="empty-tip">
      <text>暂无入场记录</text>
    </view>

    <!-- 加载更多 -->
    <view v-if="hasMore" class="load-more" @click="loadMore">
      <text>{{ loading ? '加载中...' : '加载更多' }}</text>
    </view>
  </view>
</template>

<script>
import { getParkingRecords } from '@/api/record'

export default {
  data() {
    // 默认查询最近30天
    const now = new Date()
    const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
    return {
      startDate: this.formatDate(thirtyDaysAgo),
      endDate: this.formatDate(now),
      records: [],
      cursor: null,
      hasMore: false,
      loading: false,
      pageSize: 20
    }
  },
  onShow() {
    this.handleSearch()
  },
  methods: {
    formatDate(date) {
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
    },

    onStartDateChange(e) {
      this.startDate = e.detail.value
    },

    onEndDateChange(e) {
      this.endDate = e.detail.value
    },

    /** 查询（重置游标） */
    handleSearch() {
      this.records = []
      this.cursor = null
      this.hasMore = false
      this.loadData()
    },

    /** 加载更多 */
    loadMore() {
      if (this.loading || !this.hasMore) return
      this.loadData()
    },

    async loadData() {
      if (!this.startDate || !this.endDate) {
        uni.showToast({ title: '请选择时间范围', icon: 'none' })
        return
      }

      this.loading = true
      try {
        const params = {
          startTime: this.startDate + ' 00:00:00',
          endTime: this.endDate + ' 23:59:59',
          pageSize: this.pageSize
        }
        if (this.cursor) {
          params.cursor = this.cursor
        }

        const data = await getParkingRecords(params)
        const newRecords = data.records || []
        this.records = [...this.records, ...newRecords]
        this.cursor = data.nextCursor || null
        this.hasMore = data.hasMore || false
      } catch (e) {
        // 错误已在 request 中处理
      } finally {
        this.loading = false
      }
    },

    statusText(status) {
      const map = {
        entered: '在场',
        exited: '已出场',
        exit_exception: '异常出场'
      }
      return map[status] || status
    },

    /** 格式化停放时长（分钟 → 可读文本） */
    formatDuration(minutes) {
      if (!minutes || minutes <= 0) return '--'
      const h = Math.floor(minutes / 60)
      const m = minutes % 60
      if (h > 0) {
        return m > 0 ? `${h}小时${m}分钟` : `${h}小时`
      }
      return `${m}分钟`
    }
  }
}
</script>

<style scoped>
.records-container {
  min-height: 100vh;
  padding: 24rpx;
  background: #f5f5f5;
}
.filter-section {
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}
.date-picker {
  flex: 1;
}
.filter-label {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}
.date-value {
  height: 64rpx;
  line-height: 64rpx;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  padding: 0 16rpx;
  font-size: 26rpx;
  color: #333;
}
.search-btn {
  background: #1890ff;
  color: #fff;
  font-size: 26rpx;
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 24rpx;
  border-radius: 8rpx;
}
.record-list {
  margin-bottom: 24rpx;
}
.record-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}
.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.car-number {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}
.status-tag {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}
.status-tag.entered {
  background: #e6f7ff;
  color: #1890ff;
}
.status-tag.exited {
  background: #f6ffed;
  color: #52c41a;
}
.status-tag.exit_exception {
  background: #fff1f0;
  color: #ff4d4f;
}
.record-body {
  padding-left: 4rpx;
}
.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8rpx;
}
.info-label {
  font-size: 26rpx;
  color: #999;
}
.info-value {
  font-size: 26rpx;
  color: #333;
}
.empty-tip {
  text-align: center;
  padding: 80rpx 0;
  color: #999;
  font-size: 28rpx;
}
.load-more {
  text-align: center;
  padding: 24rpx 0;
  color: #1890ff;
  font-size: 28rpx;
}
</style>
