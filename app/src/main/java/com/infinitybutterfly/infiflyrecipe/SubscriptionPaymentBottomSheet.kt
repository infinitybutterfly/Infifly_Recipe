package com.infinitybutterfly.infiflyrecipe

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.infinitybutterfly.infiflyrecipe.models.PinUpgradeRequest
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionPaymentBottomSheet : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscription_payment_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etPinInput = view.findViewById<TextInputEditText>(R.id.et_pin_input)
        val btnVerifyPin = view.findViewById<Button>(R.id.btn_verify_pin)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel_subscription)

        // 1. Listen for PIN input changes in real-time
        etPinInput.doAfterTextChanged { text ->
            val pin = text?.toString()?.trim() ?: ""

            if (pin.length > 4) {
                // PIN is 5 or 6 digits: Enable button and make text Orange
                btnVerifyPin.isEnabled = true
                btnVerifyPin.setTextColor(Color.parseColor("#FF9800")) // Material Orange
            } else {
                // PIN is too short: Disable button and revert text color
                btnVerifyPin.isEnabled = false
                btnVerifyPin.setTextColor(Color.GRAY)
            }
        }

        // 2. Handle the Verify Button Click
        btnVerifyPin.setOnClickListener {
            val enteredPin = etPinInput.text.toString().trim()

            // Disable button and show loading state
            btnVerifyPin.isEnabled = false
            btnVerifyPin.text = "Verifying..."

            val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val token = sharedPref.getString("JWT_TOKEN", "") ?: ""

            val request = PinUpgradeRequest(pin = enteredPin)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.ktorApi.upgradeToPremium("Bearer $token", request)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            // SUCCESS!
                            val successMessage = response.body()!!.message
                            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show()

                            // Update SharedPreferences to reflect Premium status
                            sharedPref.edit().putBoolean("isLocked", true).apply()

                            setFragmentResult("premium_upgrade", bundleOf("success" to true))

                            // Pop back to the previous fragment
                            findNavController().popBackStack()

                        } else {
                            // FAILURE: Invalid PIN
                            btnVerifyPin.isEnabled = true
                            btnVerifyPin.text = "Verify PIN & Upgrade"
                            btnVerifyPin.setTextColor(Color.parseColor("#FF9800"))

                            etPinInput.error = "Invalid PIN"
                            Toast.makeText(requireContext(), "Please enter a valid PIN", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // NETWORK CRASH
                        btnVerifyPin.isEnabled = true
                        btnVerifyPin.text = "Verify PIN & Upgrade"
                        btnVerifyPin.setTextColor(Color.parseColor("#FF9800"))

                        Toast.makeText(requireContext(), "Network Error. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 3. Handle the Cancel Button Click
        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}


//package com.infinitybutterfly.infiflyrecipe
//
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//
//// TODO: Rename parameter arguments, choose names that match
//// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"
//
///**
// * A simple [Fragment] subclass.
// * Use the [SubscriptionPaymentBottomSheet.newInstance] factory method to
// * create an instance of this fragment.
// */
//class SubscriptionPaymentBottomSheet : Fragment() {
//    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        return inflater.inflate(
//            R.layout.fragment_subscription_payment_bottom_sheet,
//            container,
//            false
//        )
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment SubscriptionPaymentBottomSheet.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            SubscriptionPaymentBottomSheet().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}