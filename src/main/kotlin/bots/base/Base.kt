package com.a0.kobalt.bots.base

import com.a0.kobalt.bots.sharded.KShardedBot
import com.a0.kobalt.bots.standard.KBot
import com.a0.kobalt.shared.commands.CommandGroup
import com.a0.kobalt.shared.commands.CommandMeta
import com.a0.kobalt.shared.dispatcher.CommandDispatcher
import com.a0.kobalt.shared.dispatcher.EventWaiter
import com.a0.kobalt.shared.exceptions.CommandException
import com.a0.kobalt.shared.exceptions.CommandFailedException
import com.a0.kobalt.shared.exceptions.CommandNotFoundException
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
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
    val botTimeZone: String
) : ListenerAdapter() {
    // Common properties
    var ownerID: String = ""
    val logger: KLogger = KotlinLogging.logger("KobaltBot")
    val waiter: EventWaiter = EventWaiter()

    // Abstract property to handle different types of management (JDA vs ShardManager)
    abstract val management: Any

    private val taskScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    protected var setShutdownHook = true
    protected var isShuttingDown = AtomicBoolean(false)

//////////////////////////////////////////////////  Event Functions  //////////////////////////////////////////////////

    override fun onReady(event: ReadyEvent) {
        CommandDispatcher.startTasks(taskScope)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        if (!event.message.contentRaw.startsWith(prefix)) return

        try {
            CommandDispatcher.handlePrefixCommand(event)
        } catch (e: CommandException) {
            this.onCommandError(event, e)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        try {
            CommandDispatcher.handleSlashCommand(event)
        } catch (e: CommandException) {
            this.onCommandError(event, e)
        }
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        CommandDispatcher.handleAutocomplete(event)
    }

    open fun onCommandError(event: Event, exception: CommandException) {
        when (exception) {
            is CommandNotFoundException -> {}

            is CommandFailedException -> {
                logger.error(exception) { "Command '${exception.commandName}' failed in group '${exception.commandGroupName}'" }
            }
        }
    }

//////////////////////////////////////////////////  Non-Event Functions  //////////////////////////////////////////////////

    init {
        // Command Dispatcher setup
        CommandDispatcher.setTimeZone(botTimeZone)
        CommandDispatcher.setLogger(logger)
        CommandDispatcher.setPrefix(prefix)
    }

    fun registerCommands(instance: CommandGroup) {
        CommandDispatcher.registerCommands(instance)
    }

    fun getCommands(): List<CommandMeta> {
        return CommandDispatcher.getCommands().toList()
    }

    inline fun <reified T : Event> waitFor(
        noinline condition: (T) -> Boolean,
        noinline action: (T) -> Unit,
        timeout: Long = -1,
        timeUnit: TimeUnit? = null,
        noinline timeoutAction: (() -> Unit)? = null
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