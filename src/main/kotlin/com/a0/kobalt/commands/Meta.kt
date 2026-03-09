package com.a0.kobalt.commands

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.lang.invoke.MethodHandle
import java.time.LocalTime

data class SlashOptionDetails(
    val name: String,
    val description: String,
    val required: Boolean,
    val type: OptionType,
    val choices: Set<String> = emptySet(),
    val autoComplete: Boolean,
)

data class CommandMeta(
    val name: String,
    val aliases: Set<String>,
    val shortDescription: String,
    val description: String,
    val usage: String,
    val requiredPermission: Permission,
    val hidden: Boolean,
    val permissionDeniedMessage: String,
    val args: List<SlashOptionDetails>,
    val type: CommandType,
    val methodHandle: MethodHandle,
    val instance: Any,
)

data class OnReadyCall(
    val instance: CommandGroup,
    val methodHandle: MethodHandle,
)

data class PendingIntervalTask(
    val instance: CommandGroup,
    val methodHandle: MethodHandle,
    val methodName: String,
    val delayMillis: Long,
)

data class PendingTimedTask(
    val instance: CommandGroup,
    val methodHandle: MethodHandle,
    val methodName: String,
    val times: Array<LocalTime>,
)

enum class CommandType {
    PREFIX,
    SLASH,
    HYBRID,
}
