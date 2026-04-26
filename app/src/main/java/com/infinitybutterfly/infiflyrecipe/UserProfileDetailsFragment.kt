package com.infinitybutterfly.infiflyrecipe

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterOUserProfileDetails
import com.infinitybutterfly.infiflyrecipe.databinding.FragmentUserProfileDetailsBinding
import com.infinitybutterfly.infiflyrecipe.utils.LoadingDialog
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

class UserProfileDetailsFragment : Fragment(), RefreshableFragment {
    private var currentUsername: String = ""
    private var originalImageUrl: String = ""

    override fun onRefreshAction() {
        if (currentUsername.isNotEmpty()) {
            fetchUserProfile(currentUsername)
        } else {
            (activity as? MainActivity)?.stopRefreshAnimation()
        }
    }

    private var _binding: FragmentUserProfileDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var recipeAdapter: RvAdapterOUserProfileDetails

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingDialog = LoadingDialog(requireActivity())

        // 1. Get the username passed from SearchFragment
        val targetUsername = arguments?.getString("USER_NAME") ?: ""

        if (targetUsername.isEmpty()) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        currentUsername = targetUsername

        // 2. Setup the Back Button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // 3. Setup the RecyclerView
        setupRecyclerView()

        // 4. Fetch the profile from your Database
        fetchUserProfile(currentUsername)

        binding.ivProfilePic.setOnClickListener {
            showFullScreenImage()
        }
    }

    private fun setupRecyclerView() {
        recipeAdapter = RvAdapterOUserProfileDetails(emptyList()) { clickedRecipe ->
            // Navigate to the Recipe Details when a dish is clicked
            val bundle = Bundle().apply {
                putString("MEAL_ID", clickedRecipe.id)
                putBoolean("IS_MY_BACKEND", true) // Ensure detail screen knows it's from your backend
            }
            findNavController().navigate(R.id.action_userProfileDetailsFragment_to_recipeDetailFragment, bundle)
        }

        // Use a GridLayoutManager with 2 columns.
        // We disable nested scrolling so the parent NestedScrollView handles the scrolling smoothly.
        binding.userProfileRecipes.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = recipeAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun fetchUserProfile(username: String) {
//        val mainActivity = activity as? MainActivity
        loadingDialog.startLoading()

        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("JWT_TOKEN", "") ?: ""

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.myBackendApi.getProfileByUsername("Bearer $token", username)

                if (response.isSuccessful && response.body() != null) {
                    val userProfile = response.body()!!

                    withContext(Dispatchers.Main) {
                        // Populate Texts
                        binding.tvUsername.text = userProfile.username
                        binding.tvChefName.text = userProfile.name
                        binding.tvCountry.text = userProfile.country
                        binding.tvFollowedBio.text = userProfile.bio ?: "No bio available."

                        // Populate the post count based on the size of the recipes array
                        val recipeCount = userProfile.uploadedRecipes?.size ?: 0
                        binding.tvPostCount.text = recipeCount.toString()

                        // Populate Image
                        val imageUrl = userProfile.profilePicUrl ?: ""

                        originalImageUrl = imageUrl

                        if (imageUrl.isNotBlank()) {
                            Glide.with(requireContext())
                                .load(imageUrl)
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .into(binding.ivProfilePic)
                        } else {
                            // Optional: Manually set the placeholder if it IS blank
                            binding.ivProfilePic.setImageResource(android.R.drawable.sym_def_app_icon)
                        }

                        // Send the recipes to the RecyclerView Adapter
                        userProfile.uploadedRecipes?.let { recipes ->
                            recipeAdapter.updateData(recipes)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Could not load profile: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("API_CRASH", "Failed to fetch profile: ${e.message}")
                    Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.isDismiss()
                    if (isAdded){
                        (activity as? MainActivity)?.stopRefreshAnimation()
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFullScreenImage() {
        // Create a full-screen black dialog
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        // Create an ImageView programmatically
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.BLACK)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        if (originalImageUrl.isNotEmpty()) {
            Glide.with(requireContext()).load(originalImageUrl).into(imageView)
        } else {
            imageView.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        var startX = 0f
        var startY = 0f
        var isDragging = false
        val swipeThreshold = 300f // How far in pixels they have to drag to trigger the dismiss

        // 1. Set the standard click listener to handle the dismissal
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        imageView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // Record where the finger first touched the screen
                    startX = event.rawX
                    startY = event.rawY
                    isDragging = false
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    // Calculate how far the finger has moved
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY

                    // If they moved their finger more than 20 pixels, we count it as a drag
                    if (abs(deltaX) > 20 || abs(deltaY) > 20) {
                        isDragging = true
                    }

                    if (isDragging) {
                        // Move the image with the finger
                        v.translationX = deltaX
                        v.translationY = deltaY

                        // Slowly fade out the image the further they drag it
                        val maxDistance = max(abs(deltaX), abs(deltaY))
                        v.alpha = 1f - (maxDistance / 1500f)
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY

                    if (isDragging) {
                        // Did they drag it far enough to dismiss?
                        if (abs(deltaX) > swipeThreshold || abs(deltaY) > swipeThreshold) {
                            // YES: Animate the image flying off-screen in the direction they swiped, then close!
                            v.animate()
                                .translationX(deltaX * 5)
                                .translationY(deltaY * 5)
                                .alpha(0f)
                                .setDuration(250)
                                .withEndAction { dialog.dismiss() }
                                .start()
                        } else {
                            // NO: They didn't drag far enough. Snap it back to the center!
                            v.animate()
                                .translationX(0f)
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(250)
                                .start()
                        }
                    } else {
                        // 2. It wasn't a drag, it was just a quick tap!
                        // Tell Android a tap happened, which will trigger the setOnClickListener above.
                        v.performClick()
                    }
                    true
                }
                else -> false
            }
        }

        dialog.setContentView(imageView)
        dialog.show()
    }

//    private fun showFullScreenImage() {
//        // Create a full-screen black dialog
//        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
//
//        // Create an ImageView programmatically
//        val imageView = ImageView(requireContext()).apply {
//            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//            setBackgroundColor(android.graphics.Color.BLACK)
//            scaleType = ImageView.ScaleType.FIT_CENTER
//        }
//
//        if (originalImageUrl.isNotEmpty()) {
//            Glide.with(requireContext()).load(originalImageUrl).into(imageView)
//        } else {
//            imageView.setImageResource(android.R.drawable.sym_def_app_icon)
//        }
//
//        var startX = 0f
//        var startY = 0f
//        var isDragging = false
//        val swipeThreshold = 300f // How far in pixels they have to drag to trigger the dismiss
//
//        imageView.setOnTouchListener { v, event ->
//            when (event.action) {
//                android.view.MotionEvent.ACTION_DOWN -> {
//                    // Record where the finger first touched the screen
//                    startX = event.rawX
//                    startY = event.rawY
//                    isDragging = false
//                    true
//                }
//                android.view.MotionEvent.ACTION_MOVE -> {
//                    // Calculate how far the finger has moved
//                    val deltaX = event.rawX - startX
//                    val deltaY = event.rawY - startY
//
//                    // If they moved their finger more than 20 pixels, we count it as a drag
//                    if (Math.abs(deltaX) > 20 || Math.abs(deltaY) > 20) {
//                        isDragging = true
//                    }
//
//                    if (isDragging) {
//                        // 1. Move the image with the finger
//                        v.translationX = deltaX
//                        v.translationY = deltaY
//
//                        // 2. Slowly fade out the image the further they drag it
//                        val maxDistance = Math.max(Math.abs(deltaX), Math.abs(deltaY))
//                        v.alpha = 1f - (maxDistance / 1500f)
//                    }
//                    true
//                }
//                android.view.MotionEvent.ACTION_UP -> {
//                    val deltaX = event.rawX - startX
//                    val deltaY = event.rawY - startY
//
//                    if (isDragging) {
//                        // Did they drag it far enough to dismiss?
//                        if (Math.abs(deltaX) > swipeThreshold || Math.abs(deltaY) > swipeThreshold) {
//                            // YES: Animate the image flying off-screen in the direction they swiped, then close!
//                            v.animate()
//                                .translationX(deltaX * 5)
//                                .translationY(deltaY * 5)
//                                .alpha(0f)
//                                .setDuration(250)
//                                .withEndAction { dialog.dismiss() }
//                                .start()
//                        } else {
//                            // NO: They didn't drag far enough. Snap it back to the center!
//                            v.animate()
//                                .translationX(0f)
//                                .translationY(0f)
//                                .alpha(1f)
//                                .setDuration(250)
//                                .start()
//                        }
//                    } else {
//                        // It wasn't a drag, it was just a quick tap! Dismiss immediately.
//                        dialog.dismiss()
//                    }
//                    true
//                }
//                else -> false
//            }
//        }
//
////        // Close the full-screen view when tapped
////        imageView.setOnClickListener { dialog.dismiss() }
//
//        dialog.setContentView(imageView)
//        dialog.show()
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog.isDismiss()
        _binding = null
    }
}


//package com.infinitybutterfly.infiflyrecipe
//
//import android.os.Bundle
//import android.util.Log
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
////import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterOUserProfileDetails
//import com.infinitybutterfly.infiflyrecipe.databinding.FragmentRecipeDetailBinding
//
//class UserProfileDetailsFragment : Fragment() {
////    private lateinit var rvAdapter: RvAdapterOUserProfileDetails
//    private var _binding: FragmentRecipeDetailBinding? = null
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val userName = arguments?.getString("USER_NAME")
//
//        if(userName!=null){
//            Log.e("USER_NAME_PROFILE", "Stored UserName: $userName")
//        }
//        else{
//            Log.e("USER_NAME_PROFILE", "No UserName was successfully stored")
//        }
//    }
//}
//
//
//
////// For Getting total number of recipes uploaded by user
////val recipeCount = recipeList.size
////binding.recipeCountTextView.text = "Total Recipes: $recipeCount"
//
////package com.infinitybutterfly.infiflyrecipe
////
////import android.os.Bundle
////import androidx.fragment.app.Fragment
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////
////// TODO: Rename parameter arguments, choose names that match
////// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
////private const val ARG_PARAM1 = "param1"
////private const val ARG_PARAM2 = "param2"
////
/////**
//// * A simple [Fragment] subclass.
//// * Use the [UserProfileDetailsFragment.newInstance] factory method to
//// * create an instance of this fragment.
//// */
////class UserProfileDetailsFragment : Fragment() {
////    // TODO: Rename and change types of parameters
////    private var param1: String? = null
////    private var param2: String? = null
////
////    override fun onCreate(savedInstanceState: Bundle?) {
////        super.onCreate(savedInstanceState)
////        arguments?.let {
////            param1 = it.getString(ARG_PARAM1)
////            param2 = it.getString(ARG_PARAM2)
////        }
////    }
////
////    override fun onCreateView(
////        inflater: LayoutInflater, container: ViewGroup?,
////        savedInstanceState: Bundle?
////    ): View? {
////        // Inflate the layout for this fragment
////        return inflater.inflate(R.layout.fragment_user_profile_details, container, false)
////    }
////
////    companion object {
////        /**
////         * Use this factory method to create a new instance of
////         * this fragment using the provided parameters.
////         *
////         * @param param1 Parameter 1.
////         * @param param2 Parameter 2.
////         * @return A new instance of fragment UserProfileDetailsFragment.
////         */
////        // TODO: Rename and change types and number of parameters
////        @JvmStatic
////        fun newInstance(param1: String, param2: String) =
////            UserProfileDetailsFragment().apply {
////                arguments = Bundle().apply {
////                    putString(ARG_PARAM1, param1)
////                    putString(ARG_PARAM2, param2)
////                }
////            }
////    }
////}