package com.a0.kobalt.commands

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.time.LocalTime
import kotlin.reflect.KCallable

data class SlashOptionDetails(
    val name: String,
    val description: String,
    val required: Boolean,
    val autoCompleteOptions: Set<String>,
    val type: OptionType,
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
    val method: KCallable<*>,
    val instance: Any,
)

data class OnReadyCall(
    val instance: CommandGroup,
    val method: KCallable<*>,
)

data class PendingIntervalTask(
    val instance: CommandGroup,
    val method: KCallable<*>,
    val delayMillis: Long,
)

data class PendingTimedTask(
    val instance: CommandGroup,
    val method: KCallable<*>,
    val times: Array<LocalTime>,
)

enum class CommandType {
    PREFIX,
    SLASH,
    HYBRID,
}
