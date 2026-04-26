package com.infinitybutterfly.infiflyrecipe

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.infinitybutterfly.infiflyrecipe.R
import com.infinitybutterfly.infiflyrecipe.models.PinUpgradeRequest
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_subscription_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSubscribe = view.findViewById<Button>(R.id.btn_purchase_premium)
        val btnClose = view.findViewById<Button>(R.id.btn_maybe_later)

//        btnSubscribe.setOnClickListener {
//            Toast.makeText(requireContext(), "Opening Google Play...", Toast.LENGTH_SHORT).show()
//        }
//        btnSubscribe.setOnClickListener {
//            // 1. Get the token from SharedPreferences
//            val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
//            val token = sharedPref.getString("JWT_TOKEN", "") ?: ""
//
//            // 2. Create the request with the secret PIN
//            val upgradeRequest = PinUpgradeRequest(pin = "12345")
//
//            // 3. Send it to Ktor
//            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//                try {
//                    val response = RetrofitClient.ktorApi.upgradeToPremium("Bearer $token", upgradeRequest)
//
//                    withContext(Dispatchers.Main) {
//                        if (response.isSuccessful) {
//                            // SUCCESS! Update the local cache so the app knows we are Premium
//                            sharedPref.edit().putBoolean("isLocked", true).apply()
//
//                            Toast.makeText(requireContext(), "Premium Unlocked via PIN!", Toast.LENGTH_SHORT).show()
//
//                            // Close the Bottom Sheet
//                            dismiss()
//                        } else {
//                            // Wrong PIN or server error
//                            Toast.makeText(requireContext(), "Upgrade Failed: Invalid PIN", Toast.LENGTH_LONG).show()
//                        }
//                    }
//                } catch (e: Exception) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }

        btnSubscribe.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.subscriptionPaymentBottomSheet)

//            val pinSheet = SubscriptionPaymentBottomSheet()
//            pinSheet.isCancelable = false
//            pinSheet.show(parentFragmentManager, "PinEntrySheet")
        }
////            loadingDialog.startLoading()
//
//            // 2. Launch a quick coroutine on the Main thread
//            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
//
//                kotlinx.coroutines.delay(600)
//
////                loadingDialog.isDismiss()
//
//                val bottomSheet = SubscriptionBottomSheet()
//
//                // Optional: Prevent them from clicking outside to dismiss it
//                // bottomSheet.isCancelable = false
//
//                bottomSheet.show(parentFragmentManager, "SubscriptionPaywall")
//            }
//        }

        btnClose.setOnClickListener {
            Toast.makeText(requireContext(), "Hope You Change Your Mind", Toast.LENGTH_SHORT).show()
            dismiss()
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
// * Use the [SubscriptionBottomSheet.newInstance] factory method to
// * create an instance of this fragment.
// */
//class SubscriptionBottomSheet : Fragment() {
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
//        return inflater.inflate(R.layout.fragment_subscription_bottom_sheet, container, false)
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment SubscriptionBottomSheet.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            SubscriptionBottomSheet().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}