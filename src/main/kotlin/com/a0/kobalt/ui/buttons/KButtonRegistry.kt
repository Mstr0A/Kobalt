package com.a0.kobalt.ui.buttons

import com.a0.kobalt.exceptions.ButtonExists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object KButtonRegistry {
    private val activeButtons = ConcurrentHashMap<String, KButton>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun register(button: KButton) {
        val previous = activeButtons.putIfAbsent(button.id, button)
        if (previous != null) {
            throw ButtonExists(button.id)
        }
    }

    fun unregister(buttonID: String) {
        activeButtons.remove(buttonID)?.cancelTimeout()
    }

    fun get(buttonID: String): KButton? = activeButtons[buttonID]

    fun getAll(): Collection<KButton> = activeButtons.values

    fun clearAll() {
        activeButtons.values.forEach { it.cancelTimeout() }
        activeButtons.clear()
    }

    internal fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)
}
