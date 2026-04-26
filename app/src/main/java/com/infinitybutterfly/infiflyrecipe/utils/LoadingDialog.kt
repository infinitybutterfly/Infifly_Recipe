package com.infinitybutterfly.infiflyrecipe.utils

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.infinitybutterfly.infiflyrecipe.R
import java.util.stream.DoubleStream.builder

class LoadingDialog(private val activity: Activity) {
    private var dialog: Dialog? = null

    fun startLoading() {
        if (dialog != null && dialog!!.isShowing) {
            return
        }
        // Prevent crashing if the activity is already closing
        if (activity.isFinishing || activity.isDestroyed) return

        dialog = Dialog(activity).apply {
            setContentView(R.layout.custom_loader)

            // This makes the dialog background transparent so we only see the white CardView,
            // while Android automatically dims the rest of the screen behind it!
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // This stops the user from closing the loader by tapping outside of it
            setCancelable(false)
        }

        dialog?.show()
    }

    fun isDismiss() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
            dialog=null
        }
    }
}