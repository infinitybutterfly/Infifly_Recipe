package com.infinitybutterfly.infiflyrecipe

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.setFragmentResultListener
import com.bumptech.glide.Glide

import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterDetailsRecipe
import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterUserDetailsRecipe
import com.infinitybutterfly.infiflyrecipe.databinding.FragmentRecipeDetailBinding
import com.infinitybutterfly.infiflyrecipe.utils.LoadingDialog
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient

class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog

    // 1. Declare your new adapter here
    private lateinit var detailAdapter: RvAdapterDetailsRecipe
    private lateinit var detailuserAdapter: RvAdapterUserDetailsRecipe

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener("premium_upgrade") { _, bundle ->
            val isSuccess = bundle.getBoolean("success")
            if (isSuccess) {
                // The user just paid! Refresh the profile to unlock the Edit button.
                fetchKtorRecipeDetails(id = "MEAL_ID")
                loadingDialog.isDismiss()
            }
        }

        loadingDialog = LoadingDialog(requireActivity())

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })

        // 3. Get the ID and fetch the data
        val mealId = arguments?.getString("MEAL_ID")
        val is_my_backend = arguments?.getBoolean("IS_MY_BACKEND")?: false

        // 2. Setup the RecyclerView FIRST
        setupRecyclerView(is_my_backend)

        if (mealId != null) {
            if (is_my_backend) {
                fetchKtorRecipeDetails(mealId)
            } else {
                fetchMealDbDetails(mealId)
            }
        }

//        if (mealId != null) {
//            Log.d("DETAIL_SCREEN", "Ready to fetch details for ID: $mealId")
//            Log.d("DETAIL_SCREEN", "Is from my backend check working and getting the value: $is_my_backend")
//            fetchRecipeDetails(mealId)
//        } else {
//            Log.e("DETAIL_SCREEN", "Error: No Meal ID was passed to this fragment!")
//        }
    }

//    private fun setupRecyclerView() {
//        // Initialize the adapter with an empty list
//        detailAdapter = RvAdapterDetailsRecipe(emptyList())
//
//        // Attach it to the RecyclerView in your fragment_recipe_detail.xml
//        // NOTE: Make sure the ID below matches the RecyclerView ID in your XML layout!
//        binding.recyclerviewRecipeDetails.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = detailAdapter
//        }
//    }
private fun setupRecyclerView(is_my_backend: Boolean) {
    binding.recyclerviewRecipeDetails.layoutManager = LinearLayoutManager(requireContext())

    if (is_my_backend) {
        detailuserAdapter = RvAdapterUserDetailsRecipe(emptyList())
        binding.recyclerviewRecipeDetails.adapter = detailuserAdapter
    } else {
        detailAdapter = RvAdapterDetailsRecipe(emptyList())
        binding.recyclerviewRecipeDetails.adapter = detailAdapter
    }
}

//    private fun fetchRecipeDetails(id: String) {
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitClient.detailsApi.getDetailRecipe(id)
//
//                if (response.isSuccessful) {
//                    // Get the list of meals (usually just 1 for a detail lookup)
//                    val mealsList = response.body()?.meals ?: emptyList()
//
//                    if (mealsList.isNotEmpty()) {
//                        Log.d("API_SUCCESS", "Fetched details for: ${mealsList[0].strMeal}")
//
//                        withContext(Dispatchers.Main) {
//                            // 4. Pass the data straight to your adapter!
//                            detailAdapter.updateData(mealsList)
//                        }
//                    }
//                } else {
//                    Log.e("API_ERROR", "Server responded with error code: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("API_CRASH", "Network request failed: ${e.message}")
//            }
//        }
//    }

    // --- WORK FOR MEALDB (is_my_backend = false) ---
    private fun fetchMealDbDetails(id: String) {
        loadingDialog.startLoading()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.detailsApi.getDetailRecipe(id)
                if (response.isSuccessful) {
                    val mealsList = response.body()?.meals ?: emptyList()
                    withContext(Dispatchers.Main) {
                        detailAdapter.updateData(mealsList)
                    }
                } else {
                    Log.e("API_ERROR", "Server responded with error code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("API_CRASH", "MealDB fetch failed: ${e.message}")
            } finally {
                // 2. DISMISS LOADER
                withContext(Dispatchers.Main) {
                    loadingDialog.isDismiss()
                }
            }
        }
    }

    // --- WORK FOR KTOR BACKEND (is_my_backend = true) ---

    private fun fetchKtorRecipeDetails(id: String) {
        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("JWT_TOKEN", "") ?: ""

        loadingDialog.startLoading()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.ktorApi.getRecipeById("Bearer $token", id)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val recipe = response.body()!!

                        detailuserAdapter.updateData(listOf(recipe))

                        val numbercount = recipe.viewcount>=2
                        // 2. If Ktor flagged this as locked, slide up the Paywall!
                        if (numbercount) {
                            showSubscribeDialog()
                        }

                    } else {
                        Toast.makeText(requireContext(), "Failed to load recipe", Toast.LENGTH_SHORT).show()
                        Log.e("API_ERROR", "Server responded with error code: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("API_CRASH", "Ktor fetch failed: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.isDismiss()
                }
            }
        }
    }

//    @RequiresApi(Build.VERSION_CODES.S)
//    private fun fetchKtorRecipeDetails(id: String) {
//        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
//        val token = sharedPref.getString("JWT_TOKEN", "") ?: ""
//
//        loadingDialog.startLoading()
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitClient.ktorApi.getRecipeById("Bearer $token", id)
//
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful && response.body() != null) {
//                        val recipe = response.body()!!
//
//                        // 1. ALWAYS populate the UI (This loads the teaser image and title in the background)
//                        binding.recipeTitle.text = recipe.name
//                        Glide.with(requireContext()).load(recipe.imageUrl).into(binding.recipeImage)
//
//                        // 2. Check if it is locked!
//                        if (recipe.isLocked) {
//                            // Hide the ingredients/instructions text boxes entirely (or blur them)
//                            binding.instructionsCard.visibility = View.VISIBLE
//                            val blurEffectinstructions = RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.CLAMP)
//                            binding.instructionsCard.setRenderEffect(blurEffectinstructions)
//
//                            binding.ingredientsCard.visibility = View.VISIBLE
//                            val blurEffectingredients = RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.CLAMP)
//                            binding.instructionsCard.setRenderEffect(blurEffectingredients)
//
//                            // Pop the Bottom Sheet over the beautiful background!
//                            showSubscribeDialog()
//                        } else {
//                            // It's unlocked! Show the full details.
//                            binding.instructionsCard.visibility = View.VISIBLE
//                            binding.ingredientsCard.visibility = View.VISIBLE
//                            binding.tvInstructions.text = recipe.instructions
//                            // ... load rest of data
//                        }
//
//                    } else {
//                        // Server rejected it (e.g., 404 Not Found)
//                        Toast.makeText(requireContext(), "Failed to load recipe", Toast.LENGTH_SHORT).show()
//                        Log.e("API_ERROR", "Server responded with error code: ${response.code()}")
//                    }
//                }
//            } catch (e: Exception) {
//                // Network crash (e.g., No internet connection)
//                withContext(Dispatchers.Main) {
//                    Log.e("API_CRASH", "Ktor fetch failed: ${e.message}")
//                }
//            } finally {
//                // 4. ALWAYS dismiss the loader, whether it succeeded or crashed!
//                withContext(Dispatchers.Main) {
//                    loadingDialog.isDismiss()
//                }
//            }
//        }
//
////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////            try {
////                // Assuming you have an endpoint like getRecipeById in your ktorApi
////                val response = RetrofitClient.ktorApi.getRecipeById("Bearer $token", id)
////
////                if (response.isSuccessful) {
////                    // Adapt this part based on your Ktor response model (e.g., RecipeResponse)
////                    val recipeResult = response.body()?.let { listOf(it) } ?: emptyList()
////
////                    withContext(Dispatchers.Main) {
////                        detailuserAdapter.updateData(recipeResult)
////                    }
////                } else {
////                    Log.e("API_ERROR", "Server responded with error code: ${response.code()}")
////                }
////            } catch (e: Exception) {
////                Log.e("API_CRASH", "Ktor fetch failed: ${e.message}")
////            } finally {
////                // 2. DISMISS LOADER
////                withContext(Dispatchers.Main) {
////                    loadingDialog.isDismiss()
////                }
////            }
////        }
//    }

    private fun showSubscribeDialog() {
        val bottomSheet = SubscriptionBottomSheet()

        bottomSheet.isCancelable = false

        bottomSheet.show(parentFragmentManager, "SubscriptionPaywall")
    }

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
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import com.bumptech.glide.Glide
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//import com.infinitybutterfly.infiflyrecipe.databinding.FragmentRecipeDetailBinding
//import com.infinitybutterfly.infiflyrecipe.utils.RetrofitInstanceDetail
//import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsDetails
//import com.infinitybutterfly.infiflyrecipe.models.RecipeResponseDetails
//import com.infinitybutterfly.infiflyrecipe.utils.RetrofitInstanceDetails
//
//class RecipeDetailFragment : Fragment() {
//
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
//        val mealId = arguments?.getString("MEAL_ID")
//
//        if (mealId != null) {
//            fetchRecipeDetails(mealId)
//        } else {
//            Log.e("DETAIL_SCREEN", "Error: No Meal ID was passed to this fragment!")
//        }
//    }
//
//    private fun fetchRecipeDetails(id: String) {
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitInstanceDetails.api.getDetailRecipe(id)
//
//                if (response.isSuccessful) {
//                    val mealsList = response.body()?.meals
//
//                    if (!mealsList.isNullOrEmpty()) {
//                        val specificRecipe = mealsList[0]
//
//                        withContext(Dispatchers.Main) {
//                            populateUI(specificRecipe)
//                        }
//                    }
//                } else {
//                    Log.e("API_ERROR", "Server responded with error code: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("API_CRASH", "Network request failed: ${e.message}")
//            }
//        }
//    }
//
//    // Helper function to keep our code clean!
//    private fun populateUI(recipe: RecipeItemsDetails) {
//        // 1. Set standard text
//        binding.tvRecipeName.text = recipe.strMeal
//        binding.tvCategoryArea.text = "${recipe.strCategory} | ${recipe.strArea}"
//        binding.tvInstructions.text = recipe.strInstructions
//
//        // 2. Load Image using Glide
//        Glide.with(requireContext())
//            .load(recipe.strMealThumb)
//            .into(binding.imgRecipe)
//
//        // 3. Format the messy Ingredients and Measures
//        val ingredientsList = StringBuilder()
//
//        // We append them one by one. We check if they are null or empty first!
//        if (!recipe.strIngredient1.isNullOrBlank()) ingredientsList.append("• ${recipe.strIngredient1} - ${recipe.strMeasure1}\n")
//        if (!recipe.strIngredient2.isNullOrBlank()) ingredientsList.append("• ${recipe.strIngredient2} - ${recipe.strMeasure2}\n")
//        if (!recipe.strIngredient3.isNullOrBlank()) ingredientsList.append("• ${recipe.strIngredient3} - ${recipe.strMeasure3}\n")
//        if (!recipe.strIngredient4.isNullOrBlank()) ingredientsList.append("• ${recipe.strIngredient4} - ${recipe.strMeasure4}\n")
//        if (!recipe.strIngredient5.isNullOrBlank()) ingredientsList.append("• ${recipe.strIngredient5} - ${recipe.strMeasure5}\n")
//        // ... Keep adding the rest of your ingredients up to the amount you put in your Data Class!
//
//        // Finally, set the text to our combined string
//        binding.tvIngredients.text = ingredientsList.toString()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
//
//
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
//// * Use the [RecipeDetailFragment.newInstance] factory method to
//// * create an instance of this fragment.
//// */
////class RecipeDetailFragment : Fragment() {
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
////        return inflater.inflate(R.layout.fragment_recipe_detail, container, false)
////    }
////
////    companion object {
////        /**
////         * Use this factory method to create a new instance of
////         * this fragment using the provided parameters.
////         *
////         * @param param1 Parameter 1.
////         * @param param2 Parameter 2.
////         * @return A new instance of fragment RecipeDetailFragment.
////         */
////        // TODO: Rename and change types and number of parameters
////        @JvmStatic
////        fun newInstance(param1: String, param2: String) =
////            RecipeDetailFragment().apply {
////                arguments = Bundle().apply {
////                    putString(ARG_PARAM1, param1)
////                    putString(ARG_PARAM2, param2)
////                }
////            }
////    }
////}