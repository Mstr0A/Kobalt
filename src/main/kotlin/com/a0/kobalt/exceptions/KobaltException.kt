package com.a0.kobalt.exceptions

sealed class KobaltException(
    open val issueName: String,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
