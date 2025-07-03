# Kobalt

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

> **Kobalt** is a lightweight Kotlin wrapper for [JDA](https://github.com/discord-jda/JDA) that simplifies Discord bot
> development with an idiomatic Kotlin DSL.

---

> **Note: Kobalt is still in its early stages, and there are a few things to keep in mind**:
>
> * Kobalt is **not yet available on Maven Central** – please follow the [Installation](#Installation) instructions to
    get started.
> * **Documentation is currently missing**, but it's on the roadmap.
> * **JDA API support is not complete yet.** I'm actively working on it, but since this is still a **solo project**,
    progress can be a bit slow. I'm planning to build a team in the future to help move things along.
>
> If you're interested in contributing or helping out, feel free to reach out here at kobalt@ameensonjuq.com!

---

## Table of Contents

- [Features at a Glance](#features-at-a-glance)
- [Installation](#installation)
- [Usage](#usage)
- [Help](#help)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Features at a Glance

| Feature                   | Description                                                                                              |
|---------------------------|----------------------------------------------------------------------------------------------------------|
| Annotation-based Commands | Inspired by [discord.py](https://github.com/Rapptz/discord.py), create commands with simple annotations. |
| Slash Commands            | Full support for Discord’s modern slash command interface.                                               |
| Event-Driven Framework    | React to real-time events with an efficient, type-safe API.                                              |
| Smart Error Handling      | Built-in utilities to gracefully manage and log runtime errors.                                          |
| Modular & Scalable        | Easily add or remove modules as your bot grows.                                                          |
| Developer Tools           | Advanced logging, metrics, and utility classes included.                                                 |

---

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Mstr0A/Kobalt.git
   cd Kobalt
   ```

2. Publish to your local Maven cache:

   ```bash
   ./gradlew publishToMavenLocal
   ```
3. Add to your project’s gradle build file:

   ```kt
   // build.gradle.kts
   repositories {
       mavenCentral()
       mavenLocal() // Make sure to add this since it's on Local
   }

   dependencies {
       implementation("com.a0:Kobalt:0.0.1")
   }
   ```

---

## Usage

Import and initialize `KBot`, register command groups, then start your bot.

### Example Bot

```kt
class MyGroup(val bot: KBot) : CommandGroup(bot) {
    @Command(name = "hi")
    fun sayHi(event: MessageReceivedEvent) {
        event.channel.sendMessage("Hello!").queue()
    }
    
    @SlashCommand(name = "ping")
    fun ping(event: SlashCommandInteractionEvent) {
        event.reply("Pong! (${event.jda.gatewayPing}ms)").queue()
    }
}

fun main() {
    val intents = arrayOf(
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.MESSAGE_CONTENT,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
    )
    val bot = KBot(
        token = "YOUR_TOKEN",
        intents = intents,
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

Replace `YOUR_TOKEN` and `YOUR_ID` with your actual bot token and discord account ID, and adjust the prefix as desired.


---

## Help

You can get help at our [Discord Server](https://discord.gg/vva8r55sas) in the help and development channels

---

## Roadmap

* 📚 **Documentation (v0.0.2)** – Comprehensive guides and API reference
* ✅ **Full JDA Coverage (v0.0.3)** – Implement all JDA endpoints
* 👥 **Community Team (v0.0.4)** – Onboard contributors and maintainers

---

## Contributing

Contributions of any kind (code or non-code) are welcome,
we want Kobalt to be the best it can,
so any help you can provide would be appreciated.

---

## License

Released under the [Apache 2.0 License](LICENSE).