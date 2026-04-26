package com.infinitybutterfly.infiflyrecipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MyRecipePaywallBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_my_recipe_paywall_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSubscribe = view.findViewById<Button>(R.id.btn_subscribe_my_recipe)
        val btnMaybeLater = view.findViewById<Button>(R.id.btn_maybe_later_my_recipe)

        btnSubscribe.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.subscriptionPaymentBottomSheet)
        }

        btnMaybeLater.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.homeFragment)
        }
    }
}