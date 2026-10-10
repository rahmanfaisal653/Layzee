package com.layzee.gesture

/** What to do with the screen for one frame. */
enum class SwipeCommand {
    /** Nothing to send this frame. */
    NONE,

    /** One fast swipe up from ([GestureOutput.x], [GestureOutput.y]) to [GestureOutput.toY]: next page. */
    FLICK,
}
