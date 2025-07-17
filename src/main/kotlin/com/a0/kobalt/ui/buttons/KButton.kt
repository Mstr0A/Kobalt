package com.a0.kobalt.ui.buttons

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

/*
Instead of the user using normal JDA buttons
the user can use this so it works with all the features they could need
 */
abstract class KButton(
    val id: String,
    val label: String,
    val emoji: Emoji? = null,
    val url: String? = null,
    val disabled: Boolean = false,
    val ownerId: String?,
    val timeoutMillis: Long,
    val style: ButtonStyle = ButtonStyle.PRIMARY,
) {
    val button: Button = when {
        url != null -> Button.link(url, label)
        label.isNotBlank() -> Button.of(style, id, label)
        emoji != null -> Button.of(style, id, emoji)
        else -> throw IllegalArgumentException("Either label or emoji must be provided")
    }.apply {
        withDisabled(disabled)
    }

    private var timeoutJob: Job? = null

    init {
        KButtonRegistry.register(this)
        startTimeout()
    }

    fun toButton(): Button = button

    private fun startTimeout() {
        timeoutJob?.cancel()
        timeoutJob =
            KButtonRegistry.launch {
                delay(timeoutMillis)
                onTimeout()
                KButtonRegistry.unregister(id)
            }
    }

    internal fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    abstract fun onClick(event: ButtonInteractionEvent)

    open fun onTimeout() {
        // default: do nothing
    }
}
