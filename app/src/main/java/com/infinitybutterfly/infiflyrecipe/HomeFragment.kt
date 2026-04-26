package com.infinitybutterfly.infiflyrecipe

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.activity.OnBackPressedCallback
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.infinitybutterfly.infiflyrecipe.adapters.RecyclerViewSliderAdapter

import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterForYouRecipe
import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterPopularRecipe
import com.infinitybutterfly.infiflyrecipe.databinding.FragmentHomeBinding
import com.infinitybutterfly.infiflyrecipe.utils.LoadingDialog
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient

class HomeFragment : Fragment(), RefreshableFragment {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var sliderAdapter: RecyclerViewSliderAdapter
    private var completedApiCount = 0
    private val totalApisToLoad = 2
    private val sliderHandler = Handler(Looper.getMainLooper())
    private val sliderRunnable = Runnable {
        val totalItems = sliderAdapter.itemCount
        if (totalItems > 0) {
            val currentItem = binding.recyclerviewPopularRecipe.currentItem
            val nextItem = if (currentItem == totalItems - 1) 0 else currentItem + 1
            binding.recyclerviewPopularRecipe.setCurrentItem(nextItem, true)
        }
    }
//    private var savedMealId: String = ""

    private var rvAdapterPR = RvAdapterPopularRecipe(emptyList()) { clickedRecipe ->
//        savedMealId = clickedRecipe.idMeal

//        Can be used on small projects, but not recommended
        val displayId = clickedRecipe.idMeal
        Log.d("CLICKED", "Selected Recipe ID: $displayId")

//        Not Recommended
//        Log.d("CLICKED", "User clicked on Popular recipe: ${clickedRecipe.idMeal} || ${clickedRecipe.id}")
        navigateToDetailFragment(clickedRecipe.idMeal)
    }

    private var rvAdapterFYR = RvAdapterForYouRecipe(emptyList()) { clickedRecipe ->
        val displayId = clickedRecipe.idMeal
        Log.d("CLICKED", "User clicked on For You recipe: $displayId")
        navigateToDetailFragment(clickedRecipe.idMeal)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = LoadingDialog(requireActivity())
        setupSlider()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // This kills the Activity, closing the app completely
                    requireActivity().finish()
                }
            }
        )
//        val navHostFragment = requireActivity().supportFragmentManager
//            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
//        val navController = navHostFragment.navController
//
//        val bottomAppBar = view.findViewById<BottomAppBar>(R.id.bottomAppBar)
//        val bottomNavigationView = view.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
//        val fab = view.findViewById<FloatingActionButton>(R.id.add_button_nav)
//
//        bottomNavigationView.setupWithNavController(navController)
//
//        navController.addOnDestinationChangedListener { _, destination, _ ->
//            when (destination.id) {
//                R.id.homeFragment,
//                R.id.discoverRecipeFragment,
//                R.id.addRecipeFragment,
//                R.id.myRecipeFragment,
//                R.id.profileFragment -> {
//                    bottomAppBar.visibility = View.VISIBLE
//                    fab.show()
//                }
//                else -> {
//                    bottomAppBar.visibility = View.GONE
//                    fab.hide()
//                }
//            }
//        }

        setupRecyclerViewPopular()
        setupRecyclerViewForYou()

        val mainActivity = requireActivity() as MainActivity

        val queryPopular = mainActivity.randomLetterPopular
        val queryForYou = mainActivity.randomLetterForYou

        Log.d("RANDOM_LETTERS", "Popular query: $queryPopular, For You query: $queryForYou")
//        val randomLetter1 = ('a'..'z').random().toString()
//        val randomLetter2 = ('a'..'z').random().toString()
//
//        Log.d("RANDOM_LETTERS", "Popular query: $randomLetter1, For You query: $randomLetter2")

        completedApiCount = 0
        loadingDialog.startLoading()

        fetchRecipesPopular(queryPopular)
        fetchRecipesForYou(queryForYou)

        setupSearchBarLogic()
    }

    private fun checkAndDismissLoader() {
        completedApiCount++
        if (completedApiCount >= totalApisToLoad) {
            loadingDialog.isDismiss()
            (activity as? MainActivity)?.stopRefreshAnimation()
        }
    }

    private fun setupSlider() {
        // Initialize adapter with click listener
        sliderAdapter = RecyclerViewSliderAdapter(emptyList()) { clickedRecipe ->
            Log.d("CLICKED", "User clicked on Popular recipe: ${clickedRecipe.idMeal}")
            navigateToDetailFragment(clickedRecipe.idMeal)
        }

        binding.recyclerviewPopularRecipe.adapter = sliderAdapter

        // Connect the TabLayout (Dots) to the ViewPager2
        TabLayoutMediator(binding.tabLayoutDots, binding.recyclerviewPopularRecipe) { tab, position ->
            // Leave empty. We don't need text on the tabs, just the dots.
        }.attach()

        // Setup Auto-Scroll timing
        binding.recyclerviewPopularRecipe.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 3000) // 3 seconds per slide
            }
        })
    }

    private fun setupRecyclerViewPopular() {
        binding.recyclerviewPopularRecipe.apply {
//            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = rvAdapterPR
        }
    }

    private fun setupRecyclerViewForYou() {
        binding.recyclerviewRecipeForYou.apply {
//            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = rvAdapterFYR
        }
    }

    private fun fetchRecipesPopular(randomLetter1: String) {
//        val mainActivity = activity as? MainActivity

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.popularApi.getPopularRecipe(randomLetter1)

                withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val mealsList = response.body()?.meals ?: emptyList()
                    Log.d("API_SUCCESS", "Successfully fetched ${mealsList.size} popular meals!")

                    withContext(Dispatchers.Main) {
                        rvAdapterPR.updateData(mealsList)
                        sliderAdapter.updateData(mealsList)
                    }
                } else {
                    Log.e("API_ERROR", "Popular Server error code: ${response.code()}")
                } }
            } catch (e: Exception) {
                Log.e("API_CRASH", "Popular Network request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.textView5.visibility = View.VISIBLE
                    binding.textView5.text = "Error: ${e.message}"
                }
            }finally  {
//                mainActivity?.stopRefreshAnimation()
                withContext(Dispatchers.Main) {
                    if (isAdded) checkAndDismissLoader()
                }
            }
        }
    }

    private fun fetchRecipesForYou(randomLetter2: String) {
//        val mainActivity = activity as? MainActivity

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.forYouApi.getForYouRecipe(randomLetter2)

                withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val mealsList = response.body()?.meals ?: emptyList()
                    Log.d("API_SUCCESS", "Successfully fetched ${mealsList.size} for you meals!")

                    withContext(Dispatchers.Main) {
                        rvAdapterFYR.updateData(mealsList)
                    }
                } else {
                    Log.e("API_ERROR", "For You Server error code: ${response.code()}")
                } }
            } catch (e: Exception) {
                Log.e("API_CRASH", "For You Network request failed: ${e.message}")
            } finally {
//                mainActivity?.stopRefreshAnimation()
                withContext(Dispatchers.Main) {
                    if (isAdded) checkAndDismissLoader()
                }
            }
        }
    }

    private fun navigateToDetailFragment(mealId: String) {
        val bundle = Bundle().apply {
            putString("MEAL_ID", mealId)
        }

        findNavController().navigate(R.id.action_homeFragment_to_recipeDetailFragment, bundle)
//        val detailFragment = RecipeDetailFragment().apply {
//            arguments = bundle
//        }

//        parentFragmentManager.beginTransaction()
//            .replace(R.id.fragmentContainerView, detailFragment)
//            .addToBackStack(null)
//            .commit()
    }

    private fun navigateToSearchFragment(query: String) {
        val bundle = Bundle().apply {
            putString("SEARCH_QUERY", query)
        }

        findNavController().navigate(R.id.action_homeFragment_to_searchFragment, bundle)
//        val targetFragment = SearchFragment().apply {
//            arguments = bundle
//        }
//
//        parentFragmentManager.beginTransaction()
//            .replace(R.id.fragmentContainerView, targetFragment)
//            .addToBackStack(null)
//            .commit()
    }
    private fun setupSearchBarLogic() {

        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.add_button_nav)

        binding.searchBar.setOnClickListener {
            binding.searchOverlay.visibility = View.VISIBLE
            binding.realSearchInput.requestFocus()

            populateRecentSearches()

            bottomNav?.visibility = View.GONE
            fab?.hide()

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.realSearchInput, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.btnBack.setOnClickListener {
            binding.searchOverlay.visibility = View.GONE
            binding.realSearchInput.text?.clear()
            binding.realSearchInput.clearFocus()

            bottomNav?.visibility = View.VISIBLE
            fab?.show()

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.realSearchInput.windowToken, 0)
        }

        binding.searchOverlay.setOnClickListener {
            binding.btnBack.performClick()
        }

        binding.realSearchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {

                val userQuery: String = binding.realSearchInput.text.toString().trim()

                if (userQuery.isNotEmpty()) {
                    Log.d("USER_INPUT", "Navigating to search results for: $userQuery")

                    binding.btnBack.performClick()

                    navigateToSearchFragment(userQuery)
                } else {
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.realSearchInput.windowToken, 0)
                }

                true
            } else {
                false
            }
        }
    }

    private fun populateRecentSearches() {
        // 1. Fetch the history from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val historyString = sharedPref.getString("SEARCH_HISTORY", "") ?: ""

        val recentSearches = if (historyString.isEmpty()) {
            emptyList()
        } else {
            historyString.split(";;;")
        }

        // 2. Clear out any old chips before adding new ones
        binding.recentSearch.removeAllViews()

        // 3. Hide the ChipGroup if there is no history yet
        if (recentSearches.isEmpty()) {
            binding.recentSearch.visibility = View.GONE
            return
        }

        binding.recentSearch.visibility = View.VISIBLE

        // 4. Generate a Chip for each saved search
        for (query in recentSearches) {
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = query

                // Style it exactly like your XML!
                setChipBackgroundColorResource(android.R.color.white)
                setChipIconResource(R.drawable.archive) // Replace with your actual drawable if needed
                chipStrokeWidth = 3f // Roughly 1dp in pixels
                chipStrokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DDDDDD"))

                // 5. Make the chip clickable!
                setOnClickListener {
                    // When clicked, put the text into the search bar
                    binding.realSearchInput.setText(query)

                    // Close the overlay and immediately trigger the search
                    binding.btnBack.performClick()
                    navigateToSearchFragment(query)
                }
            }

            // Add the dynamically created chip to the group
            binding.recentSearch.addView(chip)
        }
    }

//    private fun setupSearchBarLogic() {
//        // 1. Click Dummy Search to OPEN overlay
//        binding.searchBar.setOnClickListener {
//            binding.searchOverlay.visibility = View.VISIBLE
//            binding.realSearchInput.requestFocus()
//
//            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(binding.realSearchInput, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        // 2. Click Back to CLOSE overlay
//        binding.btnBack.setOnClickListener {
//            binding.searchOverlay.visibility = View.GONE
//            binding.realSearchInput.text?.clear()
//            binding.realSearchInput.clearFocus()
//
//            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.hideSoftInputFromWindow(binding.realSearchInput.windowToken, 0)
//        }
//
//        // Optional: Close overlay if user clicks the dark background outside the white menu
//        binding.searchOverlay.setOnClickListener {
//            binding.btnBack.performClick()
//        }
//        binding.realSearchInput.setOnEditorActionListener { _, actionId, event ->
//            // Check if the user pressed the "Search", "Done", or "Enter" key
//            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
//                actionId == EditorInfo.IME_ACTION_DONE ||
//                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//
//                // 1. STORE THE STRING TO A VARIABLE
//                val userQuery: String = binding.realSearchInput.text.toString()
//
//                // 2. Do something with the variable! (e.g., Log it, or search your API)
//                Log.d("USER_INPUT", "The user typed: $userQuery")
//
//                // Optional: Hide the keyboard after they press enter
//                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.hideSoftInputFromWindow(binding.realSearchInput.windowToken, 0)
//
//                // Return true to indicate we handled the action
//                true
//            } else {
//                // Return false if we didn't handle it
//                false
//            }
//        }
//    }

    override fun onRefreshAction() {
        val mainActivity = activity as? MainActivity

        val letterPopular = mainActivity?.randomLetterPopular ?: "a"
        val letterForYou = mainActivity?.randomLetterForYou ?: "z"

        completedApiCount = 0
        loadingDialog.startLoading()

        fetchRecipesPopular(randomLetter1 = letterPopular)
        fetchRecipesForYou(randomLetter2 = letterForYou)

//        fetchRecipesPopular(randomLetter1 = "a")
//        fetchRecipesForYou(randomLetter2 = "z")

//        Generate New Letter every time refreshed
//        val freshLetterPopular = ('a'..'z').random().toString()
//        val freshLetterForYou = ('a'..'z').random().toString()
//
//        fetchRecipesPopular(randomLetter1 = freshLetterPopular)
//        fetchRecipesForYou(randomLetter2 = freshLetterForYou)
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    override fun onResume() {
        super.onResume()
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog.isDismiss()
        // Prevent memory leaks by nullifying binding when the view is destroyed
        _binding = null
    }
}



//package com.infinitybutterfly.infiflyrecipe
//
//import android.content.Context
//import android.os.Bundle
//import android.util.Log
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.view.inputmethod.InputMethodManager
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.lifecycle.lifecycleScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterForYouRecipe
//import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterPopularRecipe
//import com.infinitybutterfly.infiflyrecipe.databinding.FragmentHomeBinding
//import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsForYou
//import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsPopular
//import com.infinitybutterfly.infiflyrecipe.utils.RetrofitInstanceForYou
//import com.infinitybutterfly.infiflyrecipe.utils.RetrofitInstancePopular
//
//class HomeFragment : Fragment() {
//
//    private var _binding: FragmentHomeBinding? = null
//    // This property is only valid between onCreateView and onDestroyView.
//    private val binding get() = _binding!!
//
////    private var rvAdapterPR = RvAdapterPopularRecipe(emptyList<RecipeItemsPopular>())
////    private var rvAdapterFYR = RvAdapterForYouRecipe(emptyList<RecipeItemsForYou>())
////
//        // Initialize the Popular adapter with the click listener lambda
//        private var rvAdapterPR = RvAdapterPopularRecipe(emptyList()) { clickedRecipe ->
//            // EVERYTHING IN HERE HAPPENS WHEN AN ITEM IS CLICKED
//
//            Log.d("CLICKED", "User clicked on recipe ID: ${clickedRecipe.strMealThumb}")
//
//            // Example: Show a quick popup message
//            android.widget.Toast.makeText(
//                requireContext(),
//                "Clicked: ${clickedRecipe.strMeal}",
//                android.widget.Toast.LENGTH_SHORT
//            ).show()
//
//            // NEXT STEP: Navigate to a Detail Fragment
//            // val bundle = Bundle().apply { putString("RECIPE_ID", clickedRecipe.idMeal) }
//            // val detailFragment = RecipeDetailFragment().apply { arguments = bundle }
//            // parentFragmentManager.beginTransaction()
//            //     .replace(R.id.fragment_container, detailFragment)
//            //     .addToBackStack(null)
//            //     .commit()
//        }
//
//        // Initialize the For You adapter with its own click listener
////        private var rvAdapterFYR = RvAdapterForYouRecipe(emptyList()) { clickedRecipe ->
////            Log.d("CLICKED", "User clicked For You recipe: ${clickedRecipe.strMeal}")
////            // Handle navigation here...
////        }
////
////        // ... rest of your Fragment code (onCreateView, onViewCreated, etc.) ...
////    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // 1. Setup your RecyclerViews
//        setupRecyclerViewPopular()
//        setupRecyclerViewForYou()
//
//        // 2. Fetch data for BOTH lists
//        fetchRecipesPopular()
//        fetchRecipesForYou() // <-- This is what was missing!
//
//        // 3. Setup your UI Click Listeners (Using ViewBinding!)
//        setupSearchBarLogic()
//    }
//
//    private fun setupRecyclerViewPopular() {
//        binding.recyclerviewPopularRecipe.apply {
//            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//            adapter = rvAdapterPR
//
//        }
//    }
//
//    private fun setupRecyclerViewForYou() {
//        binding.recyclerviewRecipeForYou.apply {
////            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//            adapter = rvAdapterFYR
//        }
//    }
//
//    private fun fetchRecipesPopular() {
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitInstancePopular.api.getPopularRecipe()
//
//                if (response.isSuccessful) {
//                    val mealsList = response.body()?.meals ?: emptyList<RecipeItemsPopular>()
//                    Log.d("API_SUCCESS", "Successfully fetched ${mealsList.size} popular meals!")
//
//                    withContext(Dispatchers.Main) {
////                        rvAdapterPR.updateData(mealsList)
//                    }
//                } else {
//                    Log.e("API_ERROR", "Popular Server error code: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("API_CRASH", "Popular Network request failed: ${e.message}")
//                withContext(Dispatchers.Main) {
//                    binding.textView5.visibility = View.VISIBLE
//                    binding.textView5.text = "Error: ${e.message}"
//                }
//            }
//        }
//    }
//
//    // YOU NEED TO FILL THIS IN:
//    // This is where you actually fetch the data for the "For You" list
//    private fun fetchRecipesForYou() {
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitInstanceForYou.api.getForYouRecipe()
//
//                if (response.isSuccessful) {
//                    val mealsList = response.body()?.meals ?: emptyList<RecipeItemsForYou>()
//                    Log.d("API_SUCCESS", "Successfully fetched ${mealsList.size} popular meals!")
//
//                    withContext(Dispatchers.Main) {
////                        rvAdapterFYR.updateData(mealsList)
//                    }
//                } else {
//                    Log.e("API_ERROR", "Popular Server error code: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("API_CRASH", "For You Network request failed: ${e.message}")
//            }
//        }
//    }
//
//    private fun navigateToDetailFragment(recipeName: String) {
//        // 1. Create the bundle to pass data to the new fragment
//        val bundle = Bundle().apply {
//            putString("RECIPE_NAME", recipeName)
//        }
//
//        // 2. Create the instance of your DetailFragment and attach the bundle
//        val detailFragment = RecipeDetailFragment().apply {
//            arguments = bundle
//        }
//
//        // 3. Perform the Fragment Transaction
//        parentFragmentManager.beginTransaction()
//            // Replace 'R.id.fragment_container' with the actual ID of the
//            // FrameLayout/FragmentContainerView in your MainActivity's layout!
//            .replace(R.id.fragmentContainerView, detailFragment)
//            .addToBackStack(null) // This allows the user to press the 'Back' button to return to Home
//            .commit()
//    }
//    // Initialize the Popular adapter
//    private var rvAdapterPR = RvAdapterPopularRecipe(emptyList()) { clickedRecipe ->
//        Log.d("CLICKED", "User clicked on Popular recipe: ${clickedRecipe.strMeal}")
//        navigateToDetailFragment(clickedRecipe.strMeal)
//    }
//
//    // Initialize the For You adapter
//    private var rvAdapterFYR = RvAdapterForYouRecipe(emptyList()) { clickedRecipe ->
//        Log.d("CLICKED", "User clicked on For You recipe: ${clickedRecipe.strMeal}")
//        navigateToDetailFragment(clickedRecipe.strMeal)
//    }
//
//    private fun setupSearchBarLogic() {
//        // Since you are using ViewBinding, you don't need findViewById!
//        // Just use `binding.idName`
//
//        // 1. Click Dummy Search to OPEN overlay
//        binding.searchBar.setOnClickListener {
//            binding.searchOverlay.visibility = View.VISIBLE
//            binding.realSearchInput.requestFocus()
//
//            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(binding.realSearchInput, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        // 2. Click Back to CLOSE overlay
//        binding.btnBack.setOnClickListener {
//            binding.searchOverlay.visibility = View.GONE
//            binding.realSearchInput.text?.clear()
//            binding.realSearchInput.clearFocus()
//
//            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.hideSoftInputFromWindow(binding.realSearchInput.windowToken, 0)
//        }
//
//        // Optional: Close overlay if user clicks the dark background outside the white menu
//        binding.searchOverlay.setOnClickListener {
//            binding.btnBack.performClick()
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        // Prevent memory leaks by nullifying binding when the view is destroyed
//        _binding = null
//    }
//}
//
//
////package com.infinitybutterfly.infiflyrecipe
////
////import android.content.Context
////import android.os.Bundle
////import android.util.Log
////import androidx.fragment.app.Fragment
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////import android.view.inputmethod.InputMethodManager
////import android.widget.EditText
////import android.widget.ImageView
////import kotlinx.coroutines.Dispatchers
////import kotlinx.coroutines.launch
////import kotlinx.coroutines.withContext
////import androidx.recyclerview.widget.LinearLayoutManager
////import androidx.constraintlayout.widget.ConstraintLayout
////import androidx.lifecycle.lifecycleScope
//////import androidx.databinding.tool.Context
////import com.google.android.material.search.SearchBar
////import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterForYouRecipe
////import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterPopularRecipe
////import com.infinitybutterfly.infiflyrecipe.databinding.FragmentHomeBinding
////import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsForYou
////import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsPopular
////import com.infinitybutterfly.infiflyrecipe.utils.RetrofitInstancePopular
////
////class HomeFragment : Fragment() {
////
////    private var _binding: FragmentHomeBinding? = null
////    private val binding get() = _binding!!
////
////    // Pass the specific type to emptyList
////    private var rvAdapterPR = RvAdapterPopularRecipe(emptyList<RecipeItemsPopular>())
////    private var rvAdapterFYR = RvAdapterForYouRecipe(emptyList<RecipeItemsForYou>())
////
////    override fun onCreateView(
////        inflater: LayoutInflater, container: ViewGroup?,
////        savedInstanceState: Bundle?
////    ): View? {
////        // Inflate the layout for this fragment
////        return inflater.inflate(R.layout.fragment_home, container, false)
////    }
////
////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////        super.onViewCreated(view, savedInstanceState)
////
////        super.onViewCreated(view, savedInstanceState)
////        _binding = FragmentHomeBinding.bind(view)
////
////        setupRecyclerViewPopular()
////        setupRecyclerViewForYou()
////        fetchRecipesPopular()
////    }
////
////    private fun setupRecyclerViewPopular() {
////        binding.recyclerviewPopularRecipe.apply {
////            layoutManager =
////                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
////            adapter = rvAdapterPR
////        }
////    }
////    private fun setupRecyclerViewForYou() {
////        binding.recyclerviewRecipeForYou.apply {
////            layoutManager =
////                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
////            adapter = rvAdapterFYR
////        }
////    }
////
////    private fun fetchRecipesPopular() {
////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////            try {
////                val response = RetrofitInstancePopular.api.getPopularRecipe()
////
////                if (response.isSuccessful) {
////                    val mealsList = response.body()?.meals ?: emptyList<RecipeItemsPopular>()
////
////                    Log.d("API_SUCCESS", "Successfully fetched ${mealsList.size} meals!")
////
////                    withContext(Dispatchers.Main) {
////                        rvAdapterPR.updateData(mealsList)
////                    }
////                } else {
////                    Log.e("API_ERROR", "Server responded with error code: ${response.code()}")
////                }
////            } catch (e: Exception) {
////                // ADD THIS LOG: It will print to Logcat if you have no internet or the URL is wrong
////                Log.e("API_CRASH", "Network request failed completely: ${e.message}")
////
////                withContext(Dispatchers.Main) {
////                    _binding?.let {
////                        it.textView5.visibility = View.VISIBLE
////                        it.textView5.text = "Error: ${e.message}"
////                    }
////                }
////            }
//////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//////        super.onViewCreated(view, savedInstanceState)
//////        _binding = FragmentHomeBinding.bind(view)
//////
//////        setupRecyclerView()
//////        fetchRecipes()
//////    }
//////
//////    override fun onCreate(savedInstanceState: Bundle?) {
//////        super.onCreate( savedInstanceState)
////            // Notice we call findViewById on the 'view' parameter passed into this function
////            val dummySearchBar = view?.findViewById<SearchBar>(R.id.searchBar)
////            val searchOverlay = view?.findViewById<ConstraintLayout>(R.id.searchOverlay)
////            val btnBack = view?.findViewById<ImageView>(R.id.btnBack)
////            val realSearchInput = view?.findViewById<EditText>(R.id.realSearchInput)
////
////            // 1. Click Dummy Search to OPEN overlay
////            dummySearchBar?.setOnClickListener {
////                // Show the overlay
////                searchOverlay?.visibility = View.VISIBLE
////
////                // Focus on the real EditText
////                realSearchInput?.requestFocus()
////
////                // Pop up the keyboard automatically (using requireContext() for Fragment)
////                val imm =
////                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////                imm.showSoftInput(realSearchInput, InputMethodManager.SHOW_IMPLICIT)
////            }
////
////            // 2. Click Back to CLOSE overlay
////            btnBack?.setOnClickListener {
////                // Hide the overlay
////                searchOverlay?.visibility = View.GONE
////
////                // Clear text and focus
////                realSearchInput?.text?.clear()
////                realSearchInput?.clearFocus()
////
////                // Hide the keyboard (using requireContext() for Fragment)
////                val imm =
////                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////                imm.hideSoftInputFromWindow(realSearchInput?.windowToken, 0)
////            }
////
////            // Optional: Close overlay if user clicks the dark background outside the white menu
////            searchOverlay?.setOnClickListener {
////                btnBack?.performClick()
////            }
////        }
////    }
////}
////
////
////
////
////
////
//////    private fun setupRecyclerView() {
//////        // Removed the "this?." - binding.rFYRecyclerview is guaranteed by ViewBinding
//////        binding.rFYRecyclerview.apply {
//////            this?.layoutManager = LinearLayoutManager(requireContext())
//////            this?.adapter = rvAdapter
//////        }
//////    }
////
////
////
////
////
////
////
//////package com.infinitybutterfly.infiflyrecipe
//////
//////import android.os.Bundle
//////import androidx.fragment.app.Fragment
//////import android.view.LayoutInflater
//////import android.view.View
//////import android.view.ViewGroup
//////
//////// TODO: Rename parameter arguments, choose names that match
//////// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//////private const val ARG_PARAM1 = "param1"
//////private const val ARG_PARAM2 = "param2"
//////
///////**
////// * A simple [Fragment] subclass.
////// * Use the [HomeFragment.newInstance] factory method to
////// * create an instance of this fragment.
////// */
//////class HomeFragment : Fragment() {
//////    // TODO: Rename and change types of parameters
//////    private var param1: String? = null
//////    private var param2: String? = null
//////
//////    override fun onCreate(savedInstanceState: Bundle?) {
//////        super.onCreate(savedInstanceState)
//////        arguments?.let {
//////            param1 = it.getString(ARG_PARAM1)
//////            param2 = it.getString(ARG_PARAM2)
//////        }
//////    }
//////
//////    override fun onCreateView(
//////        inflater: LayoutInflater, container: ViewGroup?,
//////        savedInstanceState: Bundle?
//////    ): View? {
//////        // Inflate the layout for this fragment
//////        return inflater.inflate(R.layout.fragment_home, container, false)
//////    }
//////
//////    companion object {
//////        /**
//////         * Use this factory method to create a new instance of
//////         * this fragment using the provided parameters.
//////         *
//////         * @param param1 Parameter 1.
//////         * @param param2 Parameter 2.
//////         * @return A new instance of fragment HomeFragment.
//////         */
//////        // TODO: Rename and change types and number of parameters
//////        @JvmStatic
//////        fun newInstance(param1: String, param2: String) =
//////            HomeFragment().apply {
//////                arguments = Bundle().apply {
//////                    putString(ARG_PARAM1, param1)
//////                    putString(ARG_PARAM2, param2)
//////                }
//////            }
//////    }
//////}



////////////Above Version Contains both recyclerview and viewpager2 for popular recipes, it can crash anytime
//
////////////More Cleaner Version Below Will Use it if app starts failing
//
//package com.infinitybutterfly.infiflyrecipe
//
//import android.content.Context
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.view.inputmethod.InputMethodManager
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.lifecycle.lifecycleScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import android.view.inputmethod.EditorInfo
//import android.view.KeyEvent
//import androidx.navigation.fragment.findNavController
//import androidx.activity.OnBackPressedCallback
//import androidx.viewpager2.widget.ViewPager2
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import com.google.android.material.tabs.TabLayoutMediator
//
//import com.infinitybutterfly.infiflyrecipe.adapters.RecyclerViewSliderAdapter
//import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterForYouRecipe
//import com.infinitybutterfly.infiflyrecipe.databinding.FragmentHomeBinding
//import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
//
//class HomeFragment : Fragment(), RefreshableFragment {
//
//    private var _binding: FragmentHomeBinding? = null
//    private val binding get() = _binding!!
//
//    // --- Slider Variables ---
//    private lateinit var sliderAdapter: RecyclerViewSliderAdapter
//    private val sliderHandler = Handler(Looper.getMainLooper())
//    private val sliderRunnable = Runnable {
//        val totalItems = sliderAdapter.itemCount
//        if (totalItems > 0) {
//            val currentItem = binding.recyclerviewPopularRecipe.currentItem
//            val nextItem = if (currentItem == totalItems - 1) 0 else currentItem + 1
//            binding.recyclerviewPopularRecipe.setCurrentItem(nextItem, true)
//        }
//    }
//
//    // --- For You Adapter ---
//    private var rvAdapterFYR = RvAdapterForYouRecipe(emptyList()) { clickedRecipe ->
//        Log.d("CLICKED", "User clicked on For You recipe: ${clickedRecipe.idMeal}")
//        navigateToDetailFragment(clickedRecipe.idMeal)
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Setup UI Components
//        setupSlider()
//        setupRecyclerViewForYou() // Only For You needs setup now, Slider is handled above!
//        setupSearchBarLogic()
//
//        requireActivity().onBackPressedDispatcher.addCallback(
//            viewLifecycleOwner,
//            object : OnBackPressedCallback(true) {
//                override fun handleOnBackPressed() {
//                    requireActivity().finish()
//                }
//            }
//        )
//
//        // Fetch Data
//        val mainActivity = requireActivity() as MainActivity
//        val queryPopular = mainActivity.randomLetterPopular
//        val queryForYou = mainActivity.randomLetterForYou
//
//        fetchRecipesPopular(queryPopular)
//        fetchRecipesForYou(queryForYou)
//    }
//
//    private fun setupSlider() {
//        // 1. Initialize the adapter
//        sliderAdapter = RecyclerViewSliderAdapter(emptyList()) { clickedRecipe ->
//            Log.d("CLICKED", "User clicked on Popular recipe: ${clickedRecipe.idMeal}")
//            navigateToDetailFragment(clickedRecipe.idMeal)
//        }
//
//        binding.recyclerviewPopularRecipe.adapter = sliderAdapter
//
//        // 2. Connect the dots (TabLayout) to the ViewPager2
//        TabLayoutMediator(binding.tabLayoutDots, binding.recyclerviewPopularRecipe) { _, _ ->
//            // Leave empty - we only want dots, no text!
//        }.attach()
//
//        // 3. Setup Auto-Scroll logic
//        binding.recyclerviewPopularRecipe.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                sliderHandler.removeCallbacks(sliderRunnable)
//                sliderHandler.postDelayed(sliderRunnable, 3000) // 3 seconds per slide
//            }
//        })
//    }
//
//    private fun setupRecyclerViewForYou() {
//        binding.recyclerviewRecipeForYou.apply {
//            adapter = rvAdapterFYR
//        }
//    }
//
//    private fun fetchRecipesPopular(randomLetter1: String) {
//        val mainActivity = activity as? MainActivity
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitClient.popularApi.getPopularRecipe(randomLetter1)
//
//                if (response.isSuccessful) {
//                    val mealsList = response.body()?.meals ?: emptyList()
//
//                    withContext(Dispatchers.Main) {
//                        // Pass data to the SLIDER adapter, not the old deleted one!
//                        sliderAdapter.updateData(mealsList)
//                    }
//                } else {
//                    Log.e("API_ERROR", "Popular Server error code: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("API_CRASH", "Popular Network request failed: ${e.message}")
//            } finally {
//                mainActivity?.stopRefreshAnimation()
//            }
//        }
//    }
//
//    private fun fetchRecipesForYou(randomLetter2: String) {
//        val mainActivity = activity as? MainActivity
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitClient.forYouApi.getForYouRecipe(randomLetter2)
//
//                if (response.isSuccessful) {
//                    val mealsList = response.body()?.meals ?: emptyList()
//
//                    withContext(Dispatchers.Main) {
//                        rvAdapterFYR.updateData(mealsList)
//                    }
//                } else {
//                    Log.e("API_ERROR", "For You Server error code: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("API_CRASH", "For You Network request failed: ${e.message}")
//            } finally {
//                mainActivity?.stopRefreshAnimation()
//            }
//        }
//    }
//
//    private fun navigateToDetailFragment(mealId: String) {
//        val bundle = Bundle().apply {
//            putString("MEAL_ID", mealId)
//        }
//        findNavController().navigate(R.id.action_homeFragment_to_recipeDetailFragment, bundle)
//    }
//
//    private fun navigateToSearchFragment(query: String) {
//        val bundle = Bundle().apply {
//            putString("SEARCH_QUERY", query)
//        }
//        findNavController().navigate(R.id.action_homeFragment_to_searchFragment, bundle)
//    }
//
//    private fun setupSearchBarLogic() {
//        binding.searchBar.setOnClickListener {
//            binding.searchOverlay.visibility = View.VISIBLE
//            binding.realSearchInput.requestFocus()
//
//            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(binding.realSearchInput, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        binding.btnBack.setOnClickListener {
//            binding.searchOverlay.visibility = View.GONE
//            binding.realSearchInput.text?.clear()
//            binding.realSearchInput.clearFocus()
//
//            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.hideSoftInputFromWindow(binding.realSearchInput.windowToken, 0)
//        }
//
//        binding.searchOverlay.setOnClickListener {
//            binding.btnBack.performClick()
//        }
//
//        binding.realSearchInput.setOnEditorActionListener { _, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
//                actionId == EditorInfo.IME_ACTION_DONE ||
//                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//
//                val userQuery: String = binding.realSearchInput.text.toString().trim()
//
//                if (userQuery.isNotEmpty()) {
//                    binding.btnBack.performClick()
//                    navigateToSearchFragment(userQuery)
//                } else {
//                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                    imm.hideSoftInputFromWindow(binding.realSearchInput.windowToken, 0)
//                }
//                true
//            } else {
//                false
//            }
//        }
//    }
//
//    override fun onRefreshAction() {
//        val mainActivity = activity as? MainActivity
//        val letterPopular = mainActivity?.randomLetterPopular ?: "a"
//        val letterForYou = mainActivity?.randomLetterForYou ?: "z"
//
//        fetchRecipesPopular(randomLetter1 = letterPopular)
//        fetchRecipesForYou(randomLetter2 = letterForYou)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        sliderHandler.removeCallbacks(sliderRunnable) // Stop slider when leaving screen
//    }
//
//    override fun onResume() {
//        super.onResume()
//        sliderHandler.postDelayed(sliderRunnable, 3000) // Restart slider when returning
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        sliderHandler.removeCallbacks(sliderRunnable)
//        _binding = null
//    }
//}