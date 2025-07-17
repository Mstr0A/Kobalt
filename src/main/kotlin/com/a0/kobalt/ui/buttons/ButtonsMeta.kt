package com.a0.kobalt.ui.buttons

import net.dv8tion.jda.api.Permission
import kotlin.reflect.KCallable

data class ButtonMeta(
    val id: String,
    val requiredPermission: Permission,
    val permissionDeniedMessage: String,
    val method: KCallable<*>,
    val instance: Any,
)
