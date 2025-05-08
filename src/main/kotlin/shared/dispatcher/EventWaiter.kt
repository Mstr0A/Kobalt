package com.a0.kobalt.shared.dispatcher

import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.internal.utils.Checks
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate


class EventWaiter : EventListener {
    private val waitingEvents: MutableMap<Class<*>, MutableSet<WaitingEvent<*>>> = HashMap()
    private val threadpool: ScheduledExecutorService
    private val shutdownAutomatically: Boolean

    private val log = LoggerFactory.getLogger(EventWaiter::class.java)!!

    constructor() {
        this.threadpool = Executors.newSingleThreadScheduledExecutor()
        this.shutdownAutomatically = true
    }

    constructor(threadpool: ScheduledExecutorService, shutdownAutomatically: Boolean) {
        Checks.notNull(threadpool, "ScheduledExecutorService")
        Checks.check(!threadpool.isShutdown, "Cannot construct EventWaiter with a closed ScheduledExecutorService!")
        this.threadpool = threadpool
        this.shutdownAutomatically = shutdownAutomatically
    }

    fun isShutdown(): Boolean = threadpool.isShutdown

    fun <T : Event> waitForEvent(
        type: Class<T>,
        condition: Predicate<T>,
        action: Consumer<T>
    ) {
        waitForEvent(type, condition, action, -1, null, null)
    }

    fun <T : Event> waitForEvent(
        type: Class<T>,
        condition: Predicate<T>,
        action: Consumer<T>,
        timeout: Long,
        unit: TimeUnit?,
        timeoutAction: Runnable?
    ) {
        Checks.check(
            !isShutdown(),
            "Attempted to register a WaitingEvent while the EventWaiter's threadpool was already shut down!"
        )
        Checks.notNull(type, "The provided class type")
        Checks.notNull(condition, "The provided condition predicate")
        Checks.notNull(action, "The provided action consumer")

        val we = WaitingEvent(condition, action)
        val set = waitingEvents.computeIfAbsent(type) { ConcurrentHashMap.newKeySet<WaitingEvent<*>>() }
        set.add(we)

        if (timeout > 0 && unit != null && timeoutAction != null) {
            threadpool.schedule({
                try {
                    if (set.remove(we)) {
                        timeoutAction.run()
                    }
                } catch (ex: Exception) {
                    log.error("Failed to run timeoutAction", ex)
                }
            }, timeout, unit)
        }
    }

    @SubscribeEvent
    @Suppress("UNCHECKED_CAST")
    override fun onEvent(event: GenericEvent) {
        var clazz: Class<*>? = event.javaClass
        while (clazz != null) {
            waitingEvents[clazz]?.let { set ->
                // Remove any that succeed
                set.removeIf { w ->
                    try {
                        (w as WaitingEvent<GenericEvent>).let { we ->
                            if (we.condition.test(event)) {
                                we.action.accept(event)
                                true
                            } else false
                        }
                    } catch (ex: Exception) {
                        log.error("Error while handling waiting-event action", ex)
                        false
                    }
                }
            }

            // auto-shutdown on JDA shutdown
            if (event is ShutdownEvent && shutdownAutomatically) {
                threadpool.shutdown()
            }

            clazz = clazz.superclass
        }
    }

    fun shutdown() {
        if (shutdownAutomatically) {
            throw UnsupportedOperationException("Cannot shutdown automatically-managed EventWaiter")
        }
        threadpool.shutdown()
    }

    private class WaitingEvent<T : Event>(
        val condition: Predicate<T>,
        val action: Consumer<T>
    )
}
