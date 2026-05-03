# Getting Started

This page will get you from zero to a running Kobalt bot.

---

## Prerequisites

- JDK 11 or higher
- A Discord bot token - if you don't have one, create an application at the [Discord Developer Portal](https://discord.com/developers/applications)

---

## Installation

Add JitPack to your repositories and Kobalt to your dependencies:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Mstr0A:Kobalt:VERSION")
}
```

Replace `VERSION` with any release available on [JitPack](https://jitpack.io/#Mstr0A/Kobalt).

> **Note:** Kobalt is not yet available on Maven Central.

---

## Your First Bot

A Kobalt bot has three parts: a `CommandGroup` with your commands, a `KBot` instance, and a `main` to wire them together.

```kotlin
class MyGroup(val bot: KBot) : CommandGroup(bot) {

    @Command("hi")
    fun sayHi(event: MessageReceivedEvent) {
        event.channel.sendMessage("Hello, ${event.author.asMention}!").queue()
    }

    @SlashCommand(name = "ping", description = "Check the bot's latency")
    fun ping(event: SlashCommandInteractionEvent) {
        event.reply("Pong! (${event.jda.gatewayPing}ms)").queue()
    }
}

fun main() {
    val bot = KBot(
        token = "YOUR_TOKEN",
        intents = arrayOf(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.GUILD_MESSAGES,
        ),
        prefix = "!",
        botTimeZone = "UTC",
        onReady = { bot ->
            bot.logger.info { "${bot.management.selfUser.name} is ready!" }
        }
    )
    bot.registerCommands(MyGroup(bot))
    bot.ownerID = "YOUR_ID"
    bot.startBot()
}
```

Replace `YOUR_TOKEN` and `YOUR_ID` with your actual bot token and Discord account ID.

---

## What Just Happened

- `CommandGroup` is where your commands live. You can have as many groups as you want, registered via `bot.registerCommands()`
- `@Command` handles prefix commands - the bot will respond to `!hi` with the prefix defined in `KBot`
- `@SlashCommand` registers and handles Discord slash commands automatically
- `onReady` is an optional callback that runs once the bot is connected and ready
- `ownerID` stores your account ID as the owner, in case you want special owner-only features

---

## Need Help?

Join the [Kobalt Discord Server](https://discord.gg/vva8r55sas) in the help and development channels.

---

## Next Steps

- [Bots](bots.md) - autocomplete, command introspection, and more
- [Commands](commands.md) - prefix commands, aliases, slash commands, options, and choices