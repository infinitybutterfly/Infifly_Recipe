package com.infinitybutterfly.infiflyrecipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProfilePaywallBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_profile_paywall_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSubscribe = view.findViewById<Button>(R.id.btn_subscribe_profile)
        val btnMaybeLater = view.findViewById<Button>(R.id.btn_maybe_later_profile)

        btnSubscribe.setOnClickListener {
            dismiss()

            findNavController().navigate(R.id.subscriptionPaymentBottomSheet)
//            findNavController().navigate(R.id.action_subscriptionBottomSheet_to_subscriptionPaymentBottomSheet)
//            val pinSheet = SubscriptionPaymentBottomSheet()
//            pinSheet.isCancelable = false
//            pinSheet.show(parentFragmentManager, "PinEntrySheet")
        }

        btnMaybeLater.setOnClickListener {
            dismiss()
        }
    }
}