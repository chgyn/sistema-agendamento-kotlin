<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-extrabold text-white">Catálogo de Serviços</h1>
        <p class="text-xs text-slate-400 mt-1">Defina os procedimentos, tempo de duração e preços da barbearia</p>
      </div>
      <button
        @click="showModal = true"
        class="px-4 py-2 rounded-xl text-xs font-bold bg-amber-400 hover:bg-amber-300 text-slate-950 transition-all shadow-md shadow-amber-400/20"
      >
        + Novo Serviço
      </button>
    </div>

    <div v-if="isLoading" class="text-center py-12">
      <div class="inline-block animate-spin w-8 h-8 border-4 border-amber-500 border-t-transparent rounded-full mb-2"></div>
      <p class="text-xs text-slate-400">Carregando serviços...</p>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="service in services"
        :key="service.id"
        class="glass-panel rounded-2xl p-5 border border-slate-800 flex flex-col justify-between"
      >
        <div>
          <div class="flex justify-between items-start mb-2">
            <h3 class="font-bold text-base text-slate-100">{{ service.name }}</h3>
            <span class="font-extrabold text-amber-400 text-base">
              R$ {{ Number(service.price).toFixed(2).replace('.', ',') }}
            </span>
          </div>
          <p class="text-xs text-slate-400 line-clamp-2 mb-3">
            {{ service.description || 'Sem descrição cadastrada.' }}
          </p>
        </div>

        <div class="pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
          <span>⏱ Duração: <strong class="text-slate-200">{{ service.durationMinutes }} minutos</strong></span>
          <span class="text-emerald-400 text-[11px] font-semibold">Ativo na Agenda</span>
        </div>
      </div>
    </div>

    <!-- Modal Novo Serviço -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div class="glass-panel rounded-3xl p-6 sm:p-8 max-w-md w-full space-y-4 border border-slate-700">
        <h2 class="text-lg font-bold text-white">Cadastrar Novo Serviço</h2>

        <form @submit.prevent="saveService" class="space-y-3">
          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Nome do Serviço *</label>
            <input v-model="form.name" required type="text" placeholder="Ex: Barba Terapia" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Duração (minutos) *</label>
              <input v-model.number="form.durationMinutes" required type="number" min="10" step="5" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Preço (R$) *</label>
              <input v-model.number="form.price" required type="number" step="0.50" min="0" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
            </div>
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Descrição</label>
            <textarea v-model="form.description" rows="2" placeholder="O que inclui este serviço?" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white resize-none"></textarea>
          </div>

          <div class="flex justify-end gap-2 pt-3">
            <button type="button" @click="showModal = false" class="px-4 py-2 rounded-lg text-xs bg-slate-800 text-slate-300">
              Cancelar
            </button>
            <button type="submit" class="px-5 py-2 rounded-lg text-xs font-bold bg-amber-400 hover:bg-amber-300 text-slate-950">
              Salvar Serviço
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminService } from '../../services/api'

const services = ref([])
const isLoading = ref(true)
const showModal = ref(false)

const form = ref({
  name: '',
  description: '',
  durationMinutes: 30,
  price: 40.00
})

onMounted(() => {
  loadServices()
})

async function loadServices() {
  isLoading.value = true
  try {
    const res = await adminService.getServices()
    services.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

async function saveService() {
  try {
    await adminService.createService(form.value)
    showModal.value = false
    form.value = { name: '', description: '', durationMinutes: 30, price: 40.00 }
    loadServices()
  } catch (e) {
    alert(e.response?.data?.message || 'Falha ao salvar serviço.')
  }
}
</script>
