package com.layzee.gesture

/** Reused result container: the engine writes into it instead of returning new objects per frame. */
class GestureOutput {
    var state = GestureState.IDLE
        internal set
}
