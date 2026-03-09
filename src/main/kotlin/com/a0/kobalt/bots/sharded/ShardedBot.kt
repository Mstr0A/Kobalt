package com.a0.kobalt.bots.sharded

import com.a0.kobalt.bots.base.KBase
import com.a0.kobalt.commands.CommandType
import com.a0.kobalt.dispatcher.CommandDispatcher
import net.dv8tion.jda.api.audio.AudioModuleConfig
import net.dv8tion.jda.api.audio.dave.DaveSessionFactory
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

/**
 * A Discord bot that supports multiple shards.
 *
 * Provides an easy interface for handling commands, events, and shutdown
 * across multiple shards. For more control, extend this class and override
 * the provided open functions.
 *
 * @param token [String] The Discord bot token.
 * @param intents [Array] of [GatewayIntent] the bot will listen to.
 * @param shardCount [Int] Number of shards to run. Defaults to -1 (automatic).
 * @param prefix [String] Prefix for prefix-based commands.
 * @param botTimeZone [String] Time zone for the bot. Defaults to "UTC".
 * @param loggerName [String] Name of the logger. Defaults to "KobaltShardedBot".
 * @param daveSessionFactory [DaveSessionFactory]? Optional DAVE session factory for E2EE support.
 * @param voiceDispatchInterceptor [VoiceDispatchInterceptor]? Optional interceptor for voice dispatch events.
 * @param onReady [(KShardedBot) -> Unit]? Optional callback invoked when the bot is ready.
 * @param onShutdown [(KShardedBot) -> Unit]? Optional callback invoked when the bot shuts down.
 */
open class KShardedBot(
    token: String,
    val intents: Array<GatewayIntent>,
    val shardCount: Int = -1,
    prefix: String,
    botTimeZone: String = "UTC",
    loggerName: String = "KobaltShardedBot",
    val daveSessionFactory: DaveSessionFactory? = null,
    val voiceDispatchInterceptor: VoiceDispatchInterceptor? = null,
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

    /**
     * Starts your bot
     */
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
                .enableIntents(intents.toSet())
                .addEventListeners(waiter)
                .addEventListeners(this)
                .apply {
                    // We set the DAVE session factory the user provided if a user wants to use the DAVE protocol
                    if (daveSessionFactory != null) {
                        setAudioModuleConfig(
                            AudioModuleConfig()
                                .withDaveSessionFactory(daveSessionFactory),
                        )
                    }
                }.apply {
                    // We set the Voice interceptor the user provided if a user wants to use something like lavalink
                    voiceDispatchInterceptor?.let {
                        setVoiceDispatchInterceptor(it)
                    }
                }.build()

        builtBot.shards.forEach { shard ->
            shard.awaitReady()
        }

        syncSlashCommands()
        CommandDispatcher.callGroupOnReady()
        ready()
    }

    /**
     * Registers and syncs all the slash commands available
     */
    override fun syncSlashCommands() {
        val slashCommandsList = CommandDispatcher.getCommands()

        val commandsToAdd =
            slashCommandsList
                .filter { it.type != CommandType.PREFIX }
                .map { slashCommand ->
                    val slashData = Commands.slash(slashCommand.name, slashCommand.description)

                    slashCommand.args.forEach { arg ->
                        val optionData = OptionData(arg.type, arg.name, arg.description, arg.required)

                        when {
                            arg.choices.isNotEmpty() -> arg.choices.forEach { optionData.addChoice(it, it) }
                            arg.autoComplete -> optionData.setAutoComplete(true)
                        }
                        slashData.addOptions(optionData)
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

    /**
     * Handles messages received from Discord (ignores bots automatically).
     */
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        super.onMessageReceived(event)
    }
}
