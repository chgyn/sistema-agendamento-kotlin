import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
})

// Interceptor para injetar o JWT automaticamente
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, (error) => {
  return Promise.reject(error)
})

// Interceptor para redirecionar em caso de 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!window.location.pathname.startsWith('/login') && !window.location.pathname.startsWith('/b/')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api

// --- Endpoints da API ---
export const authService = {
  login: (credentials) => api.post('/auth/login', credentials),
  me: () => api.get('/auth/me')
}

export const publicBookingService = {
  getTenantDetails: (slug) => api.get(`/public/tenants/${slug}`),
  getAvailability: (slug, serviceId, date, professionalId = null) => {
    const params = { serviceId, date }
    if (professionalId) params.professionalId = professionalId
    return api.get(`/public/tenants/${slug}/availability`, { params })
  },
  createAppointment: (slug, data) => api.post(`/public/tenants/${slug}/appointments`, data),
  getAppointmentByToken: (token) => api.get(`/public/appointments/${token}`),
  cancelAppointment: (token) => api.post(`/public/appointments/${token}/cancel`)
}

export const adminService = {
  getAppointments: (date) => api.get('/admin/appointments', { params: { date } }),
  updateStatus: (id, status) => api.patch(`/admin/appointments/${id}/status`, { status }),
  getProfessionals: () => api.get('/admin/professionals'),
  createProfessional: (data) => api.post('/admin/professionals', data),
  getProfessionalSchedule: (id) => api.get(`/admin/professionals/${id}/schedule`),
  setProfessionalSchedule: (id, hours) => api.put(`/admin/professionals/${id}/schedule`, { hours }),
  getServices: () => api.get('/admin/services'),
  createService: (data) => api.post('/admin/services', data),
  getTenants: () => api.get('/admin/tenants'),
  createTenant: (data) => api.post('/admin/tenants', data)
}
