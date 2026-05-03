# Extending KBot

Both `KBot` and `KShardedBot` are open classes, meaning you can subclass them to add custom behavior. This is the recommended approach for anything beyond a basic bot.

!!! danger
    `KBase` is abstract by design - it is the internal foundation that `KBot` and `KShardedBot` are built on. Do not use or extend `KBase` directly. If it was meant to be extended by users it would be `open`, not `abstract`. Always extend `KBot` or `KShardedBot` depending on your needs.

---

## Subclassing

To extend a bot, create a class that inherits from `KBot` or `KShardedBot` and pass all required parameters up to the parent:

```kotlin
class MyBot(
    token: String,
    intents: Array<GatewayIntent>,
    prefix: String,
) : KBot(
    token = token,
    intents = intents,
    prefix = prefix,
    onReady = { bot ->
        bot.logger.info { "${bot.management.selfUser.name} is ready!" }
    }
) {
    // your overrides go here
}
```

Then use your subclass instead of `KBot` directly:

```kotlin
fun main() {
    val bot = MyBot(
        token = "YOUR_TOKEN",
        intents = arrayOf(
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT,
        ),
        prefix = "!",
    )
    bot.registerCommands(MyGroup(bot))
    bot.startBot()
}
```

---

## Handling Errors

By default Kobalt silently ignores `CommandNotFound` and `ButtonActionNotFound` errors, and logs `CommandFailed` and `ButtonActionFailed` errors. Override `onInteractionError` to handle these yourself:

```kotlin
class MyBot(...) : KBot(...) {
    override fun onInteractionError(event: Event, exception: KobaltException) {
        when (exception) {
            is CommandNotFound -> {
                // fires when a user runs a command that doesn't exist
                if (event is MessageReceivedEvent) {
                    event.channel.sendMessage("Unknown command: ${exception.commandName}").queue()
                }
            }
            is CommandFailed -> {
                // fires when a command throws an exception at runtime
                logger.error(exception) { "Command failed: ${exception.commandName}" }
                if (event is SlashCommandInteractionEvent) {
                    event.reply("Something went wrong!").setEphemeral(true).queue()
                }
            }
            is ButtonActionNotFound -> {
                // fires when a button interaction has no registered handler
            }
            is ButtonActionFailed -> {
                // fires when a button handler throws an exception at runtime
                logger.error(exception) { "Button action failed: ${exception.buttonID}" }
            }
        }
    }
}
```

!!! note
    Always call `super.onInteractionError(event, exception)` if you want to keep the default logging behavior for cases you don't handle yourself.

---

## Dynamic Autocomplete

If you have a `@SlashOption` with `autoComplete = true`, handle the suggestions by overriding `onCommandAutoCompleteInteraction`:

```kotlin
class MyBot(...) : KBot(...) {
    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        when (event.focusedOption.name) {
            "command" -> {
                val focused = event.focusedOption.value
                event.replyChoiceStrings(
                    getCommands()
                        .filter { !it.hidden }
                        .map { it.name }
                        .filter { it.startsWith(focused, ignoreCase = true) }
                ).queue()
            }
            else -> return
        }
    }
}
```

The `when` block on `event.focusedOption.name` lets you handle autocomplete for multiple options across different commands in one place.

!!! note
    Autocomplete is a power user feature. If you are using it, you are expected to be comfortable with the JDA API.

---

## Overriding Message Events

You can also override `onMessageReceived` to intercept messages before Kobalt processes them - useful for logging, filtering, or custom pre-processing:

```kotlin
class MyBot(...) : KBot(...) {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        logger.info { "${event.author.name}: ${event.message.contentRaw}" }
        super.onMessageReceived(event) // always call super to keep command handling working
    }
}
```

!!! warning
    Always call `super.onMessageReceived(event)` unless you intentionally want to stop Kobalt from processing commands.

---

## Command Introspection

`getCommands()` returns an immutable snapshot of all registered commands at runtime. This is useful for building help commands, stats, or admin panels:

```kotlin
val commands = bot.getCommands()

// get all visible commands
val visible = commands.filter { !it.hidden }

// find a specific command
val ping = commands.firstOrNull { it.name == "ping" }

// group by command group class name
val grouped = commands.groupBy { it.instance::class.simpleName }
```

The list is a copy of what the dispatcher sees internally - modifying it has no effect on the bot.

---

## Next Steps

- [Commands](commands.md) - prefix, slash, and hybrid commands
- [Event Waiter](event-waiter.md) - react to user input without the extra dependency