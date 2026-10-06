import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService } from '../services/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const tenantSlug = ref(localStorage.getItem('tenantSlug') || null)

  const isAuthenticated = computed(() => !!token.value)
  const isSuperAdmin = computed(() => user.value?.role === 'SUPER_ADMIN')
  const isTenantAdmin = computed(() => user.value?.role === 'TENANT_ADMIN')
  const isOperator = computed(() => user.value?.role === 'OPERATOR')

  async function login(email, password) {
    const response = await authService.login({ email, password })
    token.value = response.data.token
    user.value = response.data.user
    tenantSlug.value = response.data.tenantSlug

    localStorage.setItem('token', token.value)
    localStorage.setItem('user', JSON.stringify(user.value))
    if (tenantSlug.value) {
      localStorage.setItem('tenantSlug', tenantSlug.value)
    }
    return response.data
  }

  function logout() {
    token.value = null
    user.value = null
    tenantSlug.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('tenantSlug')
  }

  return {
    token,
    user,
    tenantSlug,
    isAuthenticated,
    isSuperAdmin,
    isTenantAdmin,
    isOperator,
    login,
    logout
  }
})
