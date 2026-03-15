<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header style="background: #001529; padding: 0 24px; display: flex; align-items: center; justify-content: space-between">
      <div style="color: #fff; font-size: 18px; font-weight: bold">
        停车场管理后台
      </div>
      <a-button type="link" style="color: #fff" @click="handleLogout">
        退出登录
      </a-button>
    </a-layout-header>
    <a-layout-content style="padding: 24px">
      <router-view />
    </a-layout-content>
  </a-layout>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

/** 退出登录 */
function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
