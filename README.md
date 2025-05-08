# Kobalt

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

---

> **Note:** Kobalt is still in its early stages, and there are a few things to keep in mind:
>
> * Kobalt is **not yet available on Maven Central** – please follow the [Installation](#Installation) instructions to
    get started.
> * **Documentation is currently missing**, but it's on the roadmap.
> * **JDA API support is not complete yet.** I'm actively working on it, but since this is still a **solo project**,
    progress can be a bit slow. I'm planning to build a team in the future to help move things along.
>
> If you're interested in contributing or helping out, feel free to reach out [here](mailto:kobalt@ameensonjuq.com)!

---

## Overview

### Why Kobalt Was Made

- Kobalt was created as a Kotlin wrapper for JDA to provide a friendlier syntax and simplify the process of building
  Discord bots. It leverages the power of JDA while making it more accessible and intuitive for Kotlin developers and
  other developers coming from other languages.

### Core Features

- Basically every feature the JDA API provides will be provided here

### Ease of Use

- Kobalt offers an easy-to-follow design, making it easy to make commands and make control flow simple with our
  annotation
  based commands.

---

## Installation

Since Kobalt isn’t on Maven Central yet, you have to add it to mavenLocal and use it from there: <br>
<sub> (Don't worry, it's easy) </sub>

1. Clone the project with

    ```bash
    git clone https://github.com/Mstr0A/Kobalt.git
    ```

2. Open the project in your editor (Preferably IntelliJ)

3. Run

    ```bash
    .\gradlew publishToMavenLocal
    ```

4. You're done! All you have to do now is add it to your next project's Gradle build file

    ```kt
    repositories {
        mavenCentral()
        mavenLocal() // **Make sure to add this since it's on Local**
    }
    
    dependencies {
        implementation("com.a0:Kobalt:0.0.1")
    }
    ```

---

## Quick Start

A quick example bot to get you running quickly:

```kt
class MyGroup(val bot: KBot) : CommandGroup(bot) {
    @SlashCommand(
        name = "ping"
    )
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

## Features

- **Simple Yet Powerful**: Easily create Discord bots with a clean and intuitive syntax, combining the power of JDA and
  the simplicity you love.
- **Inspired by the Best**: Annotation-based command system inspired by the
  popular [discord.py](https://github.com/Rapptz/discord.py), making command creation effortless.
- **Event-Driven Framework**: Seamlessly respond to real-time events and interactions with an efficient and reliable
  system.
- **Fully Customizable**: Tailor your bot's behavior, commands, and settings to your liking with flexible options.
- **Smart Error Handling**: Gracefully manage errors to keep your bot running smoothly without interruptions.
- **Scalable and Modular**: Designed to grow with your bot, whether you're adding new features or scaling up.
- **Modern Slash Commands**: Full support for Discord's slash commands, providing a sleek and interactive user
  experience.
- **Developer Tools Built-In**: Includes advanced logging and utilities to streamline your development process.

---

## Contributing

Contributions are always welcome! <br>
If you want to contribute to Kobalt, make sure to base your branch off of our master branch (or a feature-branch) and
create your PR into that same branch. <br>
