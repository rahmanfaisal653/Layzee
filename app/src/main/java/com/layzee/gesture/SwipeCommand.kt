package com.layzee.gesture

/**
 * What the virtual finger should do for one frame. Only [GestureOutput.x] / [GestureOutput.y] (and
 * [GestureOutput.velocityY] for [LIFT]) that belong to the command are meaningful.
 */
enum class SwipeCommand {
    /** Nothing new to send this frame. */
    NONE,

    /** Virtual finger touches down at the fixed start point. */
    START,

    /** Virtual finger moves to the new position. */
    MOVE,

    /** Virtual finger lifts at the last position, moving at [GestureOutput.velocityY]. */
    LIFT,

    /** The stroke is broken (the real screen was touched); no lift velocity applies. */
    CANCEL,
}
