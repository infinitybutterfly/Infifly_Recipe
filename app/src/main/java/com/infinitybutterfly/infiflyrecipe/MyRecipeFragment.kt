package com.infinitybutterfly.infiflyrecipe

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterMyRecipe
import com.infinitybutterfly.infiflyrecipe.databinding.FragmentMyRecipeBinding
import com.infinitybutterfly.infiflyrecipe.models.Recipe
import com.infinitybutterfly.infiflyrecipe.utils.LoadingDialog
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class MyRecipeFragment : Fragment(), RefreshableFragment {

    override fun onRefreshAction() {
        // When the user swipes, just call your existing network function!
//        fetchMyRecipesFromBackend()
        checkAccessAndFetch()
    }
    private var _binding: FragmentMyRecipeBinding? = null
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var myRecipeAdapter: RvAdapterMyRecipe
    private lateinit var recyclerView: RecyclerView
    private var cachedRecipes: List<Recipe> = emptyList()
    private var currentSortSelectionId = R.id.sort_default

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        return inflater.inflate(R.layout.fragment_my_recipe, container, false)
        _binding = FragmentMyRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener("premium_upgrade") { _, bundle ->
            val isSuccess = bundle.getBoolean("success")
            if (isSuccess) {
                // The user just paid! Refresh the profile to unlock the Edit button.
                checkAccessAndFetch()
                loadingDialog.isDismiss()
            }
        }
        loadingDialog = LoadingDialog(requireActivity())

        recyclerView = view.findViewById(R.id.recyclerviewMyRecipe)
        myRecipeAdapter = RvAdapterMyRecipe(emptyList(),
            onItemClick = { clickedRecipe ->
                val displayId = clickedRecipe.id
                Log.d("CLICKED", "User clicked on For You recipe: $displayId")
                navigateToDetailFragment(clickedRecipe.id)
            },
            onEditClick = { clickedRecipe ->
                // Navigate to Add/Edit screen and pass the data!
                navigateToAddRecipeForEditing(clickedRecipe)
            },
            onDeleteClick ={ recipeId ->
//            android.app.AlertDialog.Builder(requireContext())
                MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete this recipe? This cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    deleteMyRecipe(recipeId)
                }
                .setNegativeButton("No", null)
                .show()

//            deleteMyRecipe(recipeId)
        }
        )
        recyclerView.adapter = myRecipeAdapter

        // Fetch the real data!
//        fetchMyRecipesFromBackend()
        checkAccessAndFetch()

        setupSortButton()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                    bottomNav.selectedItemId = R.id.homeFragment
                }
            }
        )
    }

    private fun navigateToDetailFragment(id: Int) {
        val bundle = Bundle().apply {
            putString("MEAL_ID", id.toString())
            putBoolean("IS_MY_BACKEND", true)
        }
        findNavController().navigate(R.id.action_myRecipeFragment_to_recipeDetailFragment, bundle)
    }

    // 2. The Navigation Helper Function
    private fun navigateToAddRecipeForEditing(recipe: Recipe) {
        val bundle = Bundle().apply {
            // Pass all the fields you want to pre-fill
            putInt("EDIT_RECIPE_ID", recipe.id)
            putString("EDIT_RECIPE_NAME", recipe.name)
            putString("EDIT_RECIPE_IMAGE", recipe.imageUrl)
            putString("EDIT_RECIPE_CATEGORY", recipe.category)
            putString("EDIT_RECIPE_COUNTRY", recipe.country)
            putString("EDIT_RECIPE_TAGS", recipe.tags)
            putString("EDIT_RECIPE_INSTRUCTIONS", recipe.instructions)
            putString("EDIT_RECIPE_INGREDIENTS_NAME", recipe.ingredientsName)
            putString("EDIT_RECIPE_INGREDIENTS_QUANTITY", recipe.ingredientsQuantity)
        }

        findNavController().navigate(R.id.addRecipeFragment, bundle)
    }

    private fun deleteMyRecipe(recipeId: Int) {
        loadingDialog.startLoading()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
                val savedToken = sharedPref.getString("JWT_TOKEN", null)

                if (savedToken == null) {
                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        Toast.makeText(requireContext(), "Sorry there's been an error \n Please Logout! then, Login Again ", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Make the Network Call
                val response = RetrofitClient.myBackendApi.deleteRecipe(
                    token = "Bearer $savedToken",
                    recipeId = recipeId
                )

                // Handle the Result
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    if (response.isSuccessful) {
                        if (!isAdded) return@withContext
                        Toast.makeText(requireContext(), "Recipe Deleted!", Toast.LENGTH_SHORT).show()

//                        fetchMyRecipesFromBackend()
                        checkAccessAndFetch()

                    } else {
                        Toast.makeText(requireContext(), "Delete failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        // 2. DISMISS THE LOADER!
                        loadingDialog.isDismiss()
                        (requireActivity() as MainActivity).stopRefreshAnimation()
                    }
                }
            }
        }
    }

//    private fun fetchMyRecipesFromBackend() {
//        val mainActivity = activity as? MainActivity
//        val sharedPreferences = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
//
//        val savedToken = sharedPreferences.getString("JWT_TOKEN", null)
//
//        // Check if the token exists
//        if (savedToken == null) {
//            if (isAdded) {
//                Toast.makeText(
//                    requireContext(),
//                    "SharedPreferences was deleted or never saved.",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//            mainActivity?.stopRefreshAnimation()
//            return
//        }
//
//        val userToken = "Bearer $savedToken"
//
//        loadingDialog.startLoading()
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                val response = withContext(Dispatchers.IO) {
//                    RetrofitClient.myBackendApi.getMyRecipes(userToken)
//                }
//
//                if (!isAdded || view == null) return@launch
//
//                if (response.isSuccessful && response.body() != null) {
//                    val myRecipeResponse = response.body()!!
//
//                    if (myRecipeResponse.success) {
//                        val realRecipes = myRecipeResponse.results ?: emptyList()
//
//                        val tvEmptyState = requireView().findViewById<TextView>(R.id.tvEmptyState)
//
//                        if (realRecipes.isEmpty()) {
//                            tvEmptyState.visibility = View.VISIBLE
//                            recyclerView.visibility = View.GONE
//                        } else {
//                            tvEmptyState.visibility = View.GONE
//                            recyclerView.visibility = View.VISIBLE
//                            myRecipeAdapter.updateData(realRecipes)
//
//                            cachedRecipes = realRecipes
//
//                            val sortedList = applyCurrentSort(cachedRecipes)
//
//                            myRecipeAdapter.updateData(sortedList)
//                        }
//
////                        val realRecipes = myRecipeResponse.results
////                        myRecipeAdapter.updateData(realRecipes)
//                    } else {
//                        Toast.makeText(requireContext(), "Failed to load recipes", Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    Log.e("MyRecipeFragment", "Error: ${response.code()}")
//                    Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                if (isAdded) {
//                    Log.e("MyRecipeFragment", "Network exception: ${e.message}")
//                    Toast.makeText(
//                        requireContext(),
//                        "Network error. Check connection.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            } finally {
//                loadingDialog.isDismiss()
//                mainActivity?.stopRefreshAnimation()
//            }
//        }
//    }

    private fun checkAccessAndFetch() {
        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
        val jwtToken = sharedPref.getString("JWT_TOKEN", "") ?: ""

        if (jwtToken.isEmpty()) {
            // Not logged in at all. Treat as free/incomplete.
            applyAccessLogic(isPremium = false, isProfileComplete = false)
            return
        }

        // 1. Show loader while we ask the server
        loadingDialog.startLoading()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 2. Fetch the live profile data from the server
                // Note: Ensure 'profileApi' matches your RetrofitClient setup for getUserProfile
                val response = RetrofitClient.profileApi.getUserProfile("Bearer $jwtToken")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        // Grab the live truth
                        val isPremiumLive = response.body()!!.isLocked ?: false
                        val isProfileCompleteLive = response.body()!!.isProfileComplete ?: false

                        // Immediately update the local cache so it stays perfectly in sync
                        sharedPref.edit()
                            .putBoolean("isLocked", isPremiumLive)
                            .putBoolean("isProfileComplete", isProfileCompleteLive)
                            .apply()

                        // 3. Run the gatekeeper scenarios using the LIVE data
                        applyAccessLogic(isPremiumLive, isProfileCompleteLive)
                    } else {
                        // Server glitch. Fallback to cache!
                        handleMyRecipeCacheFallback(sharedPref)
                        Toast.makeText(requireContext(), "Device is Offline! Using Cache", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Network crash (e.g. no internet). Fallback to cache!
                withContext(Dispatchers.Main) {
                    handleMyRecipeCacheFallback(sharedPref)
                    Toast.makeText(requireContext(), "Device is Offline! No Internet!", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // 4. Always hide the loader
                withContext(Dispatchers.Main) {
                    loadingDialog.isDismiss()
                }
            }
        }
    }

    // --- HELPER: Handles offline/error fallbacks cleanly ---
    private fun handleMyRecipeCacheFallback(sharedPref: android.content.SharedPreferences) {
        val isPremiumCached = sharedPref.getBoolean("isLocked", false)
        val isProfileCompleteCached = sharedPref.getBoolean("isProfileComplete", false)
        applyAccessLogic(isPremiumCached, isProfileCompleteCached)
    }

    // --- HELPER: The actual UI logic for your 3 scenarios ---
    private fun applyAccessLogic(isPremium: Boolean, isProfileComplete: Boolean) {
        val tvEmptyState = requireView().findViewById<TextView>(R.id.tvEmptyState)

        if (!isPremium) {
            // SCENARIO 1: Free Tier. Block them completely.
            tvEmptyState.text = "Upgrade to Premium to manage your uploaded recipes."
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            // Pop the Paywall!
            val paywallSheet = MyRecipePaywallBottomSheet()
            paywallSheet.isCancelable = false
            paywallSheet.show(parentFragmentManager, "MyRecipePaywall")

        } else if (!isProfileComplete) {
            // SCENARIO 2: Premium Tier, but Profile Incomplete.
            tvEmptyState.text = "Please complete your profile to upload and manage recipes."
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

        } else {
            // SCENARIO 3: Premium AND Complete! Let them in!
            tvEmptyState.text = "No Recipes are Uploaded" // Default empty text

            // This function will trigger its own loader and fetch the recipes
            fetchMyRecipesFromBackend()
        }
    }

//    private fun checkAccessAndFetch() {
//        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
//        val isPremium = sharedPref.getBoolean("isLocked", false) // isLocked = true means Premium
//        val isProfileComplete = sharedPref.getBoolean("isProfileComplete", false)
//
//        val tvEmptyState = requireView().findViewById<TextView>(R.id.tvEmptyState)
//
//        if (!isPremium) {
//            // SCENARIO 1: Free Tier. Block them completely.
//            tvEmptyState.text = "Upgrade to Premium to manage your uploaded recipes."
//            tvEmptyState.visibility = View.VISIBLE
//            recyclerView.visibility = View.GONE
//
//            // Pop the Paywall!
//            val paywallSheet = MyRecipePaywallBottomSheet()
//            paywallSheet.isCancelable = false
//            paywallSheet.show(parentFragmentManager, "MyRecipePaywall")
//
//        } else if (!isProfileComplete) {
//            // SCENARIO 2: Premium Tier, but Profile Incomplete.
//            tvEmptyState.text = "Please complete your profile to upload and manage recipes."
//            tvEmptyState.visibility = View.VISIBLE
//            recyclerView.visibility = View.GONE
//
//        } else {
//            // SCENARIO 3: Premium AND Complete! Let them in!
//            tvEmptyState.text = "No Recipes are Uploaded" // Default empty text
//            fetchMyRecipesFromBackend()
//        }
//    }

    private fun fetchMyRecipesFromBackend() {
        val mainActivity = activity as? MainActivity
        val sharedPreferences = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("JWT_TOKEN", null)

        if (savedToken == null) {
            if (isAdded) Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()
            mainActivity?.stopRefreshAnimation()
            return
        }

        val userToken = "Bearer $savedToken"
        loadingDialog.startLoading()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.myBackendApi.getMyRecipes(userToken)
                }

                if (!isAdded || view == null) return@launch

                if (response.isSuccessful && response.body() != null) {
                    val myRecipeResponse = response.body()!!

                    if (myRecipeResponse.success) {
                        val realRecipes = myRecipeResponse.results ?: emptyList()

                        val tvEmptyState = requireView().findViewById<TextView>(R.id.tvEmptyState)

                        if (realRecipes.isEmpty()) {
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            cachedRecipes = realRecipes
                            val sortedList = applyCurrentSort(cachedRecipes)
                            myRecipeAdapter.updateData(sortedList)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to load recipes", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Network error. Check connection.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                loadingDialog.isDismiss()
                mainActivity?.stopRefreshAnimation()
            }
        }
    }
    private fun setupSortButton() {
        binding.btnSort.setOnClickListener {
            showSortBottomSheet()
        }
    }

    @Suppress("InflateParams")
    private fun showSortBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.sort_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)

        val radioGroup = view.findViewById<RadioGroup>(R.id.rg_sort_options)

        radioGroup.check(currentSortSelectionId)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentSortSelectionId = checkedId

//            val sortedList = when (checkedId) {
//                R.id.sort_a_to_z -> cachedRecipes.sortedBy { it.title }
//                R.id.sort_z_to_a -> cachedRecipes.sortedByDescending { it.title }
//                R.id.sort_country -> cachedRecipes.sortedBy { it.country }
//                else -> cachedRecipes // Default/Relevance (returns to original API order)
//            }

            val sortedList = applyCurrentSort(cachedRecipes)
            myRecipeAdapter.updateData(sortedList)

            Handler(Looper.getMainLooper()).postDelayed({
                bottomSheetDialog.dismiss()
            }, 200)
        }

        bottomSheetDialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            isHideable = true
        }

        bottomSheetDialog.show()
    }
    private fun applyCurrentSort(list: List<Recipe>): List<Recipe> {
        return when (currentSortSelectionId) {
            R.id.sort_a_to_z -> list.sortedBy { it.name }
            R.id.sort_z_to_a -> list.sortedByDescending { it.name }
            R.id.sort_country -> list.sortedBy { it.country }
            else -> list // Default/Relevance (Original API order)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog.isDismiss()
        _binding = null
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
// * Use the [MyRecipeFragment.newInstance] factory method to
// * create an instance of this fragment.
// */
//class MyRecipeFragment : Fragment() {
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
//        return inflater.inflate(R.layout.fragment_my_recipe, container, false)
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment MyRecipeFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            MyRecipeFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}