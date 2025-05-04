package com.a0.kobalt.bots.standard

import com.a0.kobalt.bots.base.A0Base
import com.a0.kobalt.shared.commands.CommandType
import com.a0.kobalt.shared.dispatcher.CommandDispatcher
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


open class A0Bot(
    token: String,
    val intents: Array<GatewayIntent>,
    prefix: String,
    botTimeZone: String = "UTC",
    private val onReady: ((A0Bot) -> Unit)? = null,
    private val onShutdown: ((A0Bot) -> Unit)? = null,
) : A0Base(token, intents, prefix, botTimeZone) {
    private val jdaBuilder: JDABuilder = JDABuilder.createDefault(token)
    private lateinit var builtBot: JDA
    override val management: JDA
        get() = if (::builtBot.isInitialized) builtBot
        else throw IllegalStateException("Management is not initialized")

    override var isShuttingDown: AtomicBoolean = AtomicBoolean(false)

    override fun ready() {
        onReady?.invoke(this)
    }

    override fun shutdown() {
        if (isShuttingDown.getAndSet(true)) return

        try {
            // Run shutdown callback first while connections are still alive
            onShutdown?.invoke(this)

            // Wait a moment for any messages to be sent
            Thread.sleep(1000)

            // Then proceed with JDA shutdown
            if (::builtBot.isInitialized) {
                builtBot.shutdown()
                builtBot.awaitShutdown(5, TimeUnit.SECONDS)
            }

            // Finally stop command tasks
            CommandDispatcher.stopTasks()
        } catch (e: Exception) {
            logger.error { "Error during shutdown: ${e.message}" }
            if (::builtBot.isInitialized) {
                builtBot.shutdownNow()
            }
        }
    }

    override fun startBot() {
        builtBot = jdaBuilder
            .enableIntents(intents.toSet())
            .addEventListeners(waiter)
            .addEventListeners(this)
            .build()

        builtBot.awaitReady()

        syncSlashCommands()
        CommandDispatcher.callGroupOnReady()
        ready()

        val os = System.getProperty("os.name").lowercase()
        val signals = mutableListOf("INT", "TERM").apply {
            if (!os.contains("win")) addAll(listOf("HUP", "QUIT"))
        }

        signals.forEach { sigName ->
            try {
                sun.misc.Signal.handle(sun.misc.Signal(sigName)) {
                    shutdown()
                }
            } catch (e: Exception) {
                logger.warn { "Could not register signal handler for $sigName: ${e.message}" }
            }
        }

        // This doesn't work 99.999% of the time, but it's here as a backup
        Runtime.getRuntime().addShutdownHook(Thread {
            shutdown()
        })
    }

    override fun syncSlashCommands() {
        val slashCommandsList = CommandDispatcher.getCommands()

        val commandsToAdd = slashCommandsList
            .filter { slashCommand -> slashCommand.type != CommandType.PREFIX }
            .map { slashCommand ->
                val commandWithOptions = Commands.slash(slashCommand.name, slashCommand.description)
                slashCommand.args.forEach { arg ->
                    commandWithOptions.addOption(
                        arg.type,
                        arg.name,
                        arg.description,
                        arg.required,
                        arg.autoCompleteOptions.isNotEmpty()
                    )
                }
                commandWithOptions
            }

        management.updateCommands().addCommands(commandsToAdd).queue()
    }

//////////////////////////////////////////////////  Event Functions  //////////////////////////////////////////////////

    override fun onMessageReceived(event: MessageReceivedEvent) {
        super.onMessageReceived(event)
    }
}
