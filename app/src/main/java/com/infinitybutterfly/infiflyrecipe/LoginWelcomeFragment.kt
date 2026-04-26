package com.infinitybutterfly.infiflyrecipe

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.compose.animation.core.animate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginWelcomeFragment : Fragment(R.layout.fragment_login_welcome) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val includeLayout = view.findViewById<View>(R.id.welcome_include_layout)
        val buttonFlipper = includeLayout.findViewById<Button>(R.id.button_flipper)

        // 1. Set initial positions (Off-screen/Offset)
        imageView.translationY = -2000f // Start 200 pixels higher
        imageView.alpha = 0f
        includeLayout.translationY = 2000f // Start 200 pixels lower
        includeLayout.alpha = 0f

        // 2. Animate Image (Slide Down)
        imageView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(500)
            .start()

        // 3. Animate Include Layout (Slide Up)
        includeLayout.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(800) // Slight delay for a staggered "layered" look
            .setInterpolator(AccelerateDecelerateInterpolator()) // Smooth start/stop
//            .setInterpolator(DecelerateInterpolator()) // Starts fast, ends very soft
//            .withEndAction {
//                buttonFlipper.isEnabled = true
//            }
            .withEndAction {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(100)
                    buttonFlipper.isEnabled = true
                }
            }
//            .withEndAction()
            .start()

        buttonFlipper.setOnClickListener {
            // ... navigation code ...
            val navOptions = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
            findNavController().navigate(
                R.id.action_loginWelcomeFragment_to_loginCreateAccountFragment,
                null,
                navOptions
            )
        }
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        view.findViewById<Button>(R.id.button_flipper).setOnClickListener {
//
//            val navOptions = NavOptions.Builder()
//                .setEnterAnim(R.anim.slide_in_right)
//                .setExitAnim(R.anim.slide_out_left)
//                .setPopEnterAnim(R.anim.slide_in_left)
//                .setPopExitAnim(R.anim.slide_out_right)
//                .build()
//
//            findNavController().navigate(
//                R.id.action_loginWelcomeFragment_to_loginCreateAccountFragment,
//                null,
//                navOptions
//            )
//        }
//    }

    override fun onResume() {
        super.onResume()
        // Turn OFF swipe to refresh while filling out the form
        (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
    }

    override fun onPause() {
        super.onPause()
        // Turn it back ON when they leave this screen
        (requireActivity() as MainActivity).setSwipeRefreshEnabled(true)
    }
}



//package com.infinitybutterfly.infiflyrecipe
//
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.commit
//import androidx.navigation.fragment.findNavController
//
//class LoginWelcomeFragment : Fragment(R.layout.fragment_login_welcome) {
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        view.findViewById<Button>(R.id.button_flipper).setOnClickListener {
//            parentFragmentManager.commit {
//                // Set animations BEFORE adding/replacing
//                setCustomAnimations(
//                    R.anim.slide_in_right, // enter
//                    R.anim.slide_out_left, // exit
//                    R.anim.slide_in_left,  // popEnter
//                    R.anim.slide_out_right // popExit
//                )
//                replace(R.id.fragmentContainerView, LoginCreateAccountFragment())
////                addToBackStack(null) // Allows the user to go back
//            }
//        }
//    }
//}


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
// * Use the [LoginWelcomeFragment.newInstance] factory method to
// * create an instance of this fragment.
// */
//class LoginWelcomeFragment : Fragment() {
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
//        return inflater.inflate(R.layout.fragment_login_welcome, container, false)
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment LoginWelcomeFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            LoginWelcomeFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}