<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-extrabold text-white">Barbearias & Estabelecimentos (SaaS)</h1>
        <p class="text-xs text-slate-400 mt-1">Visão geral do Super Administrador da plataforma</p>
      </div>
      <button
        @click="showModal = true"
        class="px-4 py-2 rounded-xl text-xs font-bold bg-amber-400 hover:bg-amber-300 text-slate-950 transition-all shadow-md shadow-amber-400/20"
      >
        + Nova Barbearia
      </button>
    </div>

    <div v-if="isLoading" class="text-center py-12">
      <div class="inline-block animate-spin w-8 h-8 border-4 border-amber-500 border-t-transparent rounded-full mb-2"></div>
      <p class="text-xs text-slate-400">Carregando estabelecimentos...</p>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="t in tenants"
        :key="t.id"
        class="glass-panel rounded-2xl p-5 border border-slate-800 flex flex-col justify-between"
      >
        <div>
          <div class="flex justify-between items-start mb-2">
            <h3 class="font-bold text-base text-slate-100">{{ t.name }}</h3>
            <span class="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              Ativo
            </span>
          </div>
          <p class="text-xs text-slate-400 font-mono mb-2">/b/{{ t.slug }}</p>
          <div class="text-xs text-slate-400 space-y-1">
            <p>📞 {{ t.phone || 'Sem telefone' }}</p>
            <p>✉ {{ t.email || 'Sem e-mail' }}</p>
          </div>
        </div>

        <div class="mt-4 pt-3 border-t border-slate-800/80 flex justify-between items-center text-xs">
          <span class="text-slate-500">Criado em: {{ formatDate(t.createdAt) }}</span>
          <router-link
            :to="`/b/${t.slug}`"
            target="_blank"
            class="text-amber-400 hover:underline font-semibold"
          >
            Abrir Página Pública ↗
          </router-link>
        </div>
      </div>
    </div>

    <!-- Modal Nova Barbearia -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div class="glass-panel rounded-3xl p-6 sm:p-8 max-w-lg w-full space-y-4 border border-slate-700">
        <h2 class="text-lg font-bold text-white">Criar Nova Barbearia / Salão</h2>

        <form @submit.prevent="createTenant" class="space-y-3">
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Nome Fantasia *</label>
              <input v-model="form.name" required type="text" placeholder="Barbearia Alfa" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Slug (URL) *</label>
              <input v-model="form.slug" required type="text" placeholder="barbearia-alfa" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono" />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Telefone / WhatsApp</label>
              <input v-model="form.phone" type="tel" placeholder="(11) 99999-0000" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">E-mail de Contato</label>
              <input v-model="form.email" type="email" placeholder="contato@alfa.com" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
            </div>
          </div>

          <!-- Administrador Inicial da Barbearia -->
          <div class="border-t border-slate-800 pt-3 space-y-2">
            <span class="text-xs font-bold text-amber-400">Usuário Dono / Administrador da Barbearia:</span>
            <div>
              <label class="block text-xs font-semibold text-slate-300 mb-1">Nome do Gestor *</label>
              <input v-model="form.adminName" required type="text" placeholder="Carlos Dono" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs font-semibold text-slate-300 mb-1">E-mail de Login *</label>
                <input v-model="form.adminEmail" required type="email" placeholder="carlos@alfa.com" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
              </div>
              <div>
                <label class="block text-xs font-semibold text-slate-300 mb-1">Senha Inicial *</label>
                <input v-model="form.adminPassword" required type="password" placeholder="••••••••" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white" />
              </div>
            </div>
          </div>

          <div v-if="error" class="p-2.5 rounded bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs">
            {{ error }}
          </div>

          <div class="flex justify-end gap-2 pt-3">
            <button type="button" @click="showModal = false" class="px-4 py-2 rounded-lg text-xs bg-slate-800 text-slate-300">
              Cancelar
            </button>
            <button type="submit" :disabled="isSaving" class="px-5 py-2 rounded-lg text-xs font-bold bg-amber-400 hover:bg-amber-300 text-slate-950">
              {{ isSaving ? 'Criando...' : 'Criar Barbearia' }}
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

const tenants = ref([])
const isLoading = ref(true)
const showModal = ref(false)
const isSaving = ref(false)
const error = ref(null)

const form = ref({
  name: '',
  slug: '',
  phone: '',
  email: '',
  adminName: '',
  adminEmail: '',
  adminPassword: ''
})

onMounted(() => {
  loadTenants()
})

async function loadTenants() {
  isLoading.value = true
  try {
    const res = await adminService.getTenants()
    tenants.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

async function createTenant() {
  isSaving.value = true
  error.value = null
  try {
    await adminService.createTenant(form.value)
    showModal.value = false
    form.value = { name: '', slug: '', phone: '', email: '', adminName: '', adminEmail: '', adminPassword: '' }
    loadTenants()
  } catch (e) {
    error.value = e.response?.data?.message || 'Falha ao cadastrar barbearia.'
  } finally {
    isSaving.value = false
  }
}

function formatDate(isoStr) {
  if (!isoStr) return ''
  return new Date(isoStr).toLocaleDateString('pt-BR')
}
</script>
