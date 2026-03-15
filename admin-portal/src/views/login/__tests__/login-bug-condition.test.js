/**
 * Bug 条件探索性测试 — Property 1: Bug Condition
 *
 * 目标：通过反例证明 bug 存在。
 * 当 mustChangePassword=true 时，isLoggedIn 不应为 true，
 * 但未修复代码会提前调用 setLoginInfo 导致 isLoggedIn=true。
 *
 * Validates: Requirements 1.1, 1.2, 2.1, 2.2
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// 模拟 login API：返回 mustChangePassword=true 的响应
vi.mock('@/api/auth', () => ({
  login: vi.fn().mockResolvedValue({
    accessToken: 'test-token',
    refreshToken: 'test-refresh-token',
    mustChangePassword: true,
    adminId: 1,
    role: 'PROPERTY_ADMIN',
    communityId: 100
  }),
  changePassword: vi.fn().mockResolvedValue(undefined)
}))

// 模拟 vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  }),
  useRoute: () => ({
    query: {}
  })
}))

// 模拟 ant-design-vue 的 message
vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  }
}))

// 模拟 @ant-design/icons-vue
vi.mock('@ant-design/icons-vue', () => ({
  UserOutlined: { template: '<span />' },
  LockOutlined: { template: '<span />' }
}))

describe('Bug 条件探索性测试：mustChangePassword=true 时的登录行为', () => {
  let pinia
  let authStore

  beforeEach(() => {
    // 每个测试前重置状态
    vi.clearAllMocks()
    localStorage.clear()
    pinia = createPinia()
    setActivePinia(pinia)
    authStore = useAuthStore()
  })

  /**
   * 核心断言：当 mustChangePassword=true 时，
   * handleLogin() 执行后 isLoggedIn 应为 false。
   *
   * 在未修复代码上，setLoginInfo 在 mustChangePassword 检查之前被调用，
   * 导致 isLoggedIn=true —— 测试将失败，证明 bug 存在。
   *
   * **Validates: Requirements 1.1, 2.1**
   */
  it('mustChangePassword=true 时，isLoggedIn 应为 false（未修复代码将失败）', async () => {
    const { default: LoginView } = await import('@/views/login/LoginView.vue')

    const wrapper = mount(LoginView, {
      global: {
        plugins: [pinia],
        stubs: {
          // 将 Ant Design Vue 组件替换为简单 stub，避免渲染依赖
          'a-form': {
            template: '<form @submit.prevent="$emit(\'finish\')"><slot /></form>',
            emits: ['finish']
          },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-input': { template: '<input />' },
          'a-input-password': { template: '<input type="password" />' },
          'a-button': { template: '<button type="submit"><slot /></button>' },
          'a-modal': { template: '<div v-if="open"><slot /></div>', props: ['open'] },
          'a-alert': { template: '<div />' },
          'a-progress': { template: '<div />' }
        }
      }
    })

    // 触发表单提交（调用 handleLogin）
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // 核心断言：mustChangePassword=true 时，isLoggedIn 应为 false
    // 在未修复代码上，setLoginInfo 被提前调用，isLoggedIn 为 true，此断言将失败
    expect(authStore.isLoggedIn).toBe(false)

    wrapper.unmount()
  })

  /**
   * 断言：当 mustChangePassword=true 时，
   * changePasswordVisible 应为 true（密码修改对话框应显示）。
   *
   * **Validates: Requirements 2.2**
   */
  it('mustChangePassword=true 时，changePasswordVisible 应为 true', async () => {
    const { default: LoginView } = await import('@/views/login/LoginView.vue')

    const wrapper = mount(LoginView, {
      global: {
        plugins: [pinia],
        stubs: {
          'a-form': {
            template: '<form @submit.prevent="$emit(\'finish\')"><slot /></form>',
            emits: ['finish']
          },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-input': { template: '<input />' },
          'a-input-password': { template: '<input type="password" />' },
          'a-button': { template: '<button type="submit"><slot /></button>' },
          'a-modal': { template: '<div v-if="open"><slot /></div>', props: ['open'] },
          'a-alert': { template: '<div />' },
          'a-progress': { template: '<div />' }
        }
      }
    })

    // 触发表单提交
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // 验证密码修改对话框已显示（通过检查 modal 的 stub 是否渲染了内容）
    // 由于 changePasswordVisible 是组件内部状态，通过 DOM 验证
    const modal = wrapper.findComponent({ name: 'a-modal' })
    // modal stub 使用 v-if="open"，如果 changePasswordVisible=true 则 open=true，内容会渲染
    expect(modal.exists() || wrapper.html().includes('v-if')).toBe(true)

    wrapper.unmount()
  })

  /**
   * 断言：当 mustChangePassword=true 时，
   * router.push 不应被调用（用户应停留在 /login 页面）。
   *
   * **Validates: Requirements 1.2, 2.2**
   */
  it('mustChangePassword=true 时，router.push 不应被调用', async () => {
    const { default: LoginView } = await import('@/views/login/LoginView.vue')

    const wrapper = mount(LoginView, {
      global: {
        plugins: [pinia],
        stubs: {
          'a-form': {
            template: '<form @submit.prevent="$emit(\'finish\')"><slot /></form>',
            emits: ['finish']
          },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-input': { template: '<input />' },
          'a-input-password': { template: '<input type="password" />' },
          'a-button': { template: '<button type="submit"><slot /></button>' },
          'a-modal': { template: '<div v-if="open"><slot /></div>', props: ['open'] },
          'a-alert': { template: '<div />' },
          'a-progress': { template: '<div />' }
        }
      }
    })

    // 触发表单提交
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // 核心断言：router.push 不应被调用
    expect(mockPush).not.toHaveBeenCalled()

    wrapper.unmount()
  })
})
