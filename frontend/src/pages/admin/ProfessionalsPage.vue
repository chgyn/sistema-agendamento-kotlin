<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-extrabold text-white">Equipe & Profissionais</h1>
        <p class="text-xs text-slate-400 mt-1">Gerencie os barbeiros e suas grades de horários semanais</p>
      </div>
      <button
        @click="showCreateModal = true"
        class="px-4 py-2 rounded-xl text-xs font-bold bg-amber-400 hover:bg-amber-300 text-slate-950 transition-all shadow-md shadow-amber-400/20"
      >
        + Cadastrar Profissional
      </button>
    </div>

    <!-- Lista de Profissionais -->
    <div v-if="isLoading" class="text-center py-12">
      <div class="inline-block animate-spin w-8 h-8 border-4 border-amber-500 border-t-transparent rounded-full mb-2"></div>
      <p class="text-xs text-slate-400">Carregando equipe...</p>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="prof in professionals"
        :key="prof.id"
        class="glass-panel rounded-2xl p-5 border border-slate-800 flex flex-col justify-between"
      >
        <div>
          <div class="flex items-center gap-3 mb-3">
            <div class="w-12 h-12 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 flex items-center justify-center text-lg font-bold">
              {{ prof.name.charAt(0) }}
            </div>
            <div>
              <h3 class="font-bold text-base text-slate-100">{{ prof.name }}</h3>
              <p class="text-xs text-slate-400">{{ prof.bio || 'Sem biografia informada' }}</p>
            </div>
          </div>

          <div class="space-y-1.5 text-xs text-slate-400 border-t border-slate-800/80 pt-3">
            <p>✂ Serviços Habilitados: <strong class="text-slate-200">{{ prof.serviceIds.length }}</strong></p>
          </div>
        </div>

        <div class="mt-4 pt-3 border-t border-slate-800/80 flex justify-end">
          <button
            @click="openScheduleModal(prof)"
            class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-amber-400 border border-slate-700 transition-colors"
          >
            Configurar Grade Semanal ⚙
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Novo Profissional -->
    <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div class="glass-panel rounded-3xl p-6 sm:p-8 max-w-md w-full space-y-4 border border-slate-700">
        <h2 class="text-lg font-bold text-white">Cadastrar Novo Profissional</h2>

        <form @submit.prevent="createProfessional" class="space-y-3">
          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Nome Completo *</label>
            <input v-model="newProf.name" required type="text" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">E-mail</label>
            <input v-model="newProf.email" type="email" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Telefone</label>
            <input v-model="newProf.phone" type="tel" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Especialidade / Bio</label>
            <textarea v-model="newProf.bio" rows="2" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white resize-none"></textarea>
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Serviços que realiza:</label>
            <div class="space-y-1.5 max-h-32 overflow-y-auto p-2 bg-slate-900/80 rounded-lg border border-slate-800">
              <label v-for="s in services" :key="s.id" class="flex items-center gap-2 text-xs text-slate-300 cursor-pointer">
                <input type="checkbox" :value="s.id" v-model="newProf.serviceIds" class="rounded border-slate-700 text-amber-500" />
                {{ s.name }} ({{ s.durationMinutes }}min)
              </label>
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-3">
            <button type="button" @click="showCreateModal = false" class="px-4 py-2 rounded-lg text-xs bg-slate-800 text-slate-300">
              Cancelar
            </button>
            <button type="submit" class="px-5 py-2 rounded-lg text-xs font-bold bg-amber-400 hover:bg-amber-300 text-slate-950">
              Salvar
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal Grade Semanal -->
    <div v-if="showScheduleModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div class="glass-panel rounded-3xl p-6 sm:p-8 max-w-2xl w-full space-y-4 border border-slate-700 max-h-[90vh] overflow-y-auto">
        <div class="flex justify-between items-center">
          <h2 class="text-lg font-bold text-white">Grade Semanal: {{ currentProf?.name }}</h2>
          <button @click="showScheduleModal = false" class="text-slate-400 hover:text-white">✕</button>
        </div>

        <p class="text-xs text-slate-400">Configure os horários de início, término e intervalo de almoço:</p>

        <div class="space-y-2.5">
          <div
            v-for="day in weekDays"
            :key="day.dayOfWeek"
            class="p-3 rounded-xl bg-slate-900 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
          >
            <div class="flex items-center gap-2 w-32">
              <input type="checkbox" v-model="day.active" class="rounded border-slate-700 text-amber-500" />
              <span class="font-semibold text-slate-200">{{ day.label }}</span>
            </div>

            <div v-if="day.active" class="flex flex-wrap items-center gap-2">
              <span>Trabalho:</span>
              <input type="time" v-model="day.startTime" class="bg-slate-950 border border-slate-700 rounded px-2 py-1 text-slate-200" />
              <span>às</span>
              <input type="time" v-model="day.endTime" class="bg-slate-950 border border-slate-700 rounded px-2 py-1 text-slate-200" />

              <span class="ml-2">Almoço:</span>
              <input type="time" v-model="day.breakStart" class="bg-slate-950 border border-slate-700 rounded px-2 py-1 text-slate-200" />
              <span>às</span>
              <input type="time" v-model="day.breakEnd" class="bg-slate-950 border border-slate-700 rounded px-2 py-1 text-slate-200" />
            </div>

            <div v-else class="text-slate-500 italic">Folga / Não atende</div>
          </div>
        </div>

        <div class="flex justify-end gap-2 pt-3">
          <button type="button" @click="showScheduleModal = false" class="px-4 py-2 rounded-lg text-xs bg-slate-800 text-slate-300">
            Cancelar
          </button>
          <button @click="saveSchedule" class="px-5 py-2 rounded-lg text-xs font-bold bg-amber-400 hover:bg-amber-300 text-slate-950">
            Salvar Grade
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminService } from '../../services/api'

const professionals = ref([])
const services = ref([])
const isLoading = ref(true)

const showCreateModal = ref(false)
const showScheduleModal = ref(false)
const currentProf = ref(null)

const newProf = ref({
  name: '',
  email: '',
  phone: '',
  bio: '',
  serviceIds: []
})

const weekDays = ref([
  { dayOfWeek: 1, label: 'Segunda-feira', active: true, startTime: '09:00', endTime: '18:00', breakStart: '12:00', breakEnd: '13:00' },
  { dayOfWeek: 2, label: 'Terça-feira', active: true, startTime: '09:00', endTime: '18:00', breakStart: '12:00', breakEnd: '13:00' },
  { dayOfWeek: 3, label: 'Quarta-feira', active: true, startTime: '09:00', endTime: '18:00', breakStart: '12:00', breakEnd: '13:00' },
  { dayOfWeek: 4, label: 'Quinta-feira', active: true, startTime: '09:00', endTime: '18:00', breakStart: '12:00', breakEnd: '13:00' },
  { dayOfWeek: 5, label: 'Sexta-feira', active: true, startTime: '09:00', endTime: '18:00', breakStart: '12:00', breakEnd: '13:00' },
  { dayOfWeek: 6, label: 'Sábado', active: true, startTime: '09:00', endTime: '17:00', breakStart: '12:00', breakEnd: '13:00' },
  { dayOfWeek: 7, label: 'Domingo', active: false, startTime: '09:00', endTime: '14:00', breakStart: null, breakEnd: null }
])

onMounted(() => {
  loadData()
})

async function loadData() {
  isLoading.value = true
  try {
    const [pRes, sRes] = await Promise.all([
      adminService.getProfessionals(),
      adminService.getServices()
    ])
    professionals.value = pRes.data
    services.value = sRes.data
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

async function createProfessional() {
  try {
    await adminService.createProfessional(newProf.value)
    showCreateModal.value = false
    newProf.value = { name: '', email: '', phone: '', bio: '', serviceIds: [] }
    loadData()
  } catch (e) {
    alert(e.response?.data?.message || 'Falha ao salvar profissional.')
  }
}

async function openScheduleModal(prof) {
  currentProf.value = prof
  try {
    const res = await adminService.getProfessionalSchedule(prof.id)
    if (res.data && res.data.length > 0) {
      weekDays.value.forEach(d => {
        const found = res.data.find(h => h.dayOfWeek === d.dayOfWeek)
        if (found) {
          d.active = true
          d.startTime = found.startTime.substring(0, 5)
          d.endTime = found.endTime.substring(0, 5)
          d.breakStart = found.breakStart ? found.breakStart.substring(0, 5) : null
          d.breakEnd = found.breakEnd ? found.breakEnd.substring(0, 5) : null
        } else {
          d.active = false
        }
      })
    }
    showScheduleModal.value = true
  } catch (e) {
    console.error(e)
  }
}

async function saveSchedule() {
  try {
    const activeHours = weekDays.value
      .filter(d => d.active)
      .map(d => ({
        dayOfWeek: d.dayOfWeek,
        startTime: `${d.startTime}:00`,
        endTime: `${d.endTime}:00`,
        breakStart: d.breakStart ? `${d.breakStart}:00` : null,
        breakEnd: d.breakEnd ? `${d.breakEnd}:00` : null,
        slotDurationMinutes: 30
      }))

    await adminService.setProfessionalSchedule(currentProf.value.id, activeHours)
    showScheduleModal.value = false
    alert('Grade de horários atualizada!')
  } catch (e) {
    alert(e.response?.data?.message || 'Falha ao salvar horários.')
  }
}
</script>
