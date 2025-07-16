package com.a0.kobalt.bots.base

import com.a0.kobalt.bots.sharded.KShardedBot
import com.a0.kobalt.bots.standard.KBot
import com.a0.kobalt.commands.CommandGroup
import com.a0.kobalt.commands.CommandMeta
import com.a0.kobalt.dispatcher.CommandDispatcher
import com.a0.kobalt.dispatcher.EventWaiter
import com.a0.kobalt.exceptions.ButtonActionFailed
import com.a0.kobalt.exceptions.ButtonActionNotFound
import com.a0.kobalt.exceptions.ButtonException
import com.a0.kobalt.exceptions.CommandException
import com.a0.kobalt.exceptions.CommandFailed
import com.a0.kobalt.exceptions.CommandNotFound
import com.a0.kobalt.exceptions.KobaltException
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import java.lang.reflect.MalformedParametersException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The base class of [KBot] and [KShardedBot]
 *
 * *CAN NOT AND SHOULD NOT BE USED IN BUILDS*
 */
abstract class KBase(
    token: String,
    intents: Array<GatewayIntent>,
    val prefix: String,
    botTimeZone: String,
    loggerName: String,
) : ListenerAdapter() {
    // Common properties
    var ownerID: String = ""
    val logger: KLogger =
        KotlinLogging.logger(
            loggerName.ifBlank {
                throw MalformedParametersException("Logger name must not me empty")
            },
        )
    val waiter: EventWaiter = EventWaiter()

    // Abstract property to handle different types of management (JDA vs ShardManager)
    abstract val management: Any

    private val taskScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    protected var setShutdownHook = true
    protected var isShuttingDown = AtomicBoolean(false)

// ////////////////////////////////////////////////  Event Functions  //////////////////////////////////////////////////

    override fun onReady(event: ReadyEvent) {
        CommandDispatcher.startTasks(taskScope)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        if (!event.message.contentRaw.startsWith(prefix)) return

        try {
            CommandDispatcher.handlePrefixCommand(event)
        } catch (e: CommandException) {
            this.onInteractionError(event, e)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        try {
            CommandDispatcher.handleSlashCommand(event)
        } catch (e: CommandException) {
            this.onInteractionError(event, e)
        }
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        CommandDispatcher.handleAutocomplete(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        try {
            CommandDispatcher.handleButtonInteraction(event)
        } catch (e: ButtonException) {
            this.onInteractionError(event, e)
        }
    }

    open fun onInteractionError(
        event: Event,
        exception: KobaltException,
    ) {
        when (exception) {
            is CommandNotFound -> {}
            is ButtonActionNotFound -> {}

            is CommandFailed -> {
                logger.error(exception) { "Command '${exception.commandName}' failed in group '${exception.commandGroupName}'" }
            }

            is ButtonActionFailed -> {
                logger.error(exception) { "Button action with ID '${exception.buttonID}' failed" }
            }
        }
    }

// ////////////////////////////////////////////////  Non-Event Functions  //////////////////////////////////////////////////

    init {
        // Command Dispatcher setup
        CommandDispatcher.setTimeZone(botTimeZone)
        CommandDispatcher.setLogger(logger)
        CommandDispatcher.setPrefix(prefix)
    }

    fun registerCommands(instance: CommandGroup) {
        CommandDispatcher.registerCommands(instance)
    }

    fun getCommands(): List<CommandMeta> = CommandDispatcher.getCommands().toList()

    inline fun <reified T : Event> waitFor(
        noinline condition: (T) -> Boolean,
        noinline action: (T) -> Unit,
        timeout: Long = -1,
        timeUnit: TimeUnit? = null,
        noinline timeoutAction: (() -> Unit)? = null,
    ) {
        waiter.waitForEvent(T::class.java, condition, action, timeout, timeUnit, timeoutAction)
    }

    // Abstract methods that children must implement
    abstract fun startBot()

    abstract fun shutdown()

    // These are protected because they should only be used within the subclass
    protected abstract fun syncSlashCommands()

    protected abstract fun ready()
}
