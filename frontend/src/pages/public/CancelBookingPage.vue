<template>
  <div class="max-w-md mx-auto px-4 py-16">
    <div v-if="isLoading" class="text-center py-12">
      <div class="inline-block animate-spin w-8 h-8 border-4 border-amber-500 border-t-transparent rounded-full mb-3"></div>
      <p class="text-xs text-slate-400">Localizando seu agendamento...</p>
    </div>

    <div v-else-if="error" class="glass-panel rounded-2xl p-6 text-center border-rose-500/30">
      <div class="w-12 h-12 rounded-full bg-rose-500/10 text-rose-500 flex items-center justify-center mx-auto mb-3 text-xl font-bold">
        ✕
      </div>
      <h2 class="text-lg font-bold text-white mb-2">Não foi possível carregar</h2>
      <p class="text-xs text-slate-400 mb-5">{{ error }}</p>
      <router-link to="/" class="text-xs text-amber-400 hover:underline">
        Voltar à página inicial
      </router-link>
    </div>

    <!-- Detalhes do Agendamento e Ação de Cancelar -->
    <div v-else-if="appointment" class="glass-panel rounded-2xl p-6 space-y-6">
      <div class="text-center">
        <span class="text-xs uppercase tracking-wider font-semibold text-rose-400 bg-rose-500/10 px-2.5 py-0.5 rounded-full border border-rose-500/20">
          Cancelamento de Horário
        </span>
        <h1 class="text-xl font-bold text-white mt-2">{{ appointment.tenantName }}</h1>
        <p class="text-xs text-slate-400 mt-1">Deseja realmente desmarcar este atendimento?</p>
      </div>

      <!-- Resumo do Horário -->
      <div class="glass-card rounded-xl p-4 text-xs space-y-2.5 border border-slate-800">
        <div class="flex justify-between">
          <span class="text-slate-400">Cliente:</span>
          <span class="font-bold text-slate-200">{{ appointment.customerName }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Serviço:</span>
          <span class="font-bold text-slate-200">{{ appointment.serviceName }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Profissional:</span>
          <span class="font-bold text-amber-400">{{ appointment.professionalName }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Data e Hora:</span>
          <span class="font-bold text-slate-200">
            {{ formatDate(appointment.startTime) }} às {{ formatTime(appointment.startTime) }}
          </span>
        </div>
        <div class="flex justify-between items-center pt-1 border-t border-slate-800">
          <span class="text-slate-400">Status Atual:</span>
          <span :class="[
            'px-2 py-0.5 rounded font-bold text-[10px]',
            appointment.status === 'CANCELLED' ? 'bg-rose-500/10 text-rose-400' : 'bg-emerald-500/10 text-emerald-400'
          ]">
            {{ appointment.status }}
          </span>
        </div>
      </div>

      <!-- Se já estiver cancelado -->
      <div v-if="appointment.status === 'CANCELLED'" class="p-3 rounded-lg bg-slate-900 border border-slate-800 text-center text-xs text-slate-400">
        Este agendamento já se encontra cancelado.
      </div>

      <!-- Se estiver ativo, botão para cancelar -->
      <div v-else class="space-y-3">
        <div v-if="cancelError" class="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs">
          {{ cancelError }}
        </div>

        <button
          @click="confirmCancel"
          :disabled="isCancelling"
          class="w-full py-2.5 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-500 text-white transition-all shadow-md shadow-rose-600/20 disabled:opacity-50"
        >
          {{ isCancelling ? 'Cancelando...' : 'Confirmar Cancelamento' }}
        </button>
      </div>

      <!-- Sucesso pós-cancelamento -->
      <div v-if="cancelSuccess" class="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-center space-y-2">
        <p class="text-xs font-bold text-emerald-400">Horário desmarcado com sucesso!</p>
        <p class="text-[11px] text-slate-400">A vaga já foi liberada para outros clientes.</p>
      </div>

      <div class="text-center pt-2">
        <router-link to="/" class="text-xs text-slate-400 hover:text-amber-400 transition-colors">
          ← Voltar e Agendar Outro Horário
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { publicBookingService } from '../../services/api'

const route = useRoute()
const token = route.params.token

const appointment = ref(null)
const isLoading = ref(true)
const error = ref(null)

const isCancelling = ref(false)
const cancelError = ref(null)
const cancelSuccess = ref(false)

onMounted(async () => {
  try {
    isLoading.value = true
    const res = await publicBookingService.getAppointmentByToken(token)
    appointment.value = res.data
  } catch (err) {
    error.value = err.response?.data?.message || 'Agendamento não encontrado para este link.'
  } finally {
    isLoading.value = false
  }
})

async function confirmCancel() {
  isCancelling.value = true
  cancelError.value = null
  try {
    await publicBookingService.cancelAppointment(token)
    appointment.value.status = 'CANCELLED'
    cancelSuccess.value = true
  } catch (err) {
    cancelError.value = err.response?.data?.message || 'Falha ao cancelar o agendamento.'
  } finally {
    isCancelling.value = false
  }
}

function formatDate(isoStr) {
  if (!isoStr) return ''
  return new Date(isoStr).toLocaleDateString('pt-BR')
}

function formatTime(isoStr) {
  if (!isoStr) return ''
  return new Date(isoStr).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}
</script>
