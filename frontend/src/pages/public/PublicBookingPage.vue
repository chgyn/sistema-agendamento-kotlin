<template>
  <div class="max-w-4xl mx-auto px-4 py-8 sm:py-12">
    <!-- Loading Inicial do Estabelecimento -->
    <div v-if="isLoadingTenant" class="text-center py-24">
      <div class="inline-block animate-spin w-10 h-10 border-4 border-amber-500 border-t-transparent rounded-full mb-4"></div>
      <p class="text-slate-400 text-sm">Carregando informações da barbearia...</p>
    </div>

    <!-- Erro ao Carregar Barbearia -->
    <div v-else-if="tenantError" class="glass-panel rounded-2xl p-8 text-center max-w-md mx-auto my-12">
      <div class="w-12 h-12 rounded-full bg-rose-500/10 text-rose-500 flex items-center justify-center mx-auto mb-4 text-2xl font-bold">
        !
      </div>
      <h2 class="text-xl font-bold text-white mb-2">Barbearia não encontrada</h2>
      <p class="text-slate-400 text-sm mb-6">{{ tenantError }}</p>
      <router-link to="/b/barbearia-vintage" class="text-xs text-amber-400 hover:underline">
        Acessar Barbearia Vintage de Demonstração
      </router-link>
    </div>

    <!-- Fluxo Principal de Agendamento -->
    <div v-else class="space-y-8">
      <!-- Cabeçalho do Estabelecimento -->
      <div class="glass-panel rounded-2xl p-6 sm:p-8 relative overflow-hidden">
        <div class="absolute -right-10 -bottom-10 w-44 h-44 bg-amber-500/10 rounded-full blur-3xl pointer-events-none"></div>
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <span class="text-xs uppercase tracking-wider font-semibold text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-full border border-amber-500/20">
              Agendamento Online
            </span>
            <h1 class="text-2xl sm:text-3xl font-extrabold text-white mt-2">{{ tenant?.name }}</h1>
            <p class="text-slate-400 text-xs sm:text-sm mt-1">
              {{ tenant?.phone ? 'WhatsApp: ' + tenant.phone : '' }} 
              {{ tenant?.email ? '• ' + tenant.email : '' }}
            </p>
          </div>
          <div class="text-right flex sm:flex-col items-center sm:items-end justify-between">
            <span class="text-xs text-slate-400">Atendimento com hora marcada</span>
            <span class="text-xs text-emerald-400 flex items-center gap-1.5 mt-1">
              <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span> Agenda Aberta
            </span>
          </div>
        </div>
      </div>

      <!-- Etapa 1: Seleção de Serviço -->
      <section class="space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold text-white flex items-center gap-2">
            <span class="w-6 h-6 rounded-full bg-amber-500/20 text-amber-400 text-xs flex items-center justify-center font-bold">1</span>
            Escolha o Serviço
          </h2>
          <span v-if="selectedService" class="text-xs text-amber-400 font-medium">
            Selecionado: {{ selectedService.name }} ({{ selectedService.durationMinutes }}min)
          </span>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3.5">
          <div
            v-for="service in tenant?.services"
            :key="service.id"
            @click="selectService(service)"
            :class="[
              'glass-card rounded-xl p-4 cursor-pointer transition-all border',
              selectedService?.id === service.id
                ? 'border-amber-500 bg-amber-500/10 shadow-lg shadow-amber-500/10'
                : 'border-slate-800 hover:border-slate-700 hover:bg-slate-800/40'
            ]"
          >
            <div class="flex justify-between items-start mb-2">
              <h3 class="font-bold text-slate-100 text-sm">{{ service.name }}</h3>
              <span class="font-extrabold text-amber-400 text-sm">
                R$ {{ Number(service.price).toFixed(2).replace('.', ',') }}
              </span>
            </div>
            <p class="text-xs text-slate-400 line-clamp-2 mb-3">{{ service.description || 'Atendimento profissional personalizado.' }}</p>
            <div class="flex items-center justify-between text-[11px] text-slate-500 border-t border-slate-800/60 pt-2">
              <span>⏱ Duração: {{ service.durationMinutes }} min</span>
              <span v-if="selectedService?.id === service.id" class="text-amber-400 font-semibold">✓ Selecionado</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Etapa 2: Seleção de Profissional (Opcional) -->
      <section v-if="selectedService" class="space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold text-white flex items-center gap-2">
            <span class="w-6 h-6 rounded-full bg-amber-500/20 text-amber-400 text-xs flex items-center justify-center font-bold">2</span>
            Escolha o Barbeiro / Profissional
          </h2>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
          <!-- Opção Qualquer Profissional -->
          <div
            @click="selectProfessional(null)"
            :class="[
              'glass-card rounded-xl p-3.5 text-center cursor-pointer transition-all border',
              selectedProfessional === null
                ? 'border-amber-500 bg-amber-500/10'
                : 'border-slate-800 hover:border-slate-700'
            ]"
          >
            <div class="w-10 h-10 rounded-full bg-slate-800 text-amber-400 flex items-center justify-center mx-auto mb-2 text-sm font-bold">
              ★
            </div>
            <h4 class="font-semibold text-xs text-slate-200">Qualquer um</h4>
            <p class="text-[10px] text-slate-400 mt-0.5">Primeiro horário livre</p>
          </div>

          <!-- Profissionais Disponíveis para o Serviço -->
          <div
            v-for="prof in eligibleProfessionals"
            :key="prof.id"
            @click="selectProfessional(prof)"
            :class="[
              'glass-card rounded-xl p-3.5 text-center cursor-pointer transition-all border',
              selectedProfessional?.id === prof.id
                ? 'border-amber-500 bg-amber-500/10'
                : 'border-slate-800 hover:border-slate-700'
            ]"
          >
            <div class="w-10 h-10 rounded-full bg-amber-500/20 text-amber-400 flex items-center justify-center mx-auto mb-2 text-sm font-bold">
              {{ prof.name.charAt(0) }}
            </div>
            <h4 class="font-semibold text-xs text-slate-200 truncate">{{ prof.name }}</h4>
            <p class="text-[10px] text-slate-400 mt-0.5 truncate">{{ prof.bio || 'Especialista' }}</p>
          </div>
        </div>
      </section>

      <!-- Etapa 3: Seleção de Data e Horário -->
      <section v-if="selectedService" class="space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold text-white flex items-center gap-2">
            <span class="w-6 h-6 rounded-full bg-amber-500/20 text-amber-400 text-xs flex items-center justify-center font-bold">3</span>
            Data e Horário
          </h2>
        </div>

        <div class="glass-panel rounded-2xl p-5 space-y-5">
          <!-- Campo de Seleção de Data -->
          <div class="flex flex-col sm:flex-row sm:items-center gap-3">
            <label class="text-xs font-semibold text-slate-300">Escolha a Data do Atendimento:</label>
            <input
              type="date"
              v-model="selectedDate"
              :min="minDate"
              @change="fetchAvailableSlots"
              class="bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-sm text-slate-200 focus:outline-none focus:border-amber-500 transition-colors cursor-pointer"
            />
          </div>

          <!-- Grade de Slots Livres -->
          <div>
            <div v-if="isLoadingSlots" class="text-center py-8">
              <div class="inline-block animate-spin w-6 h-6 border-2 border-amber-500 border-t-transparent rounded-full mb-2"></div>
              <p class="text-xs text-slate-400">Consultando horários disponíveis...</p>
            </div>

            <div v-else-if="availableSlots.length === 0" class="text-center py-8 bg-slate-900/50 rounded-xl border border-slate-800">
              <p class="text-sm font-medium text-slate-300">Nenhum horário livre encontrado para esta data.</p>
              <p class="text-xs text-slate-500 mt-1">Experimente escolher outro dia ou selecionar outro profissional.</p>
            </div>

            <div v-else class="space-y-2">
              <span class="text-xs text-slate-400">Horários livres ({{ availableSlots.length }} disponíveis):</span>
              <div class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2">
                <button
                  v-for="slot in availableSlots"
                  :key="slot.startTime"
                  type="button"
                  @click="selectedSlot = slot"
                  :class="[
                    'py-2 px-3 rounded-lg text-xs font-bold transition-all border text-center',
                    selectedSlot?.startTime === slot.startTime
                      ? 'bg-amber-400 text-slate-950 border-amber-400 shadow-md shadow-amber-400/20 scale-105'
                      : 'bg-slate-800/80 hover:bg-slate-700 text-slate-200 border-slate-700'
                  ]"
                >
                  {{ formatTime(slot.startTime) }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Etapa 4: Dados do Cliente e Conclusão -->
      <section v-if="selectedService && selectedSlot" class="space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold text-white flex items-center gap-2">
            <span class="w-6 h-6 rounded-full bg-amber-500/20 text-amber-400 text-xs flex items-center justify-center font-bold">4</span>
            Finalize seus Dados
          </h2>
        </div>

        <form @submit.prevent="submitAppointment" class="glass-panel rounded-2xl p-6 space-y-4">
          <div v-if="submissionError" class="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs">
            {{ submissionError }}
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Seu Nome Completo *</label>
              <input
                v-model="customer.name"
                required
                type="text"
                placeholder="Ex: Pedro Henrique"
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3.5 py-2 text-sm text-slate-100 focus:outline-none focus:border-amber-500"
              />
            </div>

            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Telefone / WhatsApp *</label>
              <input
                v-model="customer.phone"
                required
                type="tel"
                placeholder="Ex: (11) 98765-4321"
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3.5 py-2 text-sm text-slate-100 focus:outline-none focus:border-amber-500"
              />
            </div>
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">E-mail para Confirmação *</label>
            <input
              v-model="customer.email"
              required
              type="email"
              placeholder="Ex: pedro@email.com"
              class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3.5 py-2 text-sm text-slate-100 focus:outline-none focus:border-amber-500"
            />
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Observações (Opcional)</label>
            <textarea
              v-model="customer.notes"
              rows="2"
              placeholder="Alguma preferência ou aviso especial?"
              class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3.5 py-2 text-sm text-slate-100 focus:outline-none focus:border-amber-500 resize-none"
            ></textarea>
          </div>

          <!-- Resumo de Confirmação -->
          <div class="p-4 rounded-xl bg-slate-900/80 border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs">
            <div>
              <p class="text-slate-400">
                Resumo: <strong class="text-slate-200">{{ selectedService.name }}</strong> no dia 
                <strong class="text-slate-200">{{ formatDateBr(selectedSlot.startTime) }}</strong> às 
                <strong class="text-amber-400">{{ formatTime(selectedSlot.startTime) }}</strong>
              </p>
              <p class="text-slate-500 mt-0.5">
                Total: R$ {{ Number(selectedService.price).toFixed(2).replace('.', ',') }} (Pagamento no local)
              </p>
            </div>

            <button
              type="submit"
              :disabled="isSubmitting"
              class="w-full sm:w-auto px-6 py-2.5 rounded-xl font-bold text-sm bg-gradient-to-r from-amber-500 to-amber-400 hover:from-amber-400 hover:to-amber-300 text-slate-950 transition-all shadow-lg shadow-amber-500/20 disabled:opacity-50"
            >
              {{ isSubmitting ? 'Confirmando Horário...' : 'Confirmar Agendamento' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { publicBookingService } from '../../services/api'

const route = useRoute()
const router = useRouter()
const slug = computed(() => route.params.slug || 'barbearia-vintage')

const tenant = ref(null)
const isLoadingTenant = ref(true)
const tenantError = ref(null)

const selectedService = ref(null)
const selectedProfessional = ref(null)
const selectedDate = ref(new Date().toISOString().split('T')[0])
const selectedSlot = ref(null)
const availableSlots = ref([])
const isLoadingSlots = ref(false)

const isSubmitting = ref(false)
const submissionError = ref(null)

const customer = ref({
  name: '',
  phone: '',
  email: '',
  notes: ''
})

const minDate = computed(() => new Date().toISOString().split('T')[0])

const eligibleProfessionals = computed(() => {
  if (!tenant.value || !selectedService.value) return []
  return tenant.value.professionals.filter(p =>
    p.serviceIds.includes(selectedService.value.id)
  )
})

onMounted(async () => {
  try {
    isLoadingTenant.value = true
    const res = await publicBookingService.getTenantDetails(slug.value)
    tenant.value = res.data
  } catch (err) {
    tenantError.value = err.response?.data?.message || 'Não foi possível carregar esta barbearia.'
  } finally {
    isLoadingTenant.value = false
  }
})

function selectService(service) {
  selectedService.value = service
  selectedSlot.value = null
  fetchAvailableSlots()
}

function selectProfessional(prof) {
  selectedProfessional.value = prof
  selectedSlot.value = null
  fetchAvailableSlots()
}

async function fetchAvailableSlots() {
  if (!selectedService.value || !selectedDate.value) return
  isLoadingSlots.value = true
  selectedSlot.value = null
  try {
    const profId = selectedProfessional.value?.id || null
    const res = await publicBookingService.getAvailability(
      slug.value,
      selectedService.value.id,
      selectedDate.value,
      profId
    )
    availableSlots.value = res.data
  } catch (err) {
    console.error('Falha ao buscar slots:', err)
  } finally {
    isLoadingSlots.value = false
  }
}

async function submitAppointment() {
  if (!selectedService.value || !selectedSlot.value) return
  isSubmitting.value = true
  submissionError.value = null

  try {
    const payload = {
      professionalId: selectedSlot.value.professionalId,
      serviceId: selectedService.value.id,
      startTime: selectedSlot.value.startTime,
      customerName: customer.value.name,
      customerEmail: customer.value.email,
      customerPhone: customer.value.phone,
      notes: customer.value.notes
    }

    const res = await publicBookingService.createAppointment(slug.value, payload)

    // Redireciona com dados para a tela de confirmação
    sessionStorage.setItem('lastBooking', JSON.stringify(res.data))
    router.push({
      name: 'booking-success',
      params: { slug: slug.value }
    })
  } catch (err) {
    if (err.response?.status === 409) {
      submissionError.value = 'Este horário acabou de ser reservado por outro cliente. Por favor, selecione outro horário.'
      fetchAvailableSlots()
    } else {
      submissionError.value = err.response?.data?.message || 'Falha ao processar agendamento. Tente novamente.'
    }
  } finally {
    isSubmitting.value = false
  }
}

function formatTime(isoStr) {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

function formatDateBr(isoStr) {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  return d.toLocaleDateString('pt-BR')
}
</script>
