package com.a0.kobalt.shared.dispatcher

import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.SubscribeEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * A port of the JDA-Chewtils eventWaiter in Kotlin for Kobalt
 *
 * (Meant to reduce bloat and reduce compilation steps since chewtils is on a custom repo)
 *
 * Massive shout-outs to [Chew](https://github.com/Chew)
 */
class EventWaiter(
    private val threadpool: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    private val shutdownAutomatically: Boolean = true,
) : EventListener {
    private val waitingEvents = ConcurrentHashMap<Class<*>, MutableSet<WaitingEvent<*>>>()
    private val log = LoggerFactory.getLogger(EventWaiter::class.java)

    fun isShutdown(): Boolean = threadpool.isShutdown

    fun <T : Event> waitForEvent(
        type: Class<T>,
        condition: Predicate<T>,
        action: Consumer<T>,
        timeout: Long = -1,
        unit: TimeUnit? = null,
        timeoutAction: Runnable? = null,
    ) {
        require(!isShutdown()) { "Attempted to register a WaitingEvent while the EventWaiter's threadpool was already shut down!" }
        requireNotNull(type) { "The provided class type must not be null" }
        requireNotNull(condition) { "The provided condition predicate must not be null" }
        requireNotNull(action) { "The provided action consumer must not be null" }

        val waitingEvent = WaitingEvent(condition, action)
        val set = waitingEvents.computeIfAbsent(type) { ConcurrentHashMap.newKeySet() }
        set += waitingEvent

        if (timeout > 0 && unit != null && timeoutAction != null) {
            threadpool.schedule({
                try {
                    if (set.remove(waitingEvent)) timeoutAction.run()
                } catch (ex: Exception) {
                    log.error("Failed to run timeoutAction", ex)
                }
            }, timeout, unit)
        }
    }

    @SubscribeEvent
    override fun onEvent(event: GenericEvent) {
        var clazz: Class<*>? = event.javaClass
        while (clazz != null) {
            waitingEvents[clazz]?.removeIf {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val waiting = it as WaitingEvent<GenericEvent>
                    if (waiting.condition.test(event)) {
                        waiting.action.accept(event)
                        true
                    } else {
                        false
                    }
                } catch (ex: Exception) {
                    log.error("Error while handling waiting-event action", ex)
                    false
                }
            }
            if (event is ShutdownEvent && shutdownAutomatically) {
                threadpool.shutdown()
            }
            clazz = clazz.superclass
        }
    }

    fun shutdown() {
        check(!shutdownAutomatically) { "Cannot shutdown automatically-managed EventWaiter" }
        threadpool.shutdown()
    }

    private class WaitingEvent<T : Event>(
        val condition: Predicate<T>,
        val action: Consumer<T>,
    )
}
