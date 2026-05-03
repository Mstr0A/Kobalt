# Event Waiter

The event waiter lets you pause and wait for a specific Discord event to occur, with an optional timeout. It is built into Kobalt - no extra dependencies needed.

---

## Basic Usage

Call `bot.waitFor<T>()` from anywhere you have access to the bot, passing the event type as a type parameter:

```kotlin
bot.waitFor<MessageReceivedEvent>(
    condition = { event -> event.author.id == "SOME_USER_ID" },
    action = { event -> event.channel.sendMessage("Got your message!").queue() },
    timeout = 30,
    timeUnit = TimeUnit.SECONDS,
    timeoutAction = { /* handle timeout */ }
)
```

---

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `condition` | `(T) -> Boolean` | required | A predicate that filters events - only events where this returns `true` will trigger the action |
| `action` | `(T) -> Unit` | required | The callback that runs when a matching event is received |
| `timeout` | `Long` | `-1` | How long to wait before timing out. `-1` means wait forever |
| `timeUnit` | `TimeUnit?` | `null` | The time unit for the timeout - required if `timeout` is set |
| `timeoutAction` | `(() -> Unit)?` | `null` | Callback that runs if the timeout expires before a matching event is received |

!!! note
    If `timeout` is set, both `timeUnit` and `timeoutAction` must also be provided, otherwise the timeout will not trigger.

---

## How It Works

The waiter listens to all incoming events and checks them against registered conditions. Once a matching event is found the action runs and the waiter is removed. If the timeout expires before a match is found, the timeout action runs instead.

Each `waitFor` call is single-use - it fires once then cleans itself up.

---

## Single-Use vs Persistent Listening

If you need something that keeps listening until a timeout - like a button that stays active - call `waitFor` again inside the action:

```kotlin
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
            },
        )
    }

    awaitReaction()
}
```

!!! warning
    Always include a timeout on persistent waiters - without one they will listen forever. Also note that because the waiter is built on Java's threading model, the user experience for rapid interactions may feel slightly off in some edge cases. This is a known artifact of the underlying implementation.

---

## Credits

The event waiter is a Kotlin port of the [JDA-Chewtils](https://github.com/Chew/JDA-Chewtils) event waiter by [Chew](https://github.com/Chew). It was ported directly into Kobalt for two reasons - to reduce bloat by avoiding an extra dependency, and because Chewtils lives on a separate private repository which would add an extra step to your build setup.

Massive shout-outs to Chew for the original implementation.

---

## Next Steps

- [Extending KBot](extending-kbot.md) - autocomplete, error handling, and command introspection