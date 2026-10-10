package com.layzee.gesture

/** Reused result container: the engine writes into it instead of returning new objects per frame. */
class GestureOutput {
    var state = GestureState.IDLE
        internal set

    /** Swipe command for this frame; [SwipeCommand.NONE] when there is nothing to send. */
    var command = SwipeCommand.NONE
        internal set

    /** Where the flick starts and ends, in screen pixels; valid for FLICK. */
    var x = 0f
        internal set
    var y = 0f
        internal set
    var toY = 0f
        internal set
}
