<template>
  <div class="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
    <!-- Navbar Global -->
    <header class="border-b border-slate-800 bg-slate-900/80 backdrop-blur-md sticky top-0 z-40">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div class="flex items-center space-x-3">
          <router-link to="/" class="flex items-center space-x-2.5 group">
            <div class="w-9 h-9 rounded-lg bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-500 group-hover:scale-105 transition-transform">
              <span class="font-extrabold text-xl tracking-tighter">✂</span>
            </div>
            <div>
              <span class="font-extrabold text-lg tracking-tight bg-gradient-to-r from-amber-400 via-amber-200 to-amber-500 bg-clip-text text-transparent">
                BarberFlow
              </span>
              <span class="hidden sm:inline-block ml-2 text-xs uppercase px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-semibold tracking-wider">
                Pro
              </span>
            </div>
          </router-link>
        </div>

        <nav class="flex items-center space-x-3 sm:space-x-4">
          <!-- Links para usuários autenticados -->
          <template v-if="auth.isAuthenticated">
            <template v-if="auth.isSuperAdmin">
              <router-link
                to="/superadmin/tenants"
                class="text-sm font-medium text-slate-300 hover:text-amber-400 transition-colors px-3 py-1.5 rounded-lg hover:bg-slate-800/60"
                active-class="text-amber-400 bg-slate-800/80"
              >
                Barbearias (SaaS)
              </router-link>
            </template>
            <template v-else>
              <router-link
                to="/admin/agenda"
                class="text-sm font-medium text-slate-300 hover:text-amber-400 transition-colors px-3 py-1.5 rounded-lg hover:bg-slate-800/60"
                active-class="text-amber-400 bg-slate-800/80"
              >
                Agenda do Dia
              </router-link>
              <router-link
                to="/admin/profissionais"
                class="text-sm font-medium text-slate-300 hover:text-amber-400 transition-colors px-3 py-1.5 rounded-lg hover:bg-slate-800/60"
                active-class="text-amber-400 bg-slate-800/80"
              >
                Profissionais
              </router-link>
              <router-link
                to="/admin/servicos"
                class="text-sm font-medium text-slate-300 hover:text-amber-400 transition-colors px-3 py-1.5 rounded-lg hover:bg-slate-800/60"
                active-class="text-amber-400 bg-slate-800/80"
              >
                Serviços
              </router-link>
            </template>

            <!-- Botão Ver Página Pública -->
            <router-link
              v-if="auth.tenantSlug"
              :to="`/b/${auth.tenantSlug}`"
              target="_blank"
              class="hidden md:inline-flex items-center text-xs font-semibold text-amber-400 bg-amber-500/10 border border-amber-500/30 px-2.5 py-1.5 rounded-lg hover:bg-amber-500/20 transition-all"
            >
              Ver Agendamento Público ↗
            </router-link>

            <!-- Perfil & Logout -->
            <div class="flex items-center pl-2 border-l border-slate-800 space-x-3">
              <span class="text-xs text-slate-400 hidden lg:inline">
                {{ auth.user?.name }} ({{ auth.user?.role }})
              </span>
              <button
                @click="handleLogout"
                class="text-xs font-medium bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/20 px-3 py-1.5 rounded-lg transition-colors"
              >
                Sair
              </button>
            </div>
          </template>

          <!-- Links para visitantes -->
          <template v-else>
            <router-link
              to="/b/barbearia-vintage"
              class="text-sm font-medium text-slate-300 hover:text-amber-400 transition-colors px-3 py-1.5"
              active-class="text-amber-400"
            >
              Agendar Horário
            </router-link>
            <router-link
              to="/login"
              class="text-sm font-semibold text-slate-950 bg-amber-400 hover:bg-amber-300 px-4 py-1.5 rounded-lg transition-all shadow-md shadow-amber-400/20"
            >
              Acesso Painel
            </router-link>
          </template>
        </nav>
      </div>
    </header>

    <!-- Conteúdo da Página -->
    <main class="flex-1">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="border-t border-slate-900 bg-slate-950/60 py-6 text-center text-xs text-slate-500">
      <div class="max-w-7xl mx-auto px-4">
        BarberFlow &copy; 2026 • Plataforma de Agendamentos em Tempo Real • Kotlin Ktor & Vue 3 Monolith
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useAuthStore } from './stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
