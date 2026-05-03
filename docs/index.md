# Kobalt

> A lightweight Kotlin wrapper for [JDA](https://github.com/discord-jda/JDA) that lets you build Discord bots without the boilerplate.

---
## Why Kobalt?

JDA is powerful, but it comes with a lot of ceremony. Command definitions live separately from their handlers, aliases require manual string matching, and something as common as waiting for a user reaction means rolling your own `ConcurrentHashMap` and `ScheduledExecutorService` - or pulling in an external library.

Kobalt fixes that. Commands, options, choices, and aliases all live together on the function that handles them. The event waiter is built in. And your bot stays readable as it grows.

---
## A Taste

**Without Kobalt:**
```kotlin
class MyListener : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        if (!event.message.contentRaw.startsWith("!")) return
        val args = event.message.contentRaw.removePrefix("!").trim().split(" ")
        when (args[0].lowercase()) {
            "greet", "hello", "hi" ->
                event.channel.sendMessage("Hey, ${event.author.asMention}!").queue()
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "color" -> {
                val color = event.getOption("color")?.asString ?: return
                event.reply("Your favorite color is $color!").queue()
            }
        }
    }
}

fun main() {
    JDABuilder.createDefault("YOUR_TOKEN")
        .enableIntents(
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.GUILD_MESSAGE_REACTIONS,
        )
        .addEventListeners(MyListener())
        .build()
        .awaitReady()
        .updateCommands()
        .addCommands(
            Commands.slash("color", "Pick your favorite color")
                .addOptions(
                    OptionData(OptionType.STRING, "color", "Your favorite color", true)
                        .addChoice("Red", "Red")
                        .addChoice("Green", "Green")
                        .addChoice("Blue", "Blue")
                )
        ).queue()
}
```

**With Kobalt:**
```kotlin
class MyGroup(bot: KBot) : CommandGroup(bot) {

    @Command("greet", aliases = ["hello", "hi"])
    fun greet(event: MessageReceivedEvent) {
        event.channel.sendMessage("Hey, ${event.author.asMention}!").queue()
    }

    @SlashCommand(name = "color", description = "Pick your favorite color")
    @SlashOption(
        name = "color",
        description = "Your favorite color",
        required = true,
        type = OptionType.STRING,
        choices = ["Red", "Green", "Blue"]
    )
    fun color(event: SlashCommandInteractionEvent) {
        val color = event.getOption("color")?.asString ?: return
        event.reply("Your favorite color is $color!").queue()
    }
}

fun main() {
    val bot = KBot(
        token = "YOUR_TOKEN",
        intents = arrayOf(
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.GUILD_MESSAGE_REACTIONS,
        ),
        prefix = "!",
        botTimeZone = "UTC",
    )
    bot.registerCommands(MyGroup(bot))
    bot.startBot()
}
```

Same result. Less noise.

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

---

## Next Steps

- [Getting Started](getting-started.md) - build your first bot in minutes
- [Bots](bots.md) - autocomplete, command introspection, and more
- [Commands](commands.md) - prefix commands, slash commands, options, and choices