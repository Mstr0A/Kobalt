package com.a0.kobalt.shared.dispatcher

import com.a0.kobalt.shared.commands.*
import com.a0.kobalt.shared.exceptions.CommandFailedException
import com.a0.kobalt.shared.exceptions.CommandNotFoundException
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.reflect.KCallable

internal object CommandDispatcher {
    // commands list
    private val commands = mutableListOf<CommandMeta>()

    // keeping all the on ready functions to call them when starting
    private val onReadyFunctions = mutableListOf<OnReadyCall>()

    // maps
    private val aliasMap = mutableMapOf<String, CommandMeta>()
    private val slashCommandsMap = mutableMapOf<String, CommandMeta>()
    private val autoCompleteOptionsMap = mutableMapOf<String, Map<String, SlashOptionDetails>>()

    // interval task things
    private val pendingIntervalTasksList = mutableListOf<PendingIntervalTask>()
    private val intervalTaskJobs = mutableListOf<Job>()

    // time tasks things
    private val pendingTimedTasksList = mutableListOf<PendingTimedTask>()
    private val timedTaskJobs = mutableListOf<Job>()

    // logger and prefix
    private lateinit var logger: KLogger
    private lateinit var timezone: String
    private lateinit var botZoneID: ZoneId
    private var prefix: String = ""

//////////////////////////////////////////////////  Setup Functions  //////////////////////////////////////////////////

    // functions for setup
    internal fun setLogger(newLogger: KLogger?) {
        if (newLogger != null) {
            logger = newLogger
        } else {
            logger.warn { "Invalid logger provided, using default logger" }
        }
    }

    internal fun setTimeZone(newZone: String) {
        timezone = newZone
        botZoneID = ZoneId.of(timezone)
    }

    internal fun setPrefix(newPrefix: String) {
        if (newPrefix.isNotEmpty()) {
            prefix = newPrefix
        } else {
            throw IllegalArgumentException("Prefix cannot be empty")
        }
    }

//////////////////////////////////////////////////  On Ready/Shutdown Things  //////////////////////////////////////////////////

    // to be called by the bot itself once everything is ready
    internal fun callGroupOnReady() {
        // Call all onReady functions
        onReadyFunctions.forEach { call ->
            try {
                call.method.call(call.instance)
            } catch (e: Exception) {
                logger.error { "Error executing onReady function for command group ${call.instance::class.qualifiedName}: ${e.message}" }
            }
        }
    }

//////////////////////////////////////////////////  Tasks Related Things  //////////////////////////////////////////////////

    private fun executeTask(task: PendingTimedTask) {
        try {
            task.method.call(task.instance)
        } catch (e: Exception) {
            logger.error { "Error in task ${task.method.name}: ${e.message}" }
        }
    }

    fun findClosestTime(times: Array<LocalTime>, referenceTime: LocalTime): LocalTime? {
        val futureTimes = times.filter { it.isAfter(referenceTime) }

        return if (futureTimes.isNotEmpty()) {
            futureTimes.minByOrNull { Duration.between(referenceTime, it).abs() }
        } else {
            times.minByOrNull { Duration.between(referenceTime, it.plusHours(24)).abs() }
        }
    }

    // for looping task management
    internal fun startTasks(scope: CoroutineScope) {
        // Clear previous jobs to prevent accumulation
        intervalTaskJobs.clear()
        timedTaskJobs.clear()

        // Launch interval tasks
        pendingIntervalTasksList.forEach { task ->
            val job = scope.launch {
                while (isActive) {
                    try {
                        task.method.call(task.instance)
                    } catch (e: Exception) {
                        logger.error { "Error in task ${task.method.name}: ${e.message}" }
                    }
                    delay(task.delayMillis)
                }
            }
            intervalTaskJobs.add(job)
        }


        // Launch timed tasks
        pendingTimedTasksList.forEach { task ->
            val job = scope.launch {
                val taskTimes = task.times
                val closestTime = findClosestTime(taskTimes, LocalTime.now(ZoneId.of(timezone))) ?: taskTimes.first()
                val initialDelayTime = timeUntilTask(closestTime)
                delay(initialDelayTime)

                executeTask(task)

                // after that we delay to the next future time
                while (isActive) {
                    delay(
                        timeUntilTask(
                            findClosestTime(taskTimes, LocalTime.now(ZoneId.of(timezone))) ?: taskTimes.first()
                        )
                    )
                    executeTask(task)
                }
            }
            timedTaskJobs.add(job)
        }
    }

    // stopping the tasks
    internal fun stopTasks() {
        intervalTaskJobs.forEach { it.cancel() }
        intervalTaskJobs.clear()
        pendingIntervalTasksList.clear()

        timedTaskJobs.forEach { it.cancel() }
        timedTaskJobs.clear()
        pendingTimedTasksList.clear()
    }

    private fun timeUntilTask(nextTime: LocalTime): Long {
        val currentTime = ZonedDateTime.now(botZoneID)
        val targetDateTime =
            ZonedDateTime.of(currentTime.toLocalDate(), nextTime, botZoneID)

        val nextTargetDateTime = if (currentTime.isBefore(targetDateTime)) {
            targetDateTime
        } else {
            targetDateTime.plusDays(1)  // Move to the next day if the target time has already passed
        }

        return ChronoUnit.MILLIS.between(currentTime, nextTargetDateTime)
    }

//////////////////////////////////////////////////  Command Separation  //////////////////////////////////////////////////

    // command registry and helper functions for it
    fun registerCommands(classInstance: CommandGroup) {
        val methods = classInstance::class.members
        for (method in methods) {
            if (method.name == "onReady") {
                onReadyFunctions.add(
                    OnReadyCall(
                        instance = classInstance,
                        method = method
                    )
                )
            }
            method.annotations.forEach { annotation ->
                when (annotation) {
                    is Command -> {
                        registerPrefixCommand(
                            classInstance = classInstance,
                            method = method,
                            annotation = annotation
                        )
                    }

                    is SlashCommand -> {
                        registerSlashCommand(
                            classInstance = classInstance,
                            method = method,
                            annotation = annotation
                        )
                    }

                    is HybridCommand -> {
                        registerHybridCommand(
                            classInstance = classInstance,
                            method = method,
                            annotation = annotation
                        )
                    }

                    is Task -> {
                        registerTask(
                            classInstance = classInstance,
                            method = method,
                            annotation = annotation
                        )
                    }
                }
            }
        }
    }

//////////////////////////////////////////////////  Command Registry  //////////////////////////////////////////////////

    // Command Registration
    private fun registerPrefixCommand(classInstance: CommandGroup, method: KCallable<*>, annotation: Command) {
        val commandMeta = CommandMeta(
            name = annotation.name.lowercase(),
            aliases = annotation.aliases.toSet(),
            shortDescription = annotation.short,
            description = annotation.description,
            usage = annotation.usage,
            requiredPermission = annotation.requiredPermission,
            hidden = annotation.hidden,
            premissionDeniedMessage = annotation.premissionDeniedMessage,
            args = emptyList(),
            type = CommandType.PREFIX,
            method = method,
            instance = classInstance
        )
        commands.add(commandMeta)
        registerAliases(commandMeta)
        logger.debug { "Registered prefix command: ${annotation.name}" }
    }

    private fun registerSlashCommand(classInstance: CommandGroup, method: KCallable<*>, annotation: SlashCommand) {
        val args = extractSlashOptions(method)
        val commandMeta = CommandMeta(
            name = annotation.name.lowercase(),
            aliases = emptySet(),
            shortDescription = annotation.short,
            description = annotation.description,
            usage = annotation.usage,
            requiredPermission = annotation.requiredPermission,
            hidden = annotation.hidden,
            premissionDeniedMessage = annotation.premissionDeniedMessage,
            args = args,
            type = CommandType.SLASH,
            method = method,
            instance = classInstance
        )
        commands.add(commandMeta)
        slashCommandsMap[commandMeta.name.lowercase()] = commandMeta
        registerAutoCompleteOptions(commandMeta)
        logger.debug { "Registered slash command: ${annotation.name}" }
    }

    private fun registerHybridCommand(classInstance: CommandGroup, method: KCallable<*>, annotation: HybridCommand) {
        val args = extractSlashOptions(method)
        val commandMeta = CommandMeta(
            name = annotation.name.lowercase(),
            aliases = annotation.aliases.toSet(),
            shortDescription = annotation.short,
            description = annotation.description,
            usage = annotation.usage,
            requiredPermission = annotation.requiredPermission,
            hidden = annotation.hidden,
            premissionDeniedMessage = annotation.premissionDeniedMessage,
            args = args,
            type = CommandType.HYBRID,
            method = method,
            instance = classInstance
        )
        commands.add(commandMeta)
        slashCommandsMap[commandMeta.name.lowercase()] = commandMeta
        registerAliases(commandMeta)
        registerAutoCompleteOptions(commandMeta)
        logger.debug { "Registered hybrid command: ${annotation.name}" }
    }

    private fun extractSlashOptions(method: KCallable<*>): MutableList<SlashOptionDetails> {
        return method.annotations
            .filterIsInstance<SlashOption>()
            .map { option ->
                SlashOptionDetails(
                    name = option.name.lowercase(),
                    description = option.description,
                    required = option.required,
                    autoCompleteOptions = option.autoCompleteOptions.toSet(),
                    type = option.type
                )
            }
            .toMutableList()
    }

    private fun registerAliases(commandMeta: CommandMeta) {
        aliasMap["$prefix${commandMeta.name}".lowercase()] = commandMeta
        commandMeta.aliases.forEach { alias ->
            aliasMap["$prefix$alias".lowercase()] = commandMeta
        }
    }

    private fun registerAutoCompleteOptions(commandMeta: CommandMeta) {
        val optionsMap = commandMeta.args.associateBy { it.name.lowercase() }
        autoCompleteOptionsMap[commandMeta.name.lowercase()] = optionsMap
    }

//////////////////////////////////////////////////  Task Registry  //////////////////////////////////////////////////

    // Task Registration
    private fun registerTask(classInstance: CommandGroup, method: KCallable<*>, annotation: Task) {
        val timeDelayList = listOf(
            annotation.milliSeconds,
            annotation.seconds * 1000,
            annotation.minutes * 60000,
            annotation.hours * 3600000,
        )

        val runningTimes = annotation.time

        // Validate time specification
        val timeDelaySet = timeDelayList.any { it != 0L }
        val runningTimesSet = runningTimes.isNotEmpty()

        if (timeDelaySet && runningTimesSet) {
            throw IllegalArgumentException("You can't use both time delays and set time at the same time!")
        }

        if (!timeDelaySet && !runningTimesSet) {
            throw IllegalArgumentException("You must use either time delays and set time!")
        }

        if (timeDelaySet) {
            pendingIntervalTasksList.add(
                PendingIntervalTask(
                    instance = classInstance,
                    method = method,
                    delayMillis = timeDelayList.sum()
                )
            )
            logger.debug { "Registered Task: ${method.name}" }
        } else {
            val parsedTimes = runningTimes.map { time -> LocalTime.parse(time) }.toTypedArray()
            pendingTimedTasksList.add(
                PendingTimedTask(
                    instance = classInstance,
                    method = method,
                    times = parsedTimes
                )
            )
            logger.debug { "Registered Task: ${method.name}" }
        }
    }

//////////////////////////////////////////////////  Get Commands  //////////////////////////////////////////////////

    // this is just to get commands for use like syncing slash commands
    internal fun getCommands() = commands

//////////////////////////////////////////////////  Command Handling  //////////////////////////////////////////////////

    // command handling happens here
    internal fun handlePrefixCommand(event: MessageReceivedEvent) {
        val splitContent = event.message.contentRaw.split(" ")
        val commandName = splitContent[0]
        val command = findCommand(commandName) ?: throw CommandNotFoundException(commandName = commandName)

        if (command.requiredPermission != Permission.UNKNOWN &&
            event.member?.hasPermission(command.requiredPermission) == false
        ) {
            event.channel.sendMessage(command.premissionDeniedMessage).queue()
            return
        }

        try {
            command.method.call(command.instance, event)
        } catch (e: Exception) {
            throw CommandFailedException(
                commandName = command.name,
                commandGroupName = command.instance.javaClass.name,
                cause = e.cause ?: e
            )
        }
    }

    // handlePrefixCommand helper function
    private fun findCommand(commandName: String): CommandMeta? {
        return aliasMap[commandName.lowercase()]
    }

    internal fun handleSlashCommand(event: SlashCommandInteractionEvent) {
        val command = slashCommandsMap[event.name.lowercase()]
            ?: throw CommandNotFoundException(commandName = event.name)

        if (command.requiredPermission != Permission.UNKNOWN &&
            event.member?.hasPermission(command.requiredPermission) == false
        ) {
            event.channel.sendMessage(command.premissionDeniedMessage).queue()
            return
        }

        try {
            command.method.call(command.instance, event)
        } catch (e: Exception) {
            throw CommandFailedException(
                commandName = command.name,
                commandGroupName = command.instance.javaClass.name,
                cause = e.cause ?: e
            )
        }
    }

    internal fun handleAutocomplete(event: CommandAutoCompleteInteractionEvent) {
        val commandName = event.name
        val focusedOption = event.focusedOption.name

        val autoCompleteOptions = findFocusedOptionList(commandName, focusedOption)

        val filteredOptions =
            autoCompleteOptions?.filter { it.startsWith(event.focusedOption.value, ignoreCase = true) }

        if (filteredOptions.isNullOrEmpty()) {
            event.replyChoices().queue()
        } else {
            event.replyChoiceStrings(filteredOptions.toList()).queue()
        }
    }

    // handleAutocomplete helper function
    private fun findFocusedOptionList(commandName: String, focusedOption: String): Set<String>? {
        val optionsMap = autoCompleteOptionsMap[commandName.lowercase()] ?: return null
        val option = optionsMap[focusedOption.lowercase()] ?: return null
        return option.autoCompleteOptions
    }
}