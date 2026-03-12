@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.a0.kobalt.ui.buttons

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

internal object KButtonRegistry {
    private val activeButtons = ConcurrentHashMap<String, KButton>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun register(button: KButton) {
        activeButtons[button.id] = button
    }

    fun unregister(button: KButton) {
        activeButtons.remove(button.id)?.cancelTimeout()
    }

    fun get(buttonID: String): KButton? = activeButtons[buttonID]

    fun getAll(): Collection<KButton> = activeButtons.values

    fun clearAll() {
        activeButtons.values.forEach { it.cancelTimeout() }
        activeButtons.clear()
    }

    internal fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)
}
