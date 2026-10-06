package com.barbershop.infrastructure.jobs

import com.barbershop.domain.model.Appointment
import com.barbershop.domain.model.Professional
import com.barbershop.domain.model.ServiceItem
import com.barbershop.domain.model.Tenant
import com.barbershop.domain.ports.NotificationPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

sealed interface NotificationEvent {
    data class AppointmentCreated(
        val appointment: Appointment,
        val tenant: Tenant,
        val service: ServiceItem,
        val professional: Professional
    ) : NotificationEvent

    data class AppointmentCancelled(
        val appointment: Appointment,
        val tenant: Tenant
    ) : NotificationEvent
}

class AsyncNotificationService : NotificationPort {
    private val logger = LoggerFactory.getLogger(AsyncNotificationService::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val channel = Channel<NotificationEvent>(capacity = Channel.BUFFERED)

    init {
        startWorker()
    }

    private fun startWorker() {
        scope.launch {
            for (event in channel) {
                try {
                    processEvent(event)
                } catch (e: Exception) {
                    logger.error("Falha ao processar notificação assíncrona: {}", event, e)
                }
            }
        }
    }

    private suspend fun processEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.AppointmentCreated -> {
                logger.info(
                    "📧 [NOTIFICAÇÃO ASSÍNCRONA] E-mail de confirmação para {}: " +
                    "Agendamento confirmado no '{}' com {} às {}. " +
                    "Link de cancelamento: /cancelar/{}",
                    event.appointment.customerEmail,
                    event.tenant.name,
                    event.professional.name,
                    event.appointment.startTime,
                    event.appointment.cancellationToken
                )
            }
            is NotificationEvent.AppointmentCancelled -> {
                logger.info(
                    "📧 [NOTIFICAÇÃO ASSÍNCRONA] E-mail de cancelamento para {}: " +
                    "Seu agendamento das {} no '{}' foi cancelado com sucesso.",
                    event.appointment.customerEmail,
                    event.appointment.startTime,
                    event.tenant.name
                )
            }
        }
    }

    override suspend fun sendAppointmentConfirmation(
        appointment: Appointment,
        tenant: Tenant,
        service: ServiceItem,
        professional: Professional
    ) {
        channel.send(NotificationEvent.AppointmentCreated(appointment, tenant, service, professional))
    }

    override suspend fun sendAppointmentCancellation(appointment: Appointment, tenant: Tenant) {
        channel.send(NotificationEvent.AppointmentCancelled(appointment, tenant))
    }
}
