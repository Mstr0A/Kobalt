package com.a0.kobalt

import com.a0.kobalt.bots.standard.KBot
import com.a0.kobalt.shared.commands.Command
import com.a0.kobalt.shared.commands.CommandGroup
import com.a0.kobalt.shared.commands.SlashCommand
import com.a0.kobalt.shared.commands.SlashOption
import com.a0.kobalt.shared.exceptions.CommandException
import com.a0.kobalt.shared.exceptions.CommandFailedException
import com.a0.kobalt.shared.exceptions.CommandNotFoundException
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.concurrent.TimeUnit

class TestBot(
    token: String,
    intents: Array<GatewayIntent>,
    prefix: String,
    botTimeZone: String = "UTC",
    onReady: ((KBot) -> Unit)? = null
) : KBot(token, intents, prefix, botTimeZone, onReady) {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        println("${event.author.name}: ${event.message.contentRaw}")
        super.onMessageReceived(event)
    }

    override fun onCommandError(event: Event, exception: CommandException) {
        when (event) {
            is MessageReceivedEvent -> handlePrefixError(event, exception)
            is SlashCommandInteractionEvent -> handleSlashError(event, exception)
        }
    }

    fun handlePrefixError(event: MessageReceivedEvent, exception: CommandException) {
        when (exception) {
            is CommandNotFoundException -> {
                println("Oops, someone made a typo and wrote: ${exception.commandName}")
                event.channel.sendMessage("I don't know the command: ${exception.commandName}").queue()
            }

            is CommandFailedException -> super.onCommandError(event, exception)
        }
    }

    fun handleSlashError(event: SlashCommandInteractionEvent, exception: CommandException) {
        when (exception) {
            is CommandNotFoundException -> {
                println("I somehow couldn't find: ${exception.commandName}")
                event.reply("This slash command (${exception.commandName}) shouldn't exist").queue()
            }

            is CommandFailedException -> super.onCommandError(event, exception)
        }
    }
}


class Testing(val bot: KBot) : CommandGroup(bot) {
    @Command(
        "Prefix",
        aliases = ["thefix", "smth"],
    )
    fun prefix(event: MessageReceivedEvent) {
        event.channel.sendMessage("Hi, ${event.author.asMention}!").queue { message ->
            message.addReaction(Emoji.fromUnicode("✅")).queue()
            var userCount = 0

            fun awaitReaction() {
                bot.waitFor<MessageReactionAddEvent>(
                    condition = { reactionEvent ->
                        reactionEvent.reaction.messageId == message.id &&
                                reactionEvent.emoji.name == "✅" &&
                                reactionEvent.user?.isBot == false &&
                                reactionEvent.channel == event.channel
                    },
                    action = { reactionEvent ->
                        if (reactionEvent.userId == event.author.id) {
                            reactionEvent.reaction.removeReaction(reactionEvent.user!!).queue {
                                userCount++
                                reactionEvent.channel
                                    .sendMessage("${reactionEvent.user?.asMention} reacted with ✅! ($userCount)")
                                    .queue()
                                awaitReaction()
                            }
                        } else {
                            reactionEvent.reaction.removeReaction(reactionEvent.user!!).queue {
                                reactionEvent.channel
                                    .sendMessage("This button is not for you! >:(")
                                    .queue()
                                awaitReaction()
                            }
                        }
                    },
                    timeout = 10,
                    timeUnit = TimeUnit.SECONDS,
                    timeoutAction = {
                        event.channel.sendMessage("Time Out").queue()
                    }
                )
            }

            awaitReaction()
        }
    }

    @SlashCommand(
        name = "slashtest"
    )
    @SlashOption(
        name = "option",
        required = true,
        autoCompleteOptions = ["somethin1", "somethin2"],
        type = OptionType.STRING
    )
    fun test(event: SlashCommandInteractionEvent) {
        event.reply("${event.user.asMention} picked option ${event.getOption("option")?.asString}").queue()
    }
}

fun main() {
    val token = "YOUR_TOKEN"
    val prefix = "!"

    val intents = arrayOf(
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.MESSAGE_CONTENT,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
    )
    val bot = TestBot(
        token = token,
        intents = intents,
        prefix = prefix,
        botTimeZone = "Asia/Amman",
        onReady = { bot ->
            bot.logger.info { "${bot.management.selfUser.name} is ready!" }
            bot.management.presence.setPresence(Activity.listening("The souls of the damned"), false)
            bot.logger.warn { "Powering Up" }
        }
    )
    bot.registerCommands(Testing(bot))

    bot.ownerID = "YOUR_ID"

    bot.startBot()
}