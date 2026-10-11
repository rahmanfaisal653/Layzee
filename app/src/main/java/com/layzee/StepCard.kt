package com.layzee

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes

/**
 * One card of the permission guide: a colored status icon, the title, a fold-out reason, and a button that
 * goes to the matching system setting. Built once and added to [parent]; [setDone] only swaps icon and emphasis.
 * [action2] and [check] stay hidden unless the step needs them (battery: app info, and the user's own tick).
 */
internal class StepCard(
    parent: ViewGroup,
    @StringRes private val title: Int,
    @StringRes why: Int,
    @StringRes actionLabel: Int,
    onAction: () -> Unit,
) {
    private val root = LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
    private val icon: ImageView = root.findViewById(R.id.step_icon)
    private val whyView: TextView = root.findViewById(R.id.step_why)
    private val whyToggle: TextView = root.findViewById(R.id.step_why_toggle)
    private val action: Button = root.findViewById(R.id.step_action)
    val action2: Button = root.findViewById(R.id.step_action2)
    val check: CheckBox = root.findViewById(R.id.step_check)

    private var isDone: Boolean? = null

    init {
        root.findViewById<TextView>(R.id.step_title).setText(title)
        whyView.setText(why)
        action.setText(actionLabel)
        action.setOnClickListener { onAction() }
        whyToggle.setOnClickListener {
            val open = whyView.visibility != View.VISIBLE
            whyView.visibility = if (open) View.VISIBLE else View.GONE
            whyToggle.setText(if (open) R.string.why_hide else R.string.why)
        }
        parent.addView(root)
        setDone(false)
    }

    fun setDone(done: Boolean) {
        if (done == isDone) return // refresh() runs often; skip re-resolving drawables and strings
        isDone = done
        val context = root.context
        icon.setImageResource(if (done) R.drawable.ic_step_done else R.drawable.ic_step_todo)
        icon.contentDescription = context.getString(
            R.string.step_status_desc,
            context.getString(title),
            context.getString(if (done) R.string.step_done_desc else R.string.step_todo_desc),
        )
        // What is still to do gets the filled button; what is in place keeps only an outlined shortcut.
        action.setEmphasis(primary = !done)
    }

    fun showSecondAction(@StringRes label: Int, onClick: () -> Unit) {
        action2.setText(label)
        action2.setOnClickListener { onClick() }
        action2.visibility = View.VISIBLE
    }
}
