package com.a0.kobalt.shared.exceptions

class CommandNotFoundException(
    override val commandName: String
) : CommandException(
    commandName,
    message = "Command '$commandName' not found"
)