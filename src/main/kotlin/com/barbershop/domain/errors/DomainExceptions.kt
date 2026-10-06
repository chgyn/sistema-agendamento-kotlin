package com.barbershop.domain.errors

sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class EntityNotFoundException(message: String) : DomainException(message)

class ScheduleConflictException(
    message: String = "O horário selecionado não está mais disponível. Por favor, escolha outro horário."
) : DomainException(message)

class InvalidCancellationException(
    message: String = "Código de cancelamento inválido ou agendamento já finalizado/cancelado."
) : DomainException(message)

class UnauthorizedBusinessException(
    message: String = "Acesso negado para esta operação ou tenant."
) : DomainException(message)

class ValidationException(message: String) : DomainException(message)
