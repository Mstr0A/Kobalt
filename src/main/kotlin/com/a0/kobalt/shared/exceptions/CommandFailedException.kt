package com.a0.kobalt.shared.exceptions

class CommandFailedException(
    override val commandName: String,
    val commandGroupName: String,
    cause: Throwable?
) : CommandException(
    commandName,
    message = "Failed to execute '$commandName' in group '$commandGroupName'",
    cause = cause
)