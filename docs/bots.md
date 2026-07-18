# Bots

Kobalt provides two bot classes depending on your needs: `KBot` for single-instance bots and `KShardedBot` for bots that need to scale across multiple shards.

Both are open classes, meaning you can extend them to add custom behavior - see [Extending KBot](extending-kbot.md) for more.

---

## KBot

`KBot` is the standard bot class, built on top of a single JDA instance. This is the right choice for most bots.

```kotlin
val bot = KBot(
    token = "YOUR_TOKEN",
    intents = arrayOf(
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.MESSAGE_CONTENT,
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
```

### Parameters

| Parameter                  | Type                        | Default       | Description                                                                         |
| -------------------------- | --------------------------- | ------------- | ----------------------------------------------------------------------------------- |
| `token`                    | `String`                    | required      | Your Discord bot token                                                              |
| `intents`                  | `Array<GatewayIntent>`      | required      | Gateway intents the bot will listen to                                              |
| `prefix`                   | `String`                    | required      | Prefix for prefix-based commands                                                    |
| `botTimeZone`              | `String`                    | `"UTC"`       | Time zone used internally by the bot                                                |
| `loggerName`               | `String`                    | `"KobaltBot"` | Name of the logger instance                                                         |
| `daveSessionFactory`       | `DaveSessionFactory?`       | `null`        | _(Advanced)_ DAVE session factory for E2EE voice support                            |
| `voiceDispatchInterceptor` | `VoiceDispatchInterceptor?` | `null`        | _(Advanced)_ Interceptor for voice dispatch events, used by libraries like Lavalink |
| `onReady`                  | `((KBot) -> Unit)?`         | `null`        | Callback invoked when the bot is ready                                              |
| `onShutdown`               | `((KBot) -> Unit)?`         | `null`        | Callback invoked when the bot shuts down                                            |

### Properties

| Property | Type | Description |
|---|---|---|
| `ownerID` | `String` | Your Discord account ID, used for owner-only features |
| `management` | `JDA` | The underlying JDA instance, available after `startBot()` |
| `logger` | `Logger` | The bot's logger instance |

---

## KShardedBot

`KShardedBot` is for bots that need to run across multiple shards. The API is identical to `KBot` with the addition of a `shardCount` parameter.

> **Note:** Discord requires sharding for bots in 2500 or more guilds. For smaller bots, `KBot` is sufficient.

```kotlin
val bot = KShardedBot(
    token = "YOUR_TOKEN",
    intents = arrayOf(
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.MESSAGE_CONTENT,
    ),
    shardCount = 4,
    prefix = "!",
    botTimeZone = "UTC",
    onReady = { bot ->
        bot.logger.info { "${bot.management.shards.size} shards ready!" }
    }
)

bot.registerCommands(MyGroup(bot))
bot.ownerID = "YOUR_ID"
bot.startBot()
```

### Parameters

Same as `KBot` with one addition:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `shardCount` | `Int` | `-1` | Number of shards to run. `-1` lets Discord decide automatically |
| `loggerName` | `String` | `"KobaltShardedBot"` | Name of the logger instance |

### Properties

| Property | Type | Description |
|---|---|---|
| `ownerID` | `String` | Your Discord account ID, used for owner-only features |
| `management` | `ShardManager` | The underlying ShardManager instance, available after `startBot()` |
| `logger` | `Logger` | The bot's logger instance |

---

## Shutdown Hook

Both bots register a shutdown hook automatically so the bot shuts down cleanly when the JVM exits. You can disable this if you need to manage shutdown yourself:

```kotlin
bot.setShutdownHook(false)
```

!!! warning
    If you disable the shutdown hook, make sure to call `bot.shutdown()` manually at the end of your own hook, otherwise `onShutdown` won't run and cleanup won't happen.

---

## Next Steps

- [Commands](commands.md) - define prefix, slash, and hybrid commands
- [Event Waiter](event-waiter.md) - react to user input without the extra dependency
- [Extending KBot](extending-kbot.md) - autocomplete, error handling, and command introspection
