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
            hint = "Введите пароль"
            setPadding(60, 40, 60, 20)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(editText)
        }

        return AlertDialog.Builder(context)
            .setTitle("Подтверждение")
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                val password = editText.text.toString()
                if (PreferenceManager.checkPassword(context, password)) {
                    onComplete?.invoke(true)
                } else {
                    Toast.makeText(context, "Неверный пароль", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(false)
                }
            }
            .setNegativeButton("Отмена") { _, _ ->
                onComplete?.invoke(false)
            }
            .create()
    }
}
