<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4 py-12">
    <div class="w-full max-w-md space-y-6">
      <div class="text-center">
        <div class="w-12 h-12 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 flex items-center justify-center mx-auto mb-3 text-2xl font-bold">
          ✂
        </div>
        <h1 class="text-2xl font-extrabold text-white">Acesso Administrativo</h1>
        <p class="text-xs text-slate-400 mt-1">Entre com suas credenciais para gerenciar a barbearia</p>
      </div>

      <div class="glass-panel rounded-2xl p-6 sm:p-8 space-y-5 border border-slate-800">
        <div v-if="error" class="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs">
          {{ error }}
        </div>

        <form @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">E-mail</label>
            <input
              v-model="email"
              type="email"
              required
              placeholder="ex: admin@vintageclub.com"
              class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3.5 py-2 text-sm text-slate-100 focus:outline-none focus:border-amber-500"
            />
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-300 mb-1">Senha</label>
            <input
              v-model="password"
              type="password"
              required
              placeholder="••••••••"
              class="w-full bg-slate-900 border border-slate-700 rounded-lg px-3.5 py-2 text-sm text-slate-100 focus:outline-none focus:border-amber-500"
            />
          </div>

          <button
            type="submit"
            :disabled="isLoading"
            class="w-full py-2.5 rounded-xl font-bold text-sm bg-gradient-to-r from-amber-500 to-amber-400 hover:from-amber-400 hover:to-amber-300 text-slate-950 transition-all shadow-lg shadow-amber-500/20 disabled:opacity-50"
          >
            {{ isLoading ? 'Entrando...' : 'Entrar no Painel' }}
          </button>
        </form>

        <!-- Acesso Rápido para Demonstração (Seed Data) -->
        <div class="border-t border-slate-800/80 pt-4 space-y-2">
          <p class="text-[11px] font-semibold text-slate-400 text-center uppercase tracking-wider">
            Contas de Teste Prontas:
          </p>
          <div class="grid grid-cols-3 gap-2">
            <button
              type="button"
              @click="fillQuick('superadmin@barbershop.com', 'admin123')"
              class="px-2 py-1.5 rounded bg-slate-800/70 hover:bg-slate-700 text-[10px] text-slate-300 font-medium border border-slate-700 transition-colors"
            >
              Super Admin
            </button>
            <button
              type="button"
              @click="fillQuick('admin@vintageclub.com', 'admin123')"
              class="px-2 py-1.5 rounded bg-slate-800/70 hover:bg-slate-700 text-[10px] text-slate-300 font-medium border border-slate-700 transition-colors"
            >
              Dono Vintage
            </button>
            <button
              type="button"
              @click="fillQuick('operador@vintageclub.com', 'admin123')"
              class="px-2 py-1.5 rounded bg-slate-800/70 hover:bg-slate-700 text-[10px] text-slate-300 font-medium border border-slate-700 transition-colors"
            >
              Recepção
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const email = ref('admin@vintageclub.com')
const password = ref('admin123')
const isLoading = ref(false)
const error = ref(null)

function fillQuick(e, p) {
  email.value = e
  password.value = p
}

async function handleLogin() {
  isLoading.value = true
  error.value = null
  try {
    await auth.login(email.value, password.value)
    if (auth.isSuperAdmin) {
      router.push('/superadmin/tenants')
    } else {
      router.push('/admin/agenda')
    }
  } catch (err) {
    error.value = err.response?.data?.message || 'Falha ao autenticar. Verifique e-mail e senha.'
  } finally {
    isLoading.value = false
  }
}
</script>
