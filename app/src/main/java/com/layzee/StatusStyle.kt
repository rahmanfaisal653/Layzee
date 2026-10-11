package com.layzee

import android.widget.Button
import androidx.annotation.ColorRes
import com.layzee.gesture.GestureState

/** The status palette shared by the overlay ring and the Dashboard label; null is Off. */
@ColorRes
internal fun statusColor(state: GestureState?): Int = when (state) {
    null -> R.color.status_off
    GestureState.IDLE -> R.color.status_idle
    GestureState.TRACKING -> R.color.status_tracking
    GestureState.PINCH -> R.color.status_pinch
}

/** The filled button for what is still to do, the outlined one for what is in place. */
internal fun Button.setEmphasis(primary: Boolean) {
    setBackgroundResource(if (primary) R.drawable.bg_button_primary else R.drawable.bg_button_secondary)
    setTextColor(context.getColorStateList(if (primary) R.color.button_primary_text else R.color.button_secondary_text))
}
