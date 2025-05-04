package com.a0.kobalt.shared.exceptions

sealed class CommandException(
    open val commandName: String,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)