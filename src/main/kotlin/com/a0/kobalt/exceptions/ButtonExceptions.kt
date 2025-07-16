package com.a0.kobalt.exceptions

sealed class ButtonException(
    open val buttonID: String,
    message: String? = null,
    cause: Throwable? = null,
) : KobaltException(buttonID, message, cause)

class ButtonActionNotFound(
    override val buttonID: String,
) : ButtonException(
        buttonID,
        message = "Button action with ID '$buttonID' not found",
    )

class ButtonActionFailed(
    override val buttonID: String,
    cause: Throwable?,
) : ButtonException(
        buttonID,
        message = "Failed to run button action with ID '$buttonID'",
        cause = cause,
    )

class ButtonExists(
    override val buttonID: String,
) : ButtonException(
        buttonID,
        message = "Button with ID '$buttonID' already exists",
    )
