# Commands

Kobalt supports three command types: prefix commands, slash commands, and hybrid commands. All are defined as annotated functions inside a `CommandGroup` class.

---

## CommandGroup

All commands must live inside a class that extends `CommandGroup`:

```kotlin
class MyGroup(val bot: KBot) : CommandGroup(bot) {
    // your commands go here
}
```

Register your groups before starting the bot:

```kotlin
bot.registerCommands(MyGroup(bot))
bot.startBot()
```

You can register as many groups as you want. It is recommended to organize them by feature - one group for music commands, one for moderation, and so on.

`CommandGroup` also exposes an `onReady()` callback that runs once the bot is ready:

```kotlin
class MyGroup(val bot: KBot) : CommandGroup(bot) {
    override fun onReady() {
        bot.logger.info { "MyGroup is ready!" }
    }
}
```

---

## Prefix Commands

Prefix commands are triggered by a message starting with the bot's prefix.

```kotlin
@Command(
    name = "greet",
    aliases = ["hello", "hi"],
    short = "Greets the user",
    description = "Sends a greeting message to the user who ran the command",
    usage = "!greet",
)
fun greet(event: MessageReceivedEvent) {
    event.channel.sendMessage("Hey, ${event.author.asMention}!").queue()
}
```

### Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | required | The command name |
| `aliases` | `Array<String>` | `[]` | Alternative names for the command |
| `short` | `String` | `"No short description provided"` | Brief description, used in help commands |
| `description` | `String` | `"No description provided"` | Full description |
| `usage` | `String` | `"No usage provided"` | Usage string |
| `requiredPermission` | `Permission` | `Permission.UNKNOWN` | Discord permission required to run the command |
| `permissionDeniedMessage` | `String` | `"You don't have the permission to use this command"` | Message sent when permission check fails |
| `hidden` | `Boolean` | `false` | Hides the command from `getCommands()` |
| `integrationTypes` | `Array<IntegrationType>` | `[IntegrationType.GUILD_INSTALL]` | Where the bot can be installed — see [User-Installed Apps](#user-installed-apps) |
| `contextTypes` | `Array<InteractionContextType>` | `[InteractionContextType.GUILD]` | Where the command can be used — see [User-Installed Apps](#user-installed-apps) |

> **Note:** `integrationTypes` and `contextTypes` have no effect on prefix commands. Prefix commands only work in guilds where the bot is installed. For user-installed app support, use slash or hybrid commands.

---

## Slash Commands

Slash commands are registered and handled automatically by Kobalt.

```kotlin
@SlashCommand(
    name = "ping",
    short = "Check the bot's latency",
    description = "Replies with the bot's current gateway ping",
)
fun ping(event: SlashCommandInteractionEvent) {
    event.reply("Pong! (${event.jda.gatewayPing}ms)").queue()
}
```

Slash commands support the same parameters as prefix commands, minus `aliases` - Discord does not support aliases for slash commands.

### Adding Options

Use `@SlashOption` to add options to a slash command. It is repeatable, so you can stack multiple options on the same function:

```kotlin
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
```

### SlashOption Parameters

| Parameter      | Type            | Default                     | Description                                                            |
| -------------- | --------------- | --------------------------- | ---------------------------------------------------------------------- |
| `name`         | `String`        | required                    | The option name                                                        |
| `description`  | `String`        | `"No description provided"` | Option description shown in Discord                                    |
| `required`     | `Boolean`       | `false`                     | Whether the option is required                                         |
| `type`         | `OptionType`    | required                    | The JDA option type                                                    |
| `choices`      | `Array<String>` | `[]`                        | Static choices shown in Discord                                        |
| `autoComplete` | `Boolean`       | `false`                     | Enables dynamic autocomplete - see [Extending KBot](extending-kbot.md) |

---

## Hybrid Commands

Hybrid commands respond to both a prefix message and a slash command. Since both event types carry different data, you handle them separately inside the function using a `when` block:

```kotlin
@HybridCommand(
    name = "greet",
    aliases = ["hello", "hi"],
    short = "Greets the user",
    description = "Sends a greeting message to the user who ran the command",
)
fun greet(event: GenericEvent) {
    when (event) {
        is SlashCommandInteractionEvent -> {
            event.reply("Hey, ${event.user.asMention}!").queue()
        }
        is MessageReceivedEvent -> {
            event.channel.sendMessage("Hey, ${event.author.asMention}!").queue()
        }
    }
}
```

For more complex commands you may want to extract shared logic into a separate function to avoid duplication:

```kotlin
@HybridCommand(name = "info", description = "Shows some info")
fun info(event: GenericEvent) {
    when (event) {
        is SlashCommandInteractionEvent -> event.reply(getInfo()).queue()
        is MessageReceivedEvent -> event.channel.sendMessage(getInfo()).queue()
    }
}

private fun getInfo(): String = "Some info here"
```

Hybrid commands support the same parameters as prefix commands including `aliases`.

---

## User-Installed Apps

By default, all commands are only available in guilds where the bot is installed. You can expand this using `integrationTypes` and `contextTypes` to support user-installed apps — bots installed directly to a user's account rather than a server.

```kotlin
@SlashCommand(
    name = "ping",
    description = "Check the bot's latency",
    integrationTypes = [IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL],
    contextTypes = [InteractionContextType.GUILD, InteractionContextType.BOT_DM, InteractionContextType.PRIVATE_CHANNEL],
)
fun ping(event: SlashCommandInteractionEvent) {
    event.reply("Pong! (${event.jda.gatewayPing}ms)").queue()
}
```

### integrationTypes

Controls where the bot can be installed:

| Value | Description |
|---|---|
| `IntegrationType.GUILD_INSTALL` | Bot is installed to a server (default) |
| `IntegrationType.USER_INSTALL` | Bot is installed to a user's account |

### contextTypes

Controls where the command can be used:

| Value | Description |
|---|---|
| `InteractionContextType.GUILD` | Inside a server (default) |
| `InteractionContextType.BOT_DM` | In a DM with the bot |
| `InteractionContextType.PRIVATE_CHANNEL` | In a DM or group DM between users |

### Traps to Avoid

When writing commands that run outside of guilds:

- **Do not** use `.asTextChannel()` — it will crash in DMs. Use `MessageChannel` or `.channel` instead.
- **Always** check `event.isFromGuild()` before accessing `.guild` or `.member` — they are null outside of guilds.
- Bulk or destructive actions like history retrieval or mass deletes will fail with `403 Forbidden` if the bot is not a guild member.

> **Note:** `integrationTypes` and `contextTypes` only apply to slash and hybrid commands. Prefix commands are not supported in user-installed contexts.

---

## Tasks

`@Task` lets you define a repeating background task directly in a `CommandGroup`:

```kotlin
@Task(minutes = 5)
fun updateStatus() {
    bot.management.presence.setPresence(Activity.listening("something"), false)
}
```

You can mix and combine time units:

```kotlin
@Task(hours = 1, minutes = 30)
fun doSomething() {
    // runs every 1.5 hours
}
```

### Parameters

| Parameter | Type | Default |
|---|---|---|
| `milliSeconds` | `Long` | `0` |
| `seconds` | `Long` | `0` |
| `minutes` | `Long` | `0` |
| `hours` | `Long` | `0` |

---

## Permissions

Both `@Command` and `@SlashCommand` support a `requiredPermission` parameter that checks whether the user has the specified Discord permission before running the command:

```kotlin
@Command(
    name = "ban",
    requiredPermission = Permission.BAN_MEMBERS,
    permissionDeniedMessage = "You need the Ban Members permission to use this command",
)
fun ban(event: MessageReceivedEvent) {
    // only runs if the user has BAN_MEMBERS
}
```

---

## Hidden Commands

Setting `hidden = true` marks a command in `getCommands()` so you can filter it out where needed, useful for owner-only or internal commands you don't want showing up in help menus:

```kotlin
@Command(name = "debug", hidden = true)
fun debug(event: MessageReceivedEvent) {
    // still appears in getCommands() but marked as hidden
}
```

To exclude hidden commands from a help menu for example:

```kotlin
val visible = bot.getCommands().filter { !it.hidden }
```
---

## Next Steps

- [Event Waiter](event-waiter.md) - react to user input without the extra dependency
- [Extending KBot](extending-kbot.md) - autocomplete, error handling, and command introspection
