# Kobalt

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

---

> **Note:** Kobalt is in early stages and there are still some things to keep note of:
> * Kobalt is not on Maven Central as of now
> * There is still no documentation
> * Kobalt does not cover the JDA API completely, that's still getting worked on, but it will be slow as I'm a one-man
    team

---

## Installation

Since Kobalt isn’t on Maven Central yet, you have to compile it and add it manually to your project

---

## Quick Start

A quick example bot to get you running quickly:

```kt
import com.a0.kobalt.bots.standard.A0Bot
import com.a0.kobalt.shared.commands.CommandGroup
import com.a0.kobalt.shared.commands.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.GatewayIntent


class MyGroup(val bot: A0Bot) : CommandGroup(bot) {
    @SlashCommand(
        name = "ping"
    )
    fun ping(event: SlashCommandInteractionEvent) {
        event.reply("Pong! (${event.jda.gatewayPing}ms)").queue()
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
    val bot = A0Bot(
        token = token,
        intents = intents,
        prefix = prefix,
        botTimeZone = "Asia/Amman",
        onReady = { bot ->
            bot.logger.info { "${bot.management.selfUser.name} is ready!" }
        }
    )
    bot.registerCommands(MyGroup(bot))

    bot.ownerID = "YOUR_ID"

    bot.startBot()
}
```

Replace `YOUR_BOT_TOKEN` with your actual token and adjust the prefix as desired.

---

## Features

* **An easy to read and write syntax**: The power of JDA with a really simple to work with syntax
* **Annotation based commands**: Make commands of any type with an easy annotation based system (inspired
  by [discord.py](https://github.com/Rapptz/discord.py))

---

## Contributing

Contributions are always welcome! <br>
If you want to contribute to Kobalt, make sure to base your branch off of our master branch (or a feature-branch) and
create your PR into that same branch. <br>
