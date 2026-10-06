<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
    <!-- Topo: Título e Controles de Data -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-extrabold text-white">Quadro da Agenda</h1>
        <p class="text-xs text-slate-400 mt-1">Gerenciamento diário de atendimentos e status</p>
      </div>

      <div class="flex flex-wrap items-center gap-2.5">
        <button
          @click="setDateToday"
          class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors"
        >
          Hoje
        </button>
        <input
          type="date"
          v-model="selectedDate"
          @change="loadAppointments"
          class="bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-amber-500 cursor-pointer"
        />
        <button
          @click="openNewModal"
          class="px-4 py-1.5 rounded-lg text-xs font-bold bg-amber-400 hover:bg-amber-300 text-slate-950 transition-all shadow-md shadow-amber-400/20"
        >
          + Novo Agendamento
        </button>
      </div>
    </div>

    <!-- Estatísticas Rápidas do Dia -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
      <div class="glass-card rounded-xl p-4 border border-slate-800">
        <span class="text-xs text-slate-400">Total do Dia</span>
        <p class="text-xl font-extrabold text-slate-100 mt-1">{{ appointments.length }}</p>
      </div>
      <div class="glass-card rounded-xl p-4 border border-slate-800">
        <span class="text-xs text-amber-400">Agendados</span>
        <p class="text-xl font-extrabold text-amber-400 mt-1">
          {{ appointments.filter(a => a.status === 'SCHEDULED').length }}
        </p>
      </div>
      <div class="glass-card rounded-xl p-4 border border-slate-800">
        <span class="text-xs text-emerald-400">Confirmados / Concluídos</span>
        <p class="text-xl font-extrabold text-emerald-400 mt-1">
          {{ appointments.filter(a => a.status === 'CONFIRMED' || a.status === 'COMPLETED').length }}
        </p>
      </div>
      <div class="glass-card rounded-xl p-4 border border-slate-800">
        <span class="text-xs text-rose-400">Cancelados</span>
        <p class="text-xl font-extrabold text-rose-400 mt-1">
          {{ appointments.filter(a => a.status === 'CANCELLED').length }}
        </p>
      </div>
    </div>

    <!-- Lista de Agendamentos -->
    <div class="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
      <div v-if="isLoading" class="text-center py-12">
        <div class="inline-block animate-spin w-8 h-8 border-4 border-amber-500 border-t-transparent rounded-full mb-2"></div>
        <p class="text-xs text-slate-400">Carregando agendamentos...</p>
      </div>

      <div v-else-if="appointments.length === 0" class="text-center py-16 bg-slate-900/40 rounded-xl border border-slate-800/80">
        <p class="text-sm font-semibold text-slate-300">Nenhum atendimento marcado para este dia.</p>
        <p class="text-xs text-slate-500 mt-1">Os agendamentos feitos pela página pública ou de balcão aparecerão aqui.</p>
      </div>

      <div v-else class="divide-y divide-slate-800/80">
        <div
          v-for="appt in appointments"
          :key="appt.id"
          class="py-4 flex flex-col md:flex-row md:items-center justify-between gap-4"
        >
          <!-- Info Horário e Cliente -->
          <div class="flex items-start sm:items-center gap-4">
            <div class="w-16 text-center py-2 px-1 rounded-xl bg-slate-900 border border-slate-800">
              <span class="block text-sm font-extrabold text-amber-400">{{ formatTime(appt.startTime) }}</span>
              <span class="block text-[10px] text-slate-500">{{ formatTime(appt.endTime) }}</span>
            </div>

            <div>
              <div class="flex items-center gap-2">
                <h3 class="font-bold text-sm text-slate-100">{{ appt.customerName }}</h3>
                <span :class="['text-[10px] font-bold px-2 py-0.5 rounded border', getStatusClass(appt.status)]">
                  {{ appt.status }}
                </span>
              </div>
              <div class="flex flex-wrap items-center gap-3 text-xs text-slate-400 mt-1">
                <span>✂ <strong>{{ appt.serviceName }}</strong></span>
                <span>👤 Profissional: <strong class="text-slate-300">{{ appt.professionalName }}</strong></span>
                <a
                  :href="'https://wa.me/' + cleanPhone(appt.customerPhone)"
                  target="_blank"
                  class="text-emerald-400 hover:underline flex items-center gap-1"
                >
                  📱 {{ appt.customerPhone }}
                </a>
              </div>
              <p v-if="appt.notes" class="text-[11px] text-slate-500 mt-1 italic">
                Obs: "{{ appt.notes }}"
              </p>
            </div>
          </div>

          <!-- Ações de Mudança de Status -->
          <div class="flex items-center gap-2 self-end md:self-center">
            <button
              v-if="appt.status === 'SCHEDULED'"
              @click="changeStatus(appt.id, 'CONFIRMED')"
              class="px-2.5 py-1 rounded-lg text-xs font-semibold bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 transition-colors"
            >
              Confirmar
            </button>
            <button
              v-if="appt.status === 'CONFIRMED' || appt.status === 'SCHEDULED'"
              @click="changeStatus(appt.id, 'COMPLETED')"
              class="px-2.5 py-1 rounded-lg text-xs font-semibold bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 border border-blue-500/30 transition-colors"
            >
              Concluir
            </button>
            <button
              v-if="appt.status !== 'CANCELLED' && appt.status !== 'COMPLETED'"
              @click="changeStatus(appt.id, 'CANCELLED')"
              class="px-2.5 py-1 rounded-lg text-xs font-semibold bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 transition-colors"
            >
              Cancelar
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal: Novo Agendamento de Balcão -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div class="glass-panel rounded-3xl p-6 sm:p-8 max-w-lg w-full space-y-4 border border-slate-700">
        <div class="flex justify-between items-center">
          <h2 class="text-lg font-bold text-white">Novo Agendamento de Balcão</h2>
          <button @click="showModal = false" class="text-slate-400 hover:text-white text-lg">✕</button>
        </div>

        <form @submit.prevent="saveManualAppointment" class="space-y-4">
          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Nome do Cliente *</label>
            <input
              v-model="newAppt.customerName"
              required
              type="text"
              class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
            />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Telefone *</label>
              <input
                v-model="newAppt.customerPhone"
                required
                type="text"
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
              />
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">E-mail</label>
              <input
                v-model="newAppt.customerEmail"
                type="email"
                placeholder="cliente@email.com"
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
              />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Serviço *</label>
              <select
                v-model="newAppt.serviceId"
                required
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
              >
                <option v-for="s in services" :key="s.id" :value="s.id">
                  {{ s.name }} ({{ s.durationMinutes }}min)
                </option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Profissional *</label>
              <select
                v-model="newAppt.professionalId"
                required
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
              >
                <option v-for="p in professionals" :key="p.id" :value="p.id">
                  {{ p.name }}
                </option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Data *</label>
              <input
                v-model="newAppt.date"
                type="date"
                required
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
              />
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Horário (HH:mm) *</label>
              <input
                v-model="newAppt.time"
                type="time"
                required
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
              />
            </div>
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Observações</label>
            <input
              v-model="newAppt.notes"
              type="text"
              class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
            />
          </div>

          <div v-if="modalError" class="p-2.5 rounded bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs">
            {{ modalError }}
          </div>

          <div class="flex justify-end gap-2 pt-2">
            <button
              type="button"
              @click="showModal = false"
              class="px-4 py-2 rounded-lg text-xs font-semibold bg-slate-800 text-slate-300 hover:bg-slate-700"
            >
              Cancelar
            </button>
            <button
              type="submit"
              :disabled="isSaving"
              class="px-5 py-2 rounded-lg text-xs font-bold bg-amber-400 hover:bg-amber-300 text-slate-950 shadow-md shadow-amber-400/20"
            >
              {{ isSaving ? 'Salvando...' : 'Salvar Atendimento' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminService, publicBookingService } from '../../services/api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const appointments = ref([])
const services = ref([])
const professionals = ref([])
const isLoading = ref(true)
const selectedDate = ref(new Date().toISOString().split('T')[0])

const showModal = ref(false)
const isSaving = ref(false)
const modalError = ref(null)

const newAppt = ref({
  customerName: '',
  customerPhone: '',
  customerEmail: '',
  serviceId: '',
  professionalId: '',
  date: selectedDate.value,
  time: '14:00',
  notes: ''
})

onMounted(() => {
  loadAppointments()
  loadMetadata()
})

function setDateToday() {
  selectedDate.value = new Date().toISOString().split('T')[0]
  loadAppointments()
}

async function loadAppointments() {
  isLoading.value = true
  try {
    const res = await adminService.getAppointments(selectedDate.value)
    appointments.value = res.data
  } catch (err) {
    console.error(err)
  } finally {
    isLoading.value = false
  }
}

async function loadMetadata() {
  try {
    const [sRes, pRes] = await Promise.all([
      adminService.getServices(),
      adminService.getProfessionals()
    ])
    services.value = sRes.data
    professionals.value = pRes.data
    if (services.value.length > 0) newAppt.value.serviceId = services.value[0].id
    if (professionals.value.length > 0) newAppt.value.professionalId = professionals.value[0].id
  } catch (err) {
    console.error(err)
  }
}

async function changeStatus(id, newStatus) {
  try {
    await adminService.updateStatus(id, newStatus)
    loadAppointments()
  } catch (err) {
    alert(err.response?.data?.message || 'Falha ao atualizar status.')
  }
}

function openNewModal() {
  newAppt.value.date = selectedDate.value
  modalError.value = null
  showModal.value = true
}

async function saveManualAppointment() {
  isSaving.value = true
  modalError.value = null

  try {
    const slug = auth.tenantSlug || 'barbearia-vintage'
    const startIso = `${newAppt.value.date}T${newAppt.value.time}:00-03:00`

    await publicBookingService.createAppointment(slug, {
      professionalId: newAppt.value.professionalId,
      serviceId: newAppt.value.serviceId,
      startTime: startIso,
      customerName: newAppt.value.customerName,
      customerEmail: newAppt.value.customerEmail || 'balcao@barbershop.local',
      customerPhone: newAppt.value.customerPhone,
      notes: newAppt.value.notes ? `[BALCÃO] ${newAppt.value.notes}` : '[BALCÃO]'
    })

    showModal.value = false
    loadAppointments()
  } catch (err) {
    modalError.value = err.response?.data?.message || 'Falha ao salvar. Verifique se o horário já está ocupado.'
  } finally {
    isSaving.value = false
  }
}

function formatTime(isoStr) {
  if (!isoStr) return ''
  return new Date(isoStr).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

function cleanPhone(phone) {
  return (phone || '').replace(/\D/g, '')
}

function getStatusClass(status) {
  switch (status) {
    case 'SCHEDULED': return 'bg-amber-500/10 text-amber-400 border-amber-500/30'
    case 'CONFIRMED': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
    case 'COMPLETED': return 'bg-blue-500/10 text-blue-400 border-blue-500/30'
    case 'CANCELLED': return 'bg-rose-500/10 text-rose-400 border-rose-500/30'
    default: return 'bg-slate-800 text-slate-300 border-slate-700'
  }
}
</script>
