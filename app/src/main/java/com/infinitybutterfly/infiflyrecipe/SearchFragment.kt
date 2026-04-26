package com.infinitybutterfly.infiflyrecipe

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.inputmethod.InputMethodManager
import android.widget.RadioGroup
import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterSearchRecipe
import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterUserProfile
import com.infinitybutterfly.infiflyrecipe.databinding.FragmentSearchBinding
import com.infinitybutterfly.infiflyrecipe.models.UnifiedRecipeItem// IMPORT YOUR NEW MODEL!
import com.infinitybutterfly.infiflyrecipe.models.UserProfileItems
import com.infinitybutterfly.infiflyrecipe.models.UserProfiles
import com.infinitybutterfly.infiflyrecipe.utils.LoadingDialog
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
import kotlin.collections.map

class SearchFragment : Fragment(), RefreshableFragment {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var rvAdapter: RvAdapterSearchRecipe
    private lateinit var profileAdapter: RvAdapterUserProfile

    // Requirement 2: Separate variables for IDs
    private var lastMealDbId: String = ""
    private var lastKtorRecipeId: String = ""
    private var lastProfileUserName: String = ""
//    private var savedMealId: String = ""
//    private var savedemail: String = ""

    // --- NEW STATE TRACKERS ---
    private enum class SearchFilter { ALL, RECIPES, PROFILES }
    private var currentFilter = SearchFilter.ALL

    private var noRecipesFound = false
    private var noProfilesFound = false

    // --- CACHE VARIABLES ---
    private var cachedRecipes: List<UnifiedRecipeItem> = emptyList()
    private var cachedProfiles: List<UserProfiles> = emptyList()
    private var hasSearched = false
    private var recipesScrollState: android.os.Parcelable? = null
    private var profilesScrollState: android.os.Parcelable? = null
    private var currentSearchQuery: String = ""
    private var completedSearchCount = 0
    private val totalSearchesToLoad = 2
    // Sorting
    private var currentSortSelectionId = R.id.sort_default

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingDialog = LoadingDialog(requireActivity())

        if (currentSearchQuery.isEmpty()) {
            currentSearchQuery = arguments?.getString("SEARCH_QUERY") ?: ""
        }

        val searchQuery = arguments?.getString("SEARCH_QUERY") ?: ""

        binding.searchEditText.setText(currentSearchQuery)
        binding.tvSearchStatus.visibility = View.VISIBLE
        binding.tvSearchStatus.text = "You have searched for: \"$currentSearchQuery\""

//        val editText = view.findViewById<EditText>(R.id.search_edit_text)
//
//        editText.setText(searchQuery)
//        binding.searchEditText.setText(searchQuery)
//
//        binding.tvSearchStatus.visibility = View.VISIBLE
//        binding.tvSearchStatus.text = "You have searched for: \"$searchQuery\""

        setupRecyclerViews()

        setupSearchToggles()

        setupSortButton()

        binding.iconBack.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)

            binding.searchEditText.clearFocus()

            findNavController().popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })

        if (searchQuery.isNotEmpty()) {
            saveSearchQueryToHistory(searchQuery)
            if (!hasSearched) {
                // 1. Grab the saved JWT token from SharedPreferences
                val sharedPref =
                    requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                val token = sharedPref.getString("JWT_TOKEN", "") ?: ""

                completedSearchCount = 0
                loadingDialog.startLoading()

                // 2. Fire the dual search!
                performMasterSearch(searchQuery, token)
                profileSearch(searchQuery, token)

                applyFilter(SearchFilter.ALL)
            } else {
                // 2. COMING BACK FROM DETAIL SCREEN: Restore instantly!
                rvAdapter.updateData(cachedRecipes)
                profileAdapter.updateData(cachedProfiles)

                // Re-apply the exact filter tab they were looking at and also the Position
                applyFilter(currentFilter)

                binding.rvSearchResults.post {
                recipesScrollState?.let {
                    binding.rvSearchResults.layoutManager?.onRestoreInstanceState(it)
                }}
                binding.rvprofileusers.post {
                profilesScrollState?.let {
                    binding.rvprofileusers.layoutManager?.onRestoreInstanceState(it)
                }}
            }
//            It will only show the recipe result initially
//            showRecipesSection()
        }

//        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
//            // Check if the user pressed the "Search", "Done", or "Enter" key on the keyboard
//            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
//                actionId == EditorInfo.IME_ACTION_DONE ||
//                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
//            ) {
//                // Get the new text the user just typed
//                val newQuery = binding.searchEditText.text.toString().trim()
//
//                if (newQuery.isNotEmpty()) {
//                    val sharedPref =
//                        requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
//                    val token = sharedPref.getString("JWT_TOKEN", "") ?: ""
//                    // A. Update the Status Text
//                    binding.tvSearchStatus.text = "You have searched for: \"$currentSearchQuery\""
//
//                    // B. Hide the keyboard so they can see the results
//                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                    imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
//
//                    // Clear the blinking cursor from the text box
//                    binding.searchEditText.clearFocus()
//
//                    // C. Reset the cache flag so it knows to fetch fresh data
//                    hasSearched = false
//
//                    // D. Fire the new search to the APIs!
//                    performMasterSearch(newQuery, token)
//                    profileSearch(newQuery, token)
//
//                    // Re-apply the current tab (All, Recipes, or Profiles)
//                    applyFilter(currentFilter)
//                } else {
//                    // If they cleared the box and hit enter, just hide the keyboard
//                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                    imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
//                }
//                true // Tell Android we handled the Enter key press
//            } else {
//                false
//            }
//        }

        // --- NEW SEARCH FROM EDITTEXT ---
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val newQuery = binding.searchEditText.text.toString().trim()

                if (newQuery.isNotEmpty()) {
                    // A. Update our master tracking variable!
                    currentSearchQuery = newQuery

                    saveSearchQueryToHistory(currentSearchQuery)

                    // B. Update the UI
                    binding.tvSearchStatus.text = "You have searched for: \"$currentSearchQuery\""
                    val sharedPref =
                        requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
                    binding.searchEditText.clearFocus()

                    // C. Reset cache & search
                    hasSearched = false
                    val freshToken = sharedPref.getString("JWT_TOKEN", "") ?: ""

                    completedSearchCount = 0
                    loadingDialog.startLoading()

                    performMasterSearch(currentSearchQuery, freshToken)
                    profileSearch(currentSearchQuery, freshToken)
                    applyFilter(currentFilter)
                } else {
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
                }
                true
            } else {
                false
            }
        }

        // Optional: Back button on the search bar to clear focus/keyboard
        binding.iconBack.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
            binding.searchEditText.clearFocus()

            //Navigate Back
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerViews() {
        // RECIPES
        if (!::rvAdapter.isInitialized) {
            rvAdapter = RvAdapterSearchRecipe(emptyList()) { clickedRecipe ->
                val bundle = Bundle().apply {
                    putString("MEAL_ID", clickedRecipe.id)
                    putBoolean("IS_MY_BACKEND", clickedRecipe.isFromMyBackend)
                }

                // Requirement 2: Store in different variables based on source
                if (clickedRecipe.isFromMyBackend) {
                    lastKtorRecipeId = clickedRecipe.id
                    Log.d("NAV_LOG", "Saved Ktor ID: $lastKtorRecipeId")
                } else {
                    lastMealDbId = clickedRecipe.id
                    Log.d("NAV_LOG", "Saved MealDB ID: $lastMealDbId")
                }

                findNavController().navigate(
                    R.id.action_searchFragment_to_recipeDetailFragment,
                    bundle
                )
            }
            rvAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
            binding.rvSearchResults.adapter = rvAdapter

//        binding.rvSearchResults.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = rvAdapter}

        // PROFILES (Requirement 3)
        if (!::profileAdapter.isInitialized) {
            profileAdapter = RvAdapterUserProfile(emptyList()) { clickedUser ->
                lastProfileUserName = clickedUser.username
                val bundle = Bundle().apply {
                    putString("USER_NAME", clickedUser.username) // Passing email as ID
                }
                findNavController().navigate(
                    R.id.action_searchFragment_to_userProfileDetailsFragment,
                    bundle
                )
            }
            profileAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.rvprofileusers.adapter = profileAdapter
    }

//    private fun setupRecyclerView() {
//        // Notice we changed clickedRecipe.idMeal to clickedRecipe.id to match the Unified item!
//        rvAdapter = RvAdapterSearchRecipe(emptyList()) { clickedRecipe ->
//
//            savedMealId = clickedRecipe.id
//            Log.d("CLICK_TEST", "Successfully saved Meal ID: $savedMealId")
//
//            val bundle = Bundle().apply {
//                putString("MEAL_ID", clickedRecipe.id)
//
//                // IMPORTANT: We pass this boolean so the Detail screen knows WHICH server to get details from!
//                putBoolean("IS_MY_BACKEND", clickedRecipe.isFromMyBackend)
//            }
//            findNavController().navigate(R.id.action_searchFragment_to_recipeDetailFragment, bundle)
//        }
//
//        binding.rvSearchResults.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = rvAdapter
//        }
//    }

//    private fun setupRecyclerViewProfile() {
//        rvAdapter = RvAdapterSearchRecipe(emptyList()) { clickedRecipe ->
//
//            savedemail = clickedRecipe.email
//            Log.d("CLICK_TEST", "Successfully saved Meal ID: $savedemail")
//
//            val bundle = Bundle().apply {
//                putString("E_MAIL", clickedRecipe.id)
//            }
//            findNavController().navigate(R.id.action_searchFragment_to_userProfileDetailsFragment, bundle)
//        }
//
//        binding.rvSearchResults.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = rvAdapter
//        }
//    }

    private fun checkAndDismissSearchLoader() {
        completedSearchCount++
        if (completedSearchCount >= totalSearchesToLoad) {
            loadingDialog.isDismiss()
            (activity as? MainActivity)?.stopRefreshAnimation()
        }
    }
    private fun performMasterSearch(query: String, savedToken: String) {
//        val mainActivity = activity as? MainActivity

//        loadingDialog.startLoading()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Fire BOTH requests at the exact same time
                val ktorDeferred = async { RetrofitClient.ktorApi.searchRecipes("Bearer $savedToken", query) }
                val mealDbDeferred = async { RetrofitClient.mealDbApi.getSearchRecipe(query) }

                // 2. Wait for BOTH of them to finish downloading
                val ktorResponse = ktorDeferred.await()
                val mealDbResponse = mealDbDeferred.await()

                withContext(Dispatchers.Main) {
                    val myDbRecipes = if (ktorResponse.isSuccessful) ktorResponse.body()?.results ?: emptyList() else emptyList()

//                    val publicRecipes: List<com.infinitybutterfly.infiflyrecipe.models.UnifiedRecipeItem> =
//                        if (mealDbResponse.isSuccessful) mealDbResponse.body()?.meals ?: emptyList() else emptyList()
                    val publicRecipes = if (mealDbResponse.isSuccessful) mealDbResponse.body()?.meals ?: emptyList() else emptyList()

                    // 3. Map Ktor recipes to our Unified format
                    val mappedKtor = myDbRecipes.map {
                        UnifiedRecipeItem(
                            id = it.id.toString(),
                            title = it.name,
                            imageUrl = it.imageUrl,
                            isFromMyBackend = true,
                            country = it.country
                        )
                    }

                    // 4. Map MealDB recipes to our Unified format
                    val mappedMealDb = publicRecipes.map {
                        UnifiedRecipeItem(
                            id = it.idMeal,
                            title = it.strMeal,
                            imageUrl = it.strMealThumb,
                            isFromMyBackend = false,
                            country = it.strArea
                        )
                    }

                    // 5. Mash them together!
                    val finalCombinedSearchList = mappedKtor + mappedMealDb

                    Log.d("API_SUCCESS", "Combined results: ${finalCombinedSearchList.size} recipes found!")

                    // --- Save to Cache! ---
                    cachedRecipes = finalCombinedSearchList
                    hasSearched = true

                    // 6. Update the UI
                    if (finalCombinedSearchList.isEmpty()) {
                        binding.textinputError.visibility = View.VISIBLE
                        binding.textinputError.text = "No recipes found for '$query'. Try another ingredient!, Name!"
                        rvAdapter.updateData(emptyList())
                    } else {
                        binding.textinputError.visibility = View.GONE

                        val sortedData = applyCurrentSort(finalCombinedSearchList)
                        rvAdapter.updateData(sortedData)
                    }
                }
            } catch (e: Exception) {
                Log.e("API_CRASH", "Network request failed completely: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.textinputError.visibility = View.VISIBLE
                    binding.textinputError.text = "Network request failed completely: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded) checkAndDismissSearchLoader()
                }
            }
        }
    }
    private fun applyCurrentSort(list: List<UnifiedRecipeItem>): List<UnifiedRecipeItem> {
        return when (currentSortSelectionId) {
            R.id.sort_a_to_z -> list.sortedBy { it.title }
            R.id.sort_z_to_a -> list.sortedByDescending { it.title }
            R.id.sort_country -> list.sortedBy { it.country }
            else -> list // Default/Relevance (Original API order)
        }
    }

//    private fun profileSearch(query: String, savedToken: String){
//        val mainActivity = activity as? MainActivity
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val profileDeferred = async { RetrofitClient.ktorApi.searchProfiles("Bearer $savedToken", query) }
//                val profilektorResponse = profileDeferred.await()
//
//                withContext(Dispatchers.Main) {
//                    val myProfiles = if (profilektorResponse.isSuccessful) profilektorResponse.body()?.results ?: emptyList() else emptyList()
//
////                    // 3. Map Ktor profiles to our format
////                    val profilemappedKtor = myProfiles.map {
////                        UserProfileItems (
////                            username = it.username,
////                            name = it.name,
////                            profilePicUrl = it.profilePicUrl,
////                            email = it.email,
////                            isProfileComplete = true
////                        )
////                    }
//
//                    Log.d("API_SUCCESS", "Results: ${myProfiles.size} recipes found!")
//
//                    // 6. Update the UI
//                    if (myProfiles.isEmpty()) {
//                        binding.textinputErrorProfile.visibility = View.VISIBLE
//                        binding.textinputErrorProfile.text = "No Chefs found."
//                        profileAdapter.updateData(emptyList())
//                    } else {
//                        binding.textinputErrorProfile.visibility = View.GONE
//                        profileAdapter.updateData(myProfiles) // Update the NEW adapter
//                    }
//                }
//
//            } catch (e: Exception) {
//                Log.e("API_CRASH", "Network request failed completely: ${e.message}")
//                withContext(Dispatchers.Main) {
//                    binding.textinputError.visibility = View.VISIBLE
//                    binding.textinputError.text = "Network request failed completely: ${e.message}"
//                }
//            } finally {
//                    mainActivity?.stopRefreshAnimation()
//            }
//        }
//    }

    private fun profileSearch(query: String, savedToken: String){
        val mainActivity = activity as? MainActivity

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val profileDeferred = async { RetrofitClient.ktorApi.searchProfiles("Bearer $savedToken", query) }
                val profilektorResponse = profileDeferred.await()

                withContext(Dispatchers.Main) {
                    // 1. Grab the raw list of UserProfiles from the backend
                    val myProfiles = if (profilektorResponse.isSuccessful) profilektorResponse.body()?.results ?: emptyList() else emptyList()

                    Log.d("API_SUCCESS", "Results: ${myProfiles.size} profiles found!")

                    // --- Save to Cache! ---
                    cachedProfiles = myProfiles
                    hasSearched = true

                    // 2. Pass them DIRECTLY to your adapter!
                    // The backend already guaranteed isProfileComplete is true for all of them.
                    if (myProfiles.isEmpty()) {
                        binding.textinputErrorProfile.visibility = View.VISIBLE
                        binding.textinputErrorProfile.text = "No Chefs found."
                        profileAdapter.updateData(emptyList())
                    } else {
                        binding.textinputErrorProfile.visibility = View.GONE
                        profileAdapter.updateData(myProfiles)
                    }
                }

            } catch (e: Exception) {
                Log.e("API_CRASH", "Network request failed completely: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.textinputErrorProfile.visibility = View.VISIBLE
                    binding.textinputErrorProfile.text = "Network Error: Please check your connection."
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded) checkAndDismissSearchLoader()
                }
            }
        }
    }

//    private fun setupSearchToggles() {
//        binding.recipesButtonSearch.setOnClickListener {
//            // Show Recipe Section
//            binding.recipes.visibility = View.VISIBLE
//            binding.rvSearchResults.visibility = View.VISIBLE
//            binding.textinputError.visibility = View.VISIBLE
//
//            // Hide Profile Section
//            binding.profile.visibility = View.GONE
//            binding.rvprofileusers.visibility = View.GONE
//            binding.textinputErrorProfile.visibility = View.GONE
//        }
//
//        binding.profileButtonSearch.setOnClickListener {
//            // Show Profile Section
//            binding.profile.visibility = View.VISIBLE
//            binding.rvprofileusers.visibility = View.VISIBLE
//            binding.textinputErrorProfile.visibility = View.VISIBLE
//
//            // Hide Recipe Section
//            binding.recipes.visibility = View.GONE
//            binding.rvSearchResults.visibility = View.GONE
//            binding.textinputError.visibility = View.GONE
//        }
//    }

//    private fun setupSearchToggles() {
//        binding.recipesButtonSearch.setOnClickListener {
//            showRecipesSection()
//        }
//
//        binding.profileButtonSearch.setOnClickListener {
//            showProfilesSection()
//        }
//    }

    private fun setupSearchToggles() {
        binding.recipesButtonSearch.setOnClickListener {
            // If it's already on Recipes, turn it off (switch to ALL). Otherwise, switch to Recipes.
            if (currentFilter == SearchFilter.RECIPES) {
                applyFilter(SearchFilter.ALL)
            } else {
                applyFilter(SearchFilter.RECIPES)
            }
        }

        binding.profileButtonSearch.setOnClickListener {
            // If it's already on Profiles, turn it off (switch to ALL). Otherwise, switch to Profiles.
            if (currentFilter == SearchFilter.PROFILES) {
                applyFilter(SearchFilter.ALL)
            } else {
                applyFilter(SearchFilter.PROFILES)
            }
        }
    }

    private fun applyFilter(filter: SearchFilter) {
        currentFilter = filter

        val showRecipes = filter == SearchFilter.ALL || filter == SearchFilter.RECIPES
        val showProfiles = filter == SearchFilter.ALL || filter == SearchFilter.PROFILES

        // 1. Toggle Recipes Section
        binding.recipes.visibility = if (showRecipes) View.VISIBLE else View.GONE
        binding.rvSearchResults.visibility = if (showRecipes) View.VISIBLE else View.GONE
        binding.textinputError.visibility = if (showRecipes && noRecipesFound) View.VISIBLE else View.GONE

        // 2. Toggle Profiles Section
        binding.profile.visibility = if (showProfiles) View.VISIBLE else View.GONE
        binding.rvprofileusers.visibility = if (showProfiles) View.VISIBLE else View.GONE
        binding.textinputErrorProfile.visibility = if (showProfiles && noProfilesFound) View.VISIBLE else View.GONE

        // 3. Visual Feedback: Fade out the unselected button slightly so the user knows what is active!
        binding.recipesButtonSearch.alpha = if (filter == SearchFilter.PROFILES) 0.5f else 1.0f
        binding.profileButtonSearch.alpha = if (filter == SearchFilter.RECIPES) 0.5f else 1.0f

        val bottomGapInPixels = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP,
            280f, // Change this number to make the gap bigger or smaller
            resources.displayMetrics
        ).toInt()

        // Apply padding logic here instead of xml
        if (filter == SearchFilter.RECIPES) {
            // When viewing ONLY Recipes, add the gap to the bottom
            binding.rvSearchResults.setPadding(
                binding.rvSearchResults.paddingLeft,
                binding.rvSearchResults.paddingTop,
                binding.rvSearchResults.paddingRight,
                bottomGapInPixels
            )
        } else {
            // When viewing ALL, remove the bottom gap from Recipes so it sits flush above Profiles
            binding.rvSearchResults.setPadding(
                binding.rvSearchResults.paddingLeft,
                binding.rvSearchResults.paddingTop,
                binding.rvSearchResults.paddingRight,
                0
            )
        }

        // Apply the bottom gap to Profiles anytime it is visible (since it's always at the bottom)
        if (showProfiles) {
            binding.rvprofileusers.setPadding(
                binding.rvprofileusers.paddingLeft,
                binding.rvprofileusers.paddingTop,
                binding.rvprofileusers.paddingRight,
                bottomGapInPixels
            )
        }
    }

//    private fun showRecipesSection() {
//        // Show Recipes
//        binding.recipes.visibility = View.VISIBLE
//        binding.rvSearchResults.visibility = View.VISIBLE
//        binding.textinputError.visibility = View.VISIBLE
//
//        // Hide Profiles
//        binding.profile.visibility = View.GONE
//        binding.rvprofileusers.visibility = View.GONE
//        binding.textinputErrorProfile.visibility = View.GONE
//    }
//
//    private fun showProfilesSection() {
//        // Show Profiles
//        binding.profile.visibility = View.VISIBLE
//        binding.rvprofileusers.visibility = View.VISIBLE
//        binding.textinputErrorProfile.visibility = View.VISIBLE
//
//        // Hide Recipes
//        binding.recipes.visibility = View.GONE
//        binding.rvSearchResults.visibility = View.GONE
//        binding.textinputError.visibility = View.GONE
//    }

//    override fun onRefreshAction() {
//        val searchQuery = arguments?.getString("SEARCH_QUERY") ?: ""
//
//        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
//        val token = sharedPref.getString("JWT_TOKEN", "") ?: ""
//
//        if (searchQuery.isNotEmpty()) {
//            performMasterSearch(searchQuery, token)
//        } else {
//            (activity as? MainActivity)?.stopRefreshAnimation()
//        }
//    }
override fun onRefreshAction() {
    val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("JWT_TOKEN", "") ?: ""

    if (currentSearchQuery.isNotEmpty()) {

        completedSearchCount = 0
        loadingDialog.startLoading()

        performMasterSearch(currentSearchQuery, token)
        profileSearch(currentSearchQuery, token)
//        setupSortButton()
    } else {
        (activity as? MainActivity)?.stopRefreshAnimation()
    }
}
    private fun setupSortButton() {
        binding.btnSort.setOnClickListener {
            showSortBottomSheet()
        }
    }

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
            rvAdapter.updateData(sortedList)

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

    // --- RECENT SEARCHES CACHE LOGIC ---

    private fun saveSearchQueryToHistory(query: String) {
        if (query.isBlank()) return

        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val historyString = sharedPref.getString("SEARCH_HISTORY", "") ?: ""

        // Convert the saved string back into a list
        val historyList = if (historyString.isNotEmpty()) {
            historyString.split(";;;").toMutableList()
        } else {
            mutableListOf()
        }

        // 1. Remove the query if it already exists (so we can move it to the front)
        historyList.remove(query)

        // 2. Add the new search to the very top (index 0)
        historyList.add(0, query)

        // 3. Keep only the top 5 items
        val trimmedList = historyList.take(5)

        // 4. Join them back into a string and save them
        val newHistoryString = trimmedList.joinToString(";;;")
        sharedPref.edit().putString("SEARCH_HISTORY", newHistoryString).apply()

        Log.d("SEARCH_HISTORY", "Saved 5 Recent: $trimmedList")
    }

    override fun onDestroyView() {
        recipesScrollState = binding.rvSearchResults.layoutManager?.onSaveInstanceState()
        profilesScrollState = binding.rvprofileusers.layoutManager?.onSaveInstanceState()
        super.onDestroyView()
        loadingDialog.isDismiss()
        _binding = null
    }
}




//package com.infinitybutterfly.infiflyrecipe
//
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import androidx.activity.OnBackPressedCallback
//import androidx.navigation.fragment.findNavController
//
//import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterPopularRecipe
//import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterSearchRecipe
//import com.infinitybutterfly.infiflyrecipe.databinding.FragmentSearchBinding
//import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
//
//class SearchFragment : Fragment() {
//    private var _binding: FragmentSearchBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var rvAdapter: RvAdapterSearchRecipe
//    private var savedMealId: String = ""
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentSearchBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
////        requireActivity().onBackPressedDispatcher.addCallback(
////            viewLifecycleOwner,
////            object : OnBackPressedCallback(true) {
////                override fun handleOnBackPressed() {
////                    // Pop everything off the stack until we hit the Home screen
////                    // Make sure R.id.homeFragment matches your exact ID in your nav_graph.xml
////                    findNavController().popBackStack(R.id.homeFragment, false)
////                }
////            }
////        )
//
//        val searchQuery = arguments?.getString("SEARCH_QUERY") ?: ""
//
//        binding.tvSearchStatus.visibility = View.VISIBLE
//        binding.tvSearchStatus.text = "You have searched for: \"$searchQuery\""
//
//        setupRecyclerView()
//
//        if (searchQuery.isNotEmpty()) {
//            fetchRecipes(searchQuery)
//        }
//    }
//
//    private fun setupRecyclerView() {
//        rvAdapter = RvAdapterSearchRecipe(emptyList()) { clickedRecipe ->
//
//            savedMealId = clickedRecipe.idMeal
//
//            Log.d("CLICK_TEST", "Successfully saved Meal ID: $savedMealId")
//
//            val bundle = Bundle().apply {
//                putString("MEAL_ID", clickedRecipe.idMeal)
//            }
//            findNavController().navigate(R.id.action_searchFragment_to_recipeDetailFragment, bundle)
////            val detailFragment = RecipeDetailFragment().apply {
////                arguments = bundle
////            }
////
////            parentFragmentManager.beginTransaction()
////                .replace(R.id.fragmentContainerView, detailFragment)
////                .addToBackStack(null)
////                .commit()
//        }
//
//        binding.rvSearchResults.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = rvAdapter
//        }
//    }
//
//    private fun fetchRecipes(searchQuery: String) {
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitClient.searchApi.getSearchRecipe(searchQuery)
////                val response2 = RetrofitInstanceProfile.api.searchRecipes(searchQuery)
//
//                if (response.isSuccessful) {
//                    val mealsList = response.body()?.meals ?: emptyList()
//
//                    Log.d("API_SUCCESS", "Successfully fetched ${mealsList.size} meals!")
//
//                    withContext(Dispatchers.Main) {
//                        if (mealsList.isEmpty()) {
//                            binding.textinputError.visibility = View.VISIBLE
//                            binding.textinputError.text = "No recipes found for '$searchQuery'. Try another ingredient!"
//                            rvAdapter.updateData(emptyList())
//                        } else {
//                            binding.textinputError.visibility = View.GONE
//                            rvAdapter.updateData(mealsList)
//                        }
//                    }
//                } else {
//                    Log.e("API_ERROR", "Server responded with error code: ${response.code()}")
//                    withContext(Dispatchers.Main) {
//                        binding.textinputError.visibility = View.VISIBLE
//                        binding.textinputError.text = "Server Error: ${response.code()}"
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("API_CRASH", "Network request failed completely: ${e.message}")
//
//                withContext(Dispatchers.Main) {
//                    binding.textinputError.visibility = View.VISIBLE
//                    binding.textinputError.text = "Network request failed completely: ${e.message}"
////                    binding.textinputError.text = "Error: Please check your internet connection."
//                }
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
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
//// * Use the [SearchFragment.newInstance] factory method to
//// * create an instance of this fragment.
//// */
////class SearchFragment : Fragment() {
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
////        return inflater.inflate(R.layout.fragment_search, container, false)
////    }
////
////    companion object {
////        /**
////         * Use this factory method to create a new instance of
////         * this fragment using the provided parameters.
////         *
////         * @param param1 Parameter 1.
////         * @param param2 Parameter 2.
////         * @return A new instance of fragment SearchFragment.
////         */
////        // TODO: Rename and change types and number of parameters
////        @JvmStatic
////        fun newInstance(param1: String, param2: String) =
////            SearchFragment().apply {
////                arguments = Bundle().apply {
////                    putString(ARG_PARAM1, param1)
////                    putString(ARG_PARAM2, param2)
////                }
////            }
////    }
////}