package com.a0.kobalt

fun main() {
    val jdaVersion = "5.3.0" // to make changing the JDA version easier
    val utilsVersion = "2.1" // to make changing the utils version easier
    val loggingVersion = "7.0.4" // to make changing the logging version easier
    val loggingAPIVersion = "2.0.17" // to make changing the logging API version easier
    val logbackVersion = "1.5.16" // to make changing the logging API version easier

    println(
        """
    ##    ##  #######  ########     ###    ##       ######## 
    ##   ##  ##     ## ##     ##   ## ##   ##          ##    
    ##  ##   ##     ## ##     ##  ##   ##  ##          ##    
    #####    ##     ## ########  ##     ## ##          ##    
    ##  ##   ##     ## ##     ## ######### ##          ##    
    ##   ##  ##     ## ##     ## ##     ## ##          ##    
    ##    ##  #######  ########  ##     ## ########    ##    
    """.trimIndent()
    )
    println()
    println("Dependencies:")
    println("  | JDA: $jdaVersion")
    println("  |-> GitHub: https://github.com/discord-jda/JDA")
    println("  | JDA-Chewtils: $utilsVersion")
    println("  |-> GitHub: https://github.com/Chew/JDA-Chewtils")
    println("  | slf4j-api: $loggingVersion")
    println("  |-> GitHub: https://github.com/qos-ch/slf4j")
    println("  | logback-classic: $loggingAPIVersion")
    println("  |-> GitHub: https://github.com/qos-ch/logback")
    println("  | kotlin-logging-jvm: $logbackVersion")
    println("  |-> GitHub: https://github.com/oshai/kotlin-logging")
}