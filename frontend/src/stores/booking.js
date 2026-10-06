import { defineStore } from 'pinia'
import { ref } from 'vue'
import { publicBookingService } from '../services/api'

export const useBookingStore = defineStore('booking', () => {
  const currentTenant = ref(null)
  const selectedService = ref(null)
  const selectedProfessional = ref(null)
  const selectedDate = ref(new Date().toISOString().split('T')[0])
  const selectedSlot = ref(null)
  const availableSlots = ref([])
  const isLoadingSlots = ref(false)
  const lastConfirmation = ref(null)

  async function fetchTenant(slug) {
    const res = await publicBookingService.getTenantDetails(slug)
    currentTenant.value = res.data
    return res.data
  }

  async function fetchSlots(slug) {
    if (!selectedService.value || !selectedDate.value) return
    isLoadingSlots.value = true
    try {
      const profId = selectedProfessional.value ? selectedProfessional.value.id : null
      const res = await publicBookingService.getAvailability(
        slug,
        selectedService.value.id,
        selectedDate.value,
        profId
      )
      availableSlots.value = res.data
    } finally {
      isLoadingSlots.value = false
    }
  }

  async function submitBooking(slug, customerData) {
    if (!selectedService.value || !selectedSlot.value) {
      throw new Error('Serviço e horário são obrigatórios')
    }
    const payload = {
      serviceId: selectedService.value.id,
      professionalId: selectedSlot.value.professionalId,
      startTime: selectedSlot.value.startTime,
      customerName: customerData.name,
      customerEmail: customerData.email,
      customerPhone: customerData.phone,
      notes: customerData.notes
    }
    const res = await publicBookingService.createAppointment(slug, payload)
    lastConfirmation.value = res.data
    return res.data
  }

  function resetSelection() {
    selectedService.value = null
    selectedProfessional.value = null
    selectedSlot.value = null
    availableSlots.value = []
  }

  return {
    currentTenant,
    selectedService,
    selectedProfessional,
    selectedDate,
    selectedSlot,
    availableSlots,
    isLoadingSlots,
    lastConfirmation,
    fetchTenant,
    fetchSlots,
    submitBooking,
    resetSelection
  }
})
