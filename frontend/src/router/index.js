import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

import PublicBookingPage from '../pages/public/PublicBookingPage.vue'
import BookingSuccessPage from '../pages/public/BookingSuccessPage.vue'
import CancelBookingPage from '../pages/public/CancelBookingPage.vue'
import LoginPage from '../pages/admin/LoginPage.vue'
import CalendarDashboardPage from '../pages/admin/CalendarDashboardPage.vue'
import ProfessionalsPage from '../pages/admin/ProfessionalsPage.vue'
import ServicesPage from '../pages/admin/ServicesPage.vue'
import TenantsPage from '../pages/superadmin/TenantsPage.vue'

const routes = [
  {
    path: '/',
    redirect: () => {
      const auth = useAuthStore()
      if (auth.isAuthenticated) {
        return auth.isSuperAdmin ? '/superadmin/tenants' : '/admin/agenda'
      }
      return '/b/barbearia-vintage'
    }
  },
  {
    path: '/b/:slug',
    name: 'public-booking',
    component: PublicBookingPage
  },
  {
    path: '/b/:slug/sucesso',
    name: 'booking-success',
    component: BookingSuccessPage
  },
  {
    path: '/cancelar/:token',
    name: 'cancel-booking',
    component: CancelBookingPage
  },
  {
    path: '/login',
    name: 'login',
    component: LoginPage
  },
  {
    path: '/admin/agenda',
    name: 'admin-calendar',
    component: CalendarDashboardPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/profissionais',
    name: 'admin-professionals',
    component: ProfessionalsPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/servicos',
    name: 'admin-services',
    component: ServicesPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/superadmin/tenants',
    name: 'superadmin-tenants',
    component: TenantsPage,
    meta: { requiresAuth: true, requiresSuperAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (to.meta.requiresSuperAdmin && !auth.isSuperAdmin) {
    return next({ path: '/admin/agenda' })
  }

  next()
})

export default router
