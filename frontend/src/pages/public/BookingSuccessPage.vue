<template>
  <div class="max-w-xl mx-auto px-4 py-12">
    <div v-if="booking" class="glass-panel rounded-3xl p-8 text-center relative overflow-hidden border border-emerald-500/30">
      <div class="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto mb-4 text-3xl font-extrabold shadow-lg shadow-emerald-500/20">
        ✓
      </div>

      <span class="text-xs font-semibold uppercase tracking-wider text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
        Agendamento Confirmado!
      </span>

      <h1 class="text-2xl font-extrabold text-white mt-3">Tudo pronto, {{ booking.customerName }}!</h1>
      <p class="text-xs text-slate-400 mt-1">
        Enviamos todos os detalhes do seu horário para <strong>{{ booking.customerEmail }}</strong>.
      </p>

      <!-- Cartão do Comprovante -->
      <div class="glass-card rounded-2xl p-5 my-6 text-left space-y-3 border border-slate-800">
        <div class="flex justify-between items-center pb-3 border-b border-slate-800">
          <span class="text-xs text-slate-400">Serviço:</span>
          <span class="text-sm font-bold text-slate-100">{{ booking.serviceName }}</span>
        </div>
        <div class="flex justify-between items-center pb-3 border-b border-slate-800">
          <span class="text-xs text-slate-400">Profissional:</span>
          <span class="text-sm font-bold text-amber-400">{{ booking.professionalName }}</span>
        </div>
        <div class="flex justify-between items-center pb-3 border-b border-slate-800">
          <span class="text-xs text-slate-400">Data e Horário:</span>
          <span class="text-sm font-bold text-slate-100">
            {{ formatDate(booking.startTime) }} às {{ formatTime(booking.startTime) }}
          </span>
        </div>
        <div class="flex justify-between items-center">
          <span class="text-xs text-slate-400">Status:</span>
          <span class="text-xs font-semibold px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            {{ booking.status }}
          </span>
        </div>
      </div>

      <!-- Área de Cancelamento / Token -->
      <div class="p-4 rounded-xl bg-slate-900/90 border border-slate-800 text-left space-y-2 mb-6">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold text-slate-300">Precisa desmarcar ou reagendar?</span>
          <span class="text-[10px] text-slate-500">Sem login necessário</span>
        </div>
        <p class="text-[11px] text-slate-400">
          Você pode desmarcar seu horário a qualquer momento clicando no botão abaixo ou utilizando o seu link pessoal:
        </p>
        <div class="flex items-center gap-2 pt-1">
          <input
            readonly
            type="text"
            :value="cancelLink"
            class="flex-1 bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1 text-xs text-slate-400 select-all font-mono"
          />
          <button
            @click="copyLink"
            class="px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded-lg transition-colors font-medium"
          >
            {{ copied ? 'Copiado!' : 'Copiar' }}
          </button>
        </div>
      </div>

      <!-- Ações -->
      <div class="flex flex-col sm:flex-row gap-3 justify-center">
        <router-link
          :to="`/cancelar/${booking.cancellationToken}`"
          class="px-4 py-2 rounded-xl text-xs font-medium text-rose-400 bg-rose-500/10 border border-rose-500/20 hover:bg-rose-500/20 transition-colors"
        >
          Desmarcar este Horário
        </router-link>
        <router-link
          :to="`/b/${slug}`"
          class="px-5 py-2 rounded-xl text-xs font-bold text-slate-950 bg-amber-400 hover:bg-amber-300 transition-colors shadow-md shadow-amber-400/20"
        >
          Fazer Outro Agendamento
        </router-link>
      </div>
    </div>

    <div v-else class="text-center py-16">
      <p class="text-slate-400 text-sm mb-4">Nenhum agendamento recente encontrado nesta sessão.</p>
      <router-link
        to="/b/barbearia-vintage"
        class="text-xs text-amber-400 hover:underline font-semibold"
      >
        Ir para página de agendamentos
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const slug = computed(() => route.params.slug || 'barbearia-vintage')
const booking = ref(null)
const copied = ref(false)

const cancelLink = computed(() => {
  if (!booking.value?.cancellationToken) return ''
  return `${window.location.origin}/cancelar/${booking.value.cancellationToken}`
})

onMounted(() => {
  const data = sessionStorage.getItem('lastBooking')
  if (data) {
    try {
      booking.value = JSON.parse(data)
    } catch (e) {
      console.error(e)
    }
  }
})

function copyLink() {
  if (!cancelLink.value) return
  navigator.clipboard.writeText(cancelLink.value)
  copied.value = true
  setTimeout(() => { copied.value = false }, 3000)
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
