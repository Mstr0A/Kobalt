# Changelog

> This changelog starts from version **0.0.36**. For changes prior to this version, refer to the [GitHub commit history](https://github.com/Mstr0A/Kobalt/commits/main).

---

## [0.0.36]

### Added
- User-installed app support — `@Command`, `@SlashCommand`, and `@HybridCommand` now accept `integrationTypes` and `contextTypes` parameters, allowing commands to be used outside of guilds in DMs and user-installed contexts
- `getCommandsToAdd()` extracted from `syncSlashCommands` for cleaner internal logic

### Changed
- Bumped dependencies:
    - Kotlin `2.3.10` → `2.3.20`
    - JDA `6.3.1` → `6.5.0`
    - Logback `1.5.32` → `1.5.38`
    - kotlin-logging `8.0.01` → `8.0.4`
    - kotlinx-coroutines `1.10.2` → `1.11.0`
