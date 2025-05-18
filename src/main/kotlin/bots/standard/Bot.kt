package com.a0.kobalt.bots.standard

import com.a0.kobalt.bots.base.KBase
import com.a0.kobalt.shared.commands.CommandType
import com.a0.kobalt.shared.dispatcher.CommandDispatcher
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.concurrent.TimeUnit


open class KBot(
    token: String,
    val intents: Array<GatewayIntent>,
    prefix: String,
    botTimeZone: String = "UTC",
    private val onReady: ((KBot) -> Unit)? = null,
    private val onShutdown: ((KBot) -> Unit)? = null,
) : KBase(token, intents, prefix, botTimeZone) {
    private val jdaBuilder: JDABuilder = JDABuilder.createDefault(token)
    private lateinit var builtBot: JDA
    override val management: JDA
        get() = if (::builtBot.isInitialized) builtBot
        else throw IllegalStateException("Management is not initialized")

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
            logger.error(e) { "Error during shutdown: ${e.message}" }
            if (::builtBot.isInitialized) {
                builtBot.shutdownNow()
            }
        }
    }

    override fun startBot() {
        Runtime.getRuntime().addShutdownHook(Thread({
            try {
                shutdown()
            } catch (e: Exception) {
                logger.warn(e) { "onShutdown failed" }
            }
        }, "Kobalt Shutdown Hook"))

        builtBot = jdaBuilder
            .setEnableShutdownHook(false) // No shutdown hook since we have our own
            .enableIntents(intents.toSet())
            .addEventListeners(waiter)
            .addEventListeners(this)
            .build()
            .also { it.awaitReady() }

        syncSlashCommands()
        CommandDispatcher.callGroupOnReady()
        ready()
    }

    override fun syncSlashCommands() {
        val slashCommandsList = CommandDispatcher.getCommands()

        val commandsToAdd = slashCommandsList
            .filter { it.type != CommandType.PREFIX }
            .map { slashCommand ->
                val slashData = Commands.slash(slashCommand.name, slashCommand.description)

                slashCommand.args.forEach { arg ->
                    if (arg.autoCompleteOptions.isNotEmpty()) {
                        val optionData = OptionData(
                            arg.type,
                            arg.name,
                            arg.description,
                            arg.required
                        )
                        arg.autoCompleteOptions.forEach { choice ->
                            optionData.addChoice(
                                choice,
                                choice.lowercase()
                            )
                        }
                        slashData.addOptions(optionData)
                    } else {
                        slashData.addOption(
                            arg.type,
                            arg.name,
                            arg.description,
                            arg.required
                        )
                    }
                }
                slashData
            }

        management.updateCommands()
            .addCommands(commandsToAdd)
            .queue()
    }

//////////////////////////////////////////////////  Event Functions  //////////////////////////////////////////////////

    override fun onMessageReceived(event: MessageReceivedEvent) {
        super.onMessageReceived(event)
    }
}
