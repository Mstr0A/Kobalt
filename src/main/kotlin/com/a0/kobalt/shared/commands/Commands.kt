package com.a0.kobalt.shared.commands

import com.a0.kobalt.bots.base.KBase
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.OptionType

// To inherit from to make sure all commands have access to the bot
open class CommandGroup(
    bot: KBase,
) {
    open fun onReady() {}
}

// Used to define a standard command
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(
    val name: String,
    val aliases: Array<String> = [],
    val short: String = "No short description provided",
    val description: String = "No description provided",
    val usage: String = "No usage provided",
    val requiredPermission: Permission = Permission.UNKNOWN,
    val hidden: Boolean = false,
    val premissionDeniedMessage: String = "You don't have the permission to use this command",
)

// Used to define a slash command
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SlashCommand(
    val name: String,
    // Doesn't need aliases as it's not supported
    val short: String = "No short description provided",
    val description: String = "No description provided",
    val usage: String = "No usage provided",
    val requiredPermission: Permission = Permission.UNKNOWN,
    val hidden: Boolean = false,
    val premissionDeniedMessage: String = "You don't have the permission to use this command",
)

// Used to define a standard and a slash command
// Although this needs extra setup by the user
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HybridCommand(
    val name: String,
    val aliases: Array<String> = [],
    val short: String = "No short description provided",
    val description: String = "No description provided",
    val usage: String = "No usage provided",
    val requiredPermission: Permission = Permission.UNKNOWN,
    val hidden: Boolean = false,
    val premissionDeniedMessage: String = "You don't have the permission to use this command",
)

// Used to define looping tasks
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Task(
    val milliSeconds: Long = 0,
    val seconds: Long = 0,
    val minutes: Long = 0,
    val hours: Long = 0,
    val time: Array<String> = [],
)

// Used to add options to a slash or a hybrid command
@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SlashOption(
    val name: String,
    val description: String = "No description provided",
    val required: Boolean = false,
    val autoCompleteOptions: Array<String> = [],
    val type: OptionType,
)
