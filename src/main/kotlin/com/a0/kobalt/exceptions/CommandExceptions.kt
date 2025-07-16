package com.a0.kobalt.exceptions

sealed class CommandException(
    open val commandName: String,
    message: String? = null,
    cause: Throwable? = null,
) : KobaltException(commandName, message, cause)

class CommandFailed(
    override val commandName: String,
    val commandGroupName: String,
    cause: Throwable?,
) : CommandException(
        commandName,
        message = "Failed to execute '$commandName' in group '$commandGroupName'",
        cause = cause,
    )

class CommandNotFound(
    override val commandName: String,
) : CommandException(
        commandName,
        message = "Command '$commandName' not found",
    )
