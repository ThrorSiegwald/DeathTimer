package com.deathtimer

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class PasswordDialogFragment : DialogFragment() {

    companion object {
        private var onComplete: ((Boolean) -> Unit)? = null

        fun newInstance(onComplete: (Boolean) -> Unit): PasswordDialogFragment {
            this.onComplete = onComplete
            return PasswordDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val editText = EditText(context).apply {
            hint = getString(R.string.password_confirm_hint)
            setPadding(60, 40, 60, 20)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(editText)
        }

        return AlertDialog.Builder(context)
            .setTitle(R.string.password_confirm_title)
            .setView(container)
            .setPositiveButton(R.string.btn_ok) { _, _ ->
                val password = editText.text.toString()
                if (PreferenceManager.checkPassword(context, password)) {
                    onComplete?.invoke(true)
                } else {
                    Toast.makeText(context, R.string.password_wrong, Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(false)
                }
            }
            .setNegativeButton(R.string.btn_cancel_dialog) { _, _ ->
                onComplete?.invoke(false)
            }
            .create()
    }
}
