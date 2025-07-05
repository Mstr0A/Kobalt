package com.a0.kobalt.bots.sharded

import com.a0.kobalt.bots.base.KBase
import com.a0.kobalt.shared.commands.CommandType
import com.a0.kobalt.shared.dispatcher.CommandDispatcher
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

open class KShardedBot(
    token: String,
    val intents: Array<GatewayIntent>,
    val shardCount: Int = -1,
    prefix: String,
    botTimeZone: String = "UTC",
    loggerName: String = "KobaltShardedBot",
    private val onReady: ((KShardedBot) -> Unit)? = null,
    private val onShutdown: ((KShardedBot) -> Unit)? = null,
) : KBase(token, intents, prefix, botTimeZone, loggerName) {
    private val jdaShardedBuilder: DefaultShardManagerBuilder = DefaultShardManagerBuilder.createDefault(token)
    private lateinit var builtBot: ShardManager
    override val management: ShardManager
        get() =
            if (::builtBot.isInitialized) {
                builtBot
            } else {
                throw IllegalStateException("Management is not initialized")
            }

    /**
     *  Disables the Kobalt shutdown hook, setting this to false could mess with onShutdown functionality,
     *  it also keeps the JDA shutdown hook disabled.
     *
     *  <b>DO NOT TOUCH UNLESS YOU KNOW WHAT YOU'RE DOING</b>
     *
     *  If you do disable the shutdown hook remember to add a call to [shutdown] at the end of your hook
     *  to ensure onShutdown and cleanup happens.
     *
     *  @param state
     *         The state of the shutdown hook
     */
    fun setShutdownHook(state: Boolean) {
        setShutdownHook = state
    }

    override fun ready() {
        onReady?.invoke(this)
    }

    /**
     * Shuts down the bot cleanly, including JDA cleanup and the onShutdown functionality.
     */
    override fun shutdown() {
        if (isShuttingDown.getAndSet(true)) return

        try {
            // Run shutdown callback first while connections are still alive
            onShutdown?.invoke(this)

            // Then proceed with JDA shutdown
            builtBot.shutdown()
            builtBot.shards.forEach { shard ->
                if (!shard.awaitShutdown(5, TimeUnit.SECONDS)) {
                    shard.shutdownNow()
                    shard.awaitShutdown()
                }
            }

            // Finally stop command tasks
            CommandDispatcher.stopTasks()
        } catch (e: Exception) {
            logger.error(e) { "Error during shutdown: ${e.message}" }
            if (::builtBot.isInitialized) {
                builtBot.shards.forEach { shard ->
                    shard.shutdownNow()
                }
            }
        }
    }

    override fun startBot() {
        Runtime.getRuntime().addShutdownHook(
            Thread({
                try {
                    shutdown()
                } catch (e: Exception) {
                    logger.warn(e) { "onShutdown failed" }
                }
            }, "Kobalt Shutdown Hook"),
        )

        builtBot =
            jdaShardedBuilder
                .setEnableShutdownHook(false) // No shutdown hook since we have our own
                .setShardsTotal(shardCount)
                .enableIntents(intents.toSet())
                .addEventListeners(waiter)
                .addEventListeners(this)
                .build()

        builtBot.shards.forEach { shard ->
            shard.awaitReady()
        }

        syncSlashCommands()
        CommandDispatcher.callGroupOnReady()
        ready()
    }

    override fun syncSlashCommands() {
        val slashCommandsList = CommandDispatcher.getCommands()

        val commandsToAdd =
            slashCommandsList
                .filter { it.type != CommandType.PREFIX }
                .map { slashCommand ->
                    val slashData = Commands.slash(slashCommand.name, slashCommand.description)

                    slashCommand.args.forEach { arg ->
                        if (arg.autoCompleteOptions.isNotEmpty()) {
                            val optionData =
                                OptionData(
                                    arg.type,
                                    arg.name,
                                    arg.description,
                                    arg.required,
                                )
                            arg.autoCompleteOptions.forEach { choice ->
                                optionData.addChoice(
                                    choice,
                                    choice.lowercase(),
                                )
                            }
                            slashData.addOptions(optionData)
                        } else {
                            slashData.addOption(
                                arg.type,
                                arg.name,
                                arg.description,
                                arg.required,
                            )
                        }
                    }
                    slashData
                }

        management.shards
            .first()
            .updateCommands()
            .addCommands(commandsToAdd)
            .queue()
    }

// ////////////////////////////////////////////////  Event Functions  //////////////////////////////////////////////////

    override fun onMessageReceived(event: MessageReceivedEvent) {
        super.onMessageReceived(event)
    }
}
