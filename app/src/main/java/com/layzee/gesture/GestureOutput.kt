package com.layzee.gesture

/** Reused result container: the engine writes into it instead of returning new objects per frame. */
class GestureOutput {
    var state = GestureState.IDLE
        internal set

    /** Swipe command for this frame; [SwipeCommand.NONE] when there is nothing new to send. */
    var command = SwipeCommand.NONE
        internal set

    /** Virtual finger position in screen pixels; valid for START, MOVE, LIFT and CANCEL. */
    var x = 0f
        internal set
    var y = 0f
        internal set

    /** Vertical speed of the virtual finger in px/s (positive = down); valid for LIFT. */
    var velocityY = 0f
        internal set
}
