package com.infinitybutterfly.infiflyrecipe

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.infinitybutterfly.infiflyrecipe.adapters.AdapterAllergy
import com.infinitybutterfly.infiflyrecipe.adapters.AdapterFavFood
import com.infinitybutterfly.infiflyrecipe.models.AllergyView
import com.infinitybutterfly.infiflyrecipe.models.FavFoodView
import com.infinitybutterfly.infiflyrecipe.models.VerifyEmailRequest
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
import com.infinitybutterfly.infiflyrecipe.viewmodels.LoginCreateAccountFragmentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.core.content.edit
import androidx.navigation.NavOptions

class LoginCreateAccountFragment : Fragment(R.layout.fragment_login_create_account) {
    private val viewModel: LoginCreateAccountFragmentViewModel by viewModels()
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var btnOTP: Button
    private lateinit var btnSubmitOTP: Button
    private lateinit var btnContinue: Button
    private lateinit var btnContinue2: Button
    private lateinit var btnGetStarted: Button
    private lateinit var tvErrorMessage: TextView
    private lateinit var emailEditText: android.widget.EditText
    private var finalFavFoods: String = ""
    private var finalAllergies: String = ""

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the strings you've already built
        outState.putString("temp_fav_foods", finalFavFoods)
        outState.putString("temp_allergies", finalAllergies)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            finalFavFoods = savedInstanceState.getString("temp_fav_foods", "")
            finalAllergies = savedInstanceState.getString("temp_allergies", "")
        }

        // 1. Initialize Views
        initViews(view)

        // 2. Restore ViewFlipper Step from ViewModel
        viewFlipper.displayedChild = viewModel.currentStep

        // 3. Setup Logic & Observers
        setupObservers()
        setupOtpEntryLogic(view)

        // --- ADAPTER SETUP ---
        val adapterAllergy = setupAllergyRecycler(view)
        val adapterFavFood = setupFavFoodRecycler(view)

        // 4. EMAIL VALIDATION (This enables the Send OTP button)
        emailEditText.doOnTextChanged { text, _, _, _ ->
            val emailInput = text.toString().trim()
            btnOTP.isEnabled = emailInput.isNotEmpty() &&
                    Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()
        }

        // 5. CLICK LISTENERS

        // STEP 0: Send Email OTP
        btnOTP.setOnClickListener {
            val emailInput = emailEditText.text.toString().trim()
            tvErrorMessage.visibility = View.GONE
            viewModel.sendOtp(emailInput)
        }

        // STEP 1: Verify OTP
        btnSubmitOTP.setOnClickListener {
            verifyOtpAndNavigate(view)
        }

        // STEP 2: Fav Foods Continue
        btnContinue.setOnClickListener {
            val selected = adapterFavFood.getSelectedFavFoods()
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.selectedFavFoodsList = selected.map { it.nameText }.toMutableSet()
                finalFavFoods = viewModel.selectedFavFoodsList.joinToString(", ")
//                finalFavFoods = selected.joinToString(", ") { it.nameText }
                showNextStep()
            }
        }

        // STEP 3: Allergy Continue
        btnContinue2.setOnClickListener {
            val selected = adapterAllergy.getSelectedAllergyFoods()
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.selectedAllergiesList = selected.map { it.nameText }.toMutableSet()
                finalAllergies = viewModel.selectedAllergiesList.joinToString(", ")
//                finalAllergies = selected.joinToString(", ") { it.nameText }
                showNextStep()
            }
        }

        // STEP 4: Get Started Button
        btnGetStarted.setOnClickListener {
            // 1. Get the JWT token we saved during the OTP step
            val sharedPref = requireActivity().getSharedPreferences(
                "MyAppPrefs",
                android.content.Context.MODE_PRIVATE
            )
            val jwtToken = sharedPref.getString("JWT_TOKEN", "") ?: ""

            if (jwtToken.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Authentication error. Please log in again.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // 2. Convert our Strings into OkHttp RequestBodies so Retrofit can send them as Multipart data
            val textType = "text/plain".toMediaTypeOrNull()
//                        val jwtBody = jwtToken.toRequestBody(textType)
            val favFoodsBody = finalFavFoods.toRequestBody(textType)
            val allergiesBody = finalAllergies.toRequestBody(textType)
            val tvErrorMessage2 = view.findViewById<TextView>(R.id.tvErrorMessage2)

            // We set this to "false" because they just finished the onboarding flow! but haven't completed the profile
            val isCompleteBody = "false".toRequestBody(textType)
            val isLocked = "false".toRequestBody(textType)

            // 3. Make the API Call!
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Notice we pass 'null' for things like bio and dob because we haven't asked for them yet!
                    val response = RetrofitClient.ktorApi.updateProfile(
                        token = "Bearer $jwtToken",
                        name = null,
                        country = null,
                        username = null,
                        dob = null,
                        bio = null,
                        favFoods = favFoodsBody,
                        allergies = allergiesBody,
                        isCompleteCache = isCompleteBody,
                        image = null,
                        email = null,
                        isLocked = isLocked
                    )

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.success == true) {

                            // UPDATE CACHE: The profile is now fully complete!
                            sharedPref.edit { putBoolean("isProfileComplete", false)
                                putBoolean("isLocked", false)
                                apply()}

                            // Navigate to the Home Screen!
                            val navOptions = NavOptions.Builder()
                                .setEnterAnim(R.anim.slide_in_right)
                                .setExitAnim(R.anim.slide_out_left)
                                .setPopEnterAnim(R.anim.slide_in_left)
                                .setPopExitAnim(R.anim.slide_out_right)
                                .build()

                            findNavController().navigate(
                                R.id.action_login_to_main_app,
                                null,
                                navOptions
                            )

                            Toast.makeText(
                                requireContext(),
                                "Welcome! Login Successful!",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Failed to save profile: ${response.body()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
//                                    Toast.makeText(requireContext(), "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                        tvErrorMessage2.text = "Network Error: ${e.message}"
                        tvErrorMessage2.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun initViews(view: View) {
        viewFlipper = view.findViewById(R.id.viewFlipper)
        btnOTP = view.findViewById(R.id.send_otp_button)
        btnSubmitOTP = view.findViewById(R.id.submit_otp_button)
        btnContinue = view.findViewById(R.id.btnContinue)
        btnContinue2 = view.findViewById(R.id.btnContinue2)
        btnGetStarted = view.findViewById(R.id.btnGetStarted)
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
        emailEditText = view.findViewById(R.id.email_id)

        btnOTP.isEnabled = false
        btnSubmitOTP.isEnabled = false
    }

    private fun setupObservers() {
        viewModel.otpSentSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true && viewFlipper.displayedChild == 0) {
                Toast.makeText(requireContext(), "OTP Sent!", Toast.LENGTH_SHORT).show()
                showNextStep()
                viewModel.consumeOtpEvent()
            } else if (success == false) {
                Toast.makeText(requireContext(), "Failed to send OTP", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                tvErrorMessage.text = it
                tvErrorMessage.visibility = View.VISIBLE
                viewModel.errorMessage.value = null
            }
        }
    }

//    private fun verifyOtpAndNavigate(view: View) {
//        val otp = getOtpFromBoxes(view)
//        if (otp.length < 6) return
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val request = VerifyEmailRequest(email = viewModel.userEmail, otp = otp)
//                val response = RetrofitClient.myBackendApi.verifyOtp(request)
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful && response.body()?.success == true) {
//                        val serverFav = response.body()?.profileData?.favFoods
//                        val serverAllergy = response.body()?.profileData?.allergies
//
//                        if (!serverFav.isNullOrBlank() && !serverAllergy.isNullOrBlank()) {
//                            findNavController().navigate(R.id.action_login_to_main_app)
//                        } else {
//                            showNextStep()
//                        }
//                    } else {
//                        Toast.makeText(requireContext(), "Invalid OTP!", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    tvErrorMessage.text = "Error: ${e.message}"
//                    tvErrorMessage.visibility = View.VISIBLE
//                }
//            }
//        }
//    }

    private fun verifyOtpAndNavigate(view: View) {
        val otp = getOtpFromBoxes(view)
        if (otp.length < 6) return

        // 2. Call the server to verify the OTP
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = VerifyEmailRequest(email = viewModel.userEmail, otp = otp)
                val response = RetrofitClient.myBackendApi.verifyOtp(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {

                        // 3. CACHE THE DATA!
                        val jwtToken = response.body()?.token
                        val isProfileCompleteCache = response.body()?.isProfileComplete
                        val isLocked = response.body()?.isLocked
                        val serverFavFoods = response.body()?.profileData?.favFoods
                        val serverAllergies = response.body()?.profileData?.allergies
//                                val isComplete = response.body()?.isProfileComplete ?: false

                        Log.d(
                            "AUTH_DEBUG",
                            "Saving this token to SharedPreferences: $jwtToken"
                        )

                        val sharedPref = requireActivity().getSharedPreferences(
                            "MyAppPrefs",
                            android.content.Context.MODE_PRIVATE
                        )
                        sharedPref.edit().apply {
                            putString("JWT_TOKEN", jwtToken)
                            putBoolean("isProfileComplete", isProfileCompleteCache ?: false)
                            putBoolean("isLocked", isLocked ?: false)
                            apply() // Save asynchronously
                            Log.d("Login","saved sharedpref successfully")
                        }

                        Toast.makeText(
                            requireContext(),
                            "Correct OTP!",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.d(
                            "LOGIN_CHECK",
                            "TOKEN IS: '$jwtToken' | FavFoods from server: '$serverFavFoods' | Allergies from server: '$serverAllergies'"
                        )
                        // CHECK THE SERVER DATA TO DECIDE WHERE TO GO!
//                                if (isComplete) {
                        if (!serverFavFoods.isNullOrBlank() && !serverAllergies.isNullOrBlank()) {
                            // RETURNING USER: Their profile is already done.
                            // Skip the form and go straight to the Main App (Home)
                            val navOptions = NavOptions.Builder()
                                .setEnterAnim(R.anim.slide_in_right)
                                .setExitAnim(R.anim.slide_out_left)
                                .setPopEnterAnim(R.anim.slide_in_left)
                                .setPopExitAnim(R.anim.slide_out_right)
                                .build()

                            findNavController().navigate(
                                R.id.action_login_to_main_app,
                                null,
                                navOptions
                            )
                            Toast.makeText(
                                requireContext(),
                                "Login Successful!",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
//                            // NEW USER: Their profile is NOT complete.
//                            // Move the ViewFlipper to the next screen to ask for Fav Foods
//                            viewFlipper.setInAnimation(
//                                requireContext(),
//                                android.R.anim.slide_in_left
//                            )
//                            viewFlipper.setOutAnimation(
//                                requireContext(),
//                                android.R.anim.slide_out_right
//                            )
//                            viewFlipper.showNext()
                            showNextStep()
                        }
//                                // Flip the screen
//                                viewFlipper.setInAnimation(requireContext(), android.R.anim.slide_in_left)
//                                viewFlipper.setOutAnimation(requireContext(), android.R.anim.slide_out_right)
//                                viewFlipper.showNext()

                    } else {
                        Toast.makeText(requireContext(), "Invalid OTP!", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Network Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    (requireActivity() as MainActivity).stopRefreshAnimation()
                }
            }
        }
    }
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val request = VerifyEmailRequest(email = viewModel.userEmail, otp = otp)
//                val response = RetrofitClient.myBackendApi.verifyOtp(request)
//
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful && response.body()?.success == true) {
//                        val jwtToken = response.body()?.token
//
//                        // SAVE TO SHARED PREFERENCES
//                        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
//                        sharedPref.edit {
//                            putString("JWT_TOKEN", jwtToken)
//                            apply()
//                        }
//
//                        // Check profile and move forward
//                        val serverFav = response.body()?.profileData?.favFoods
//                        val serverAllergy = response.body()?.profileData?.allergies
//
//                        if (!serverFav.isNullOrBlank() && !serverAllergy.isNullOrBlank()) {
//                            findNavController().navigate(R.id.action_login_to_main_app)
//                        } else {
//                            showNextStep()
//                        }
//                    } else {
//                        Toast.makeText(requireContext(), "Invalid OTP!", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    tvErrorMessage.text = "Error: ${e.message}"
//                    tvErrorMessage.visibility = View.VISIBLE
//                }
//            }
//        }

    private fun showNextStep() {
        viewFlipper.setInAnimation(requireContext(), android.R.anim.slide_in_left)
        viewFlipper.setOutAnimation(requireContext(), android.R.anim.slide_out_right)
        viewFlipper.showNext()
        viewModel.currentStep = viewFlipper.displayedChild
    }

    private fun setupOtpEntryLogic(view: View) {
        val boxes = listOf<com.google.android.material.textfield.TextInputEditText>(
            view.findViewById(R.id.etOtp1), view.findViewById(R.id.etOtp2),
            view.findViewById(R.id.etOtp3), view.findViewById(R.id.etOtp4),
            view.findViewById(R.id.etOtp5), view.findViewById(R.id.etOtp6)
        )

        boxes.forEachIndexed { index, et ->
            et.doOnTextChanged { text, _, _, _ ->
                if (text?.length == 1 && index < 5) boxes[index + 1].requestFocus()
                btnSubmitOTP.isEnabled = boxes.all { it.text?.length == 1 }
            }
            et.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
                    if (et.text.isNullOrEmpty() && index > 0) {
                        boxes[index - 1].requestFocus()
                        boxes[index - 1].text?.clear()
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    private fun getOtpFromBoxes(view: View): String {
        return listOf(R.id.etOtp1, R.id.etOtp2, R.id.etOtp3, R.id.etOtp4, R.id.etOtp5, R.id.etOtp6)
            .map { view.findViewById<TextView>(it).text.toString() }
            .joinToString("")
    }

    private fun setupAllergyRecycler(view: View): AdapterAllergy {
        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewAllergy)


        viewModel.listAllergy.forEach { item ->
            if (viewModel.selectedAllergiesList.contains(item.nameText)) {
                item.isSelected = true
                Log.d("DEBUG_RECOVERY", "Restored selection for Allergy for: ${item.nameText}")
            }
        }

        val adapter = AdapterAllergy(viewModel.listAllergy)
        rv.adapter = adapter
        return adapter
    }

    private fun setupFavFoodRecycler(view: View): AdapterFavFood {
        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewFavFoods)

        viewModel.listFavFood.forEach { item ->
            if (viewModel.selectedFavFoodsList.contains(item.nameText)) {
                item.isSelected = true
                Log.d("DEBUG_RECOVERY", "Restored selection for FavFood for: ${item.nameText}")
            }
        }

        val adapter = AdapterFavFood(viewModel.listFavFood)
        rv.adapter = adapter
        return adapter
    }
}


//package com.infinitybutterfly.infiflyrecipe
//
//import android.os.Bundle
//import android.util.Log
//import android.util.Patterns
//import android.view.View
//import android.widget.Button
//import android.widget.TextView
//import android.widget.Toast
//import android.widget.ViewFlipper
//import androidx.core.widget.doOnTextChanged
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.NavOptions
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.RecyclerView
//import com.infinitybutterfly.infiflyrecipe.adapters.AdapterAllergy
//import com.infinitybutterfly.infiflyrecipe.adapters.AdapterFavFood
//import com.infinitybutterfly.infiflyrecipe.models.AllergyView
//import com.infinitybutterfly.infiflyrecipe.models.FavFoodView
//import com.infinitybutterfly.infiflyrecipe.models.VerifyEmailRequest
//import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
//import com.infinitybutterfly.infiflyrecipe.viewmodels.LoginCreateAccountFragmentViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class LoginCreateAccountFragment : Fragment(R.layout.fragment_login_create_account) {
//
//    private val viewModel: LoginCreateAccountFragmentViewModel by viewModels()
//
//    private lateinit var viewFlipper: ViewFlipper
//    private lateinit var btnOTP: Button
//    private lateinit var btnSubmitOTP: Button
//    private lateinit var btnContinue: Button
//    private lateinit var btnContinue2: Button
//    private lateinit var btnGetStarted: Button
//    private lateinit var tvErrorMessage: TextView
//    private var finalFavFoods: String = ""
//    private var finalAllergies: String = ""
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // 1. Initialize Views
//        initViews(view)
//
//        // 2. RESTORE POSITION: Set flipper to the step saved in ViewModel
//        viewFlipper.displayedChild = viewModel.currentStep
//
//        // 3. Setup Observers
//        setupObservers()
//
//        // 4. Setup OTP Box Logic (Auto-advance and Backspace)
//        setupOtpEntryLogic(view)
//
//        // 5. Button Click Listeners
//
//        // STEP 0: Send Email OTP
//        btnOTP.setOnClickListener {
//            val emailEditText = view.findViewById<android.widget.EditText>(R.id.email_id)
////            val emailInput = emailEditText.text.toString().trim()
//
//            emailEditText.doOnTextChanged { text, _, _, _ ->
//                val emailInput = text.toString().trim()
//
//                btnOTP.isEnabled = emailInput.isNotEmpty() &&
//                        Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()
////            if (emailInput.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
////                tvErrorMessage.visibility = View.GONE
////                viewModel.sendOtp(emailInput)
//            }
//        }
//
//        // STEP 1: Verify OTP
//        btnSubmitOTP.setOnClickListener {
//            verifyOtpAndNavigate(view)
//        }
//
//
////        Allergy Selection
//
//            val recyclerViewallergy = view.findViewById<RecyclerView>(R.id.recyclerViewAllergy)
//
//            val allergyView = ArrayList<AllergyView>()
//
//            allergyView.add(
//                AllergyView(
//                    R.drawable.milk,
//                    "Milk"
//                )
//            )
//            allergyView.add(
//                AllergyView(
//                    R.drawable.egg,
//                    "Eggs"
//                )
//            )
//            allergyView.add(
//                AllergyView(
//                    R.drawable.peanut,
//                    "Peanuts"
//                )
//            )
//            allergyView.add(
//                AllergyView(
//                    R.drawable.tree_nuts,
//                    "Tree Nuts"
//                )
//            )
//            allergyView.add(
//                AllergyView(
//                    R.drawable.fish,
//                    "Fish"
//                )
//            )
//            allergyView.add(
//                AllergyView(
//                    R.drawable.sesame,
//                    "Sesame"
//                )
//            )
//            allergyView.add(
//                AllergyView(
//                    R.drawable.wheat,
//                    "Wheat"
//                )
//            )
//            allergyView.add(
//                AllergyView(
//                    R.drawable.soybean,
//                    "Soybeans"
//                )
//            )
//            allergyView.add(
//                AllergyView(
//                    R.drawable.block,
//                    "No Allergy"
//                )
//            )
//
////        recyclerView.layoutManager = LinearLayoutManager(requireContext())
//
//            val adapterallergy = AdapterAllergy(allergyView)
//            recyclerViewallergy.adapter = adapterallergy
//
//
////        Fav Food Selection
//
//            val recyclerViewfavfood = view.findViewById<RecyclerView>(R.id.recyclerViewFavFoods)
//
//            val favFoodView = ArrayList<FavFoodView>()
//
//            favFoodView.add(
//                FavFoodView(
//                    R.drawable.vegetable,
//                    "Vegetables"
//                )
//            )
//            favFoodView.add(
//                FavFoodView(
//                    R.drawable.fruits,
//                    "Fruits"
//                )
//            )
//            favFoodView.add(
//                FavFoodView(
//                    R.drawable.dairy_products,
//                    "Dairy Products"
//                )
//            )
//            favFoodView.add(
//                FavFoodView(
//                    R.drawable.chicken,
//                    "Chicken"
//                )
//            )
//            favFoodView.add(
//                FavFoodView(
//                    R.drawable.wheat,
//                    "Grains/Staples"
//                )
//            )
//            favFoodView.add(
//                FavFoodView(
//                    R.drawable.fish,
//                    "Seafood"
//                )
//            )
//            favFoodView.add(
//                FavFoodView(
//                    R.drawable.dessertssweets,
//                    "Desserts/Sweets"
//                )
//            )
//            favFoodView.add(
//                FavFoodView(
//                    R.drawable.soybean,
//                    "Legumes/Beans"
//                )
//            )
//            favFoodView.add(
//                FavFoodView(
//                    R.drawable.tree_nuts,
//                    "Nuts & Seeds"
//                )
//            )
//
//        val adapterfavfood = AdapterFavFood(favFoodView)
//            recyclerViewfavfood.adapter = adapterfavfood
//
//        // STEP 2: Fav Foods
//        btnContinue.setOnClickListener {
//            val selectedItemsFavFood = adapterfavfood.getSelectedFavFoods()
//
//                if (selectedItemsFavFood.isEmpty()) {
//                    Toast.makeText(
//                        requireContext(),
//                        "Please select at least one item",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                } else {
////                for (food in selectedItemsFavFood) {
////                    println("User selected Fav Food Items: ${food.nameText}")
////                }
//                    finalFavFoods =
//                        selectedItemsFavFood.joinToString(separator = ", ") { it.nameText }
//                    println("User selected Fav Food Items: $finalFavFoods")
//                    showNextStep()
////                    viewFlipper.showNext()
//                }
//        }
//
//        // STEP 3: Allergies
//        btnContinue2.setOnClickListener {
//            val selectedItemsAllergy = adapterallergy.getSelectedAllergyFoods()
//
//                if (selectedItemsAllergy.isEmpty()) {
//                    Toast.makeText(
//                        requireContext(),
//                        "Please select at least one item",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                } else {
////                for (food in selectedItemsAllergy) {
////                    println("User selected Allergy Items: ${food.nameText}")
////                }
//                    finalAllergies =
//                        selectedItemsAllergy.joinToString(separator = ", ") { it.nameText }
//                    println("User selected Allergy Items: $finalAllergies")
//                    showNextStep()
////                    viewFlipper.showNext()
//                }
//            }
//
//    }
//
//    private fun initViews(view: View) {
//        viewFlipper = view.findViewById(R.id.viewFlipper)
//        btnOTP = view.findViewById(R.id.send_otp_button)
//        btnSubmitOTP = view.findViewById(R.id.submit_otp_button)
//        btnContinue = view.findViewById(R.id.btnContinue)
//        btnContinue2 = view.findViewById(R.id.btnContinue2)
//        btnGetStarted = view.findViewById(R.id.btnGetStarted)
//        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
//
//        // Initial button states
//        btnSubmitOTP.isEnabled = false
//    }
//
//    private fun setupObservers() {
//        viewModel.otpSentSuccess.observe(viewLifecycleOwner) { success ->
//            // Only trigger navigation if we are currently on the Email screen (index 0)
//            if (success == true && viewFlipper.displayedChild == 0) {
//                Toast.makeText(requireContext(), "OTP Sent!", Toast.LENGTH_SHORT).show()
//                showNextStep()
//                viewModel.consumeOtpEvent() // Prevent re-trigger on rotation
//            } else if (success == false) {
//                Toast.makeText(requireContext(), "Failed to send OTP", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
//            error?.let {
//                tvErrorMessage.text = it
//                tvErrorMessage.visibility = View.VISIBLE
//                viewModel.errorMessage.value = null
//            }
//        }
//    }
//
//    private fun verifyOtpAndNavigate(view: View) {
//        val otp1 = view.findViewById<TextView>(R.id.etOtp1).text.toString()
//        val otp2 = view.findViewById<TextView>(R.id.etOtp2).text.toString()
//        val otp3 = view.findViewById<TextView>(R.id.etOtp3).text.toString()
//        val otp4 = view.findViewById<TextView>(R.id.etOtp4).text.toString()
//        val otp5 = view.findViewById<TextView>(R.id.etOtp5).text.toString()
//        val otp6 = view.findViewById<TextView>(R.id.etOtp6).text.toString()
//        val fullOtp = "$otp1$otp2$otp3$otp4$otp5$otp6"
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val request = VerifyEmailRequest(email = viewModel.userEmail, otp = fullOtp)
//                val response = RetrofitClient.myBackendApi.verifyOtp(request)
//
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful && response.body()?.success == true) {
//                        val serverFavFoods = response.body()?.profileData?.favFoods
//                        val serverAllergies = response.body()?.profileData?.allergies
//
//                        if (!serverFavFoods.isNullOrBlank() && !serverAllergies.isNullOrBlank()) {
//                            findNavController().navigate(R.id.action_login_to_main_app)
//                        } else {
//                            showNextStep()
//                        }
//                    } else {
//                        Toast.makeText(requireContext(), "Invalid OTP!", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    tvErrorMessage.text = "Network Error: ${e.message}"
//                    tvErrorMessage.visibility = View.VISIBLE
//                }
//            }
//        }
//    }
//
//    private fun showNextStep() {
//        viewFlipper.setInAnimation(requireContext(), android.R.anim.slide_in_left)
//        viewFlipper.setOutAnimation(requireContext(), android.R.anim.slide_out_right)
//        viewFlipper.showNext()
//
//        viewModel.currentStep = viewFlipper.displayedChild
//    }
//
//    private fun setupOtpEntryLogic(view: View) {
//        val etOtp1 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp1)
//        val etOtp2 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp2)
//        val etOtp3 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp3)
//        val etOtp4 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp4)
//        val etOtp5 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp5)
//        val etOtp6 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp6)
//
//        val otpBoxes = listOf(etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6)
//
//        otpBoxes.forEachIndexed { index, editText ->
//            editText.doOnTextChanged { text, _, _, _ ->
//                if (text?.length == 1 && index < otpBoxes.size - 1) {
//                    otpBoxes[index + 1].requestFocus()
//                }
//                updateOtpButtonState(otpBoxes)
//            }
//
//            editText.setOnKeyListener { _, keyCode, event ->
//                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
//                    if (editText.text.isNullOrEmpty() && index > 0) {
//                        otpBoxes[index - 1].requestFocus()
//                        otpBoxes[index - 1].text?.clear()
//                        updateOtpButtonState(otpBoxes)
//                        return@setOnKeyListener true
//                    }
//                }
//                false
//            }
//        }
//    }
//
//    private fun updateOtpButtonState(otpBoxes: List<com.google.android.material.textfield.TextInputEditText>) {
//        btnSubmitOTP.isEnabled = otpBoxes.all { it.text?.length == 1 }
//    }
//}
//
//
//
////package com.infinitybutterfly.infiflyrecipe
////
////import android.annotation.SuppressLint
////import android.os.Bundle
////import android.util.Log
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////import android.widget.Button
////import android.widget.Toast
////import android.widget.ViewFlipper
////import androidx.fragment.app.Fragment
////import androidx.navigation.NavOptions
////import androidx.navigation.fragment.findNavController
////import androidx.recyclerview.widget.RecyclerView
////import com.infinitybutterfly.infiflyrecipe.adapters.AdapterAllergy
////import com.infinitybutterfly.infiflyrecipe.adapters.AdapterFavFood
////import com.infinitybutterfly.infiflyrecipe.models.FavFoodView
////import androidx.lifecycle.lifecycleScope
////import com.infinitybutterfly.infiflyrecipe.models.EmailOtpRequest
////import kotlinx.coroutines.Dispatchers
////import kotlinx.coroutines.launch
////import kotlinx.coroutines.withContext
////import androidx.core.widget.doOnTextChanged
////import com.infinitybutterfly.infiflyrecipe.models.VerifyEmailRequest
////import android.util.Patterns
////import android.widget.TextView
////import com.infinitybutterfly.infiflyrecipe.models.AllergyView
////import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
////import kotlin.collections.joinToString
////import okhttp3.MediaType.Companion.toMediaTypeOrNull
////import okhttp3.RequestBody.Companion.toRequestBody
////import androidx.core.content.edit
////import androidx.fragment.app.viewModels
////import com.infinitybutterfly.infiflyrecipe.viewmodels.LoginCreateAccountFragmentViewModel
////
////
////class LoginCreateAccountFragment : Fragment(R.layout.fragment_login_create_account) {
////
////    private val viewModel: LoginCreateAccountFragmentViewModel by viewModels()
////    private lateinit var viewFlipper: ViewFlipper
////    private lateinit var btnOTP: Button
////    private lateinit var btnSubmitOTP: Button
////    private lateinit var btnContinue: Button
////    private lateinit var btnContinue2: Button
////    private lateinit var btnGetStarted: Button
////    private var finalFavFoods: String = ""
////    private var finalAllergies: String = ""
//////    private var userEmail: String = ""
////    private lateinit var tvErrorMessage: TextView
////
////    override fun onCreateView(
////        inflater: LayoutInflater, container: ViewGroup?,
////        savedInstanceState: Bundle?
////    ): View? {
////        return inflater.inflate(R.layout.fragment_login_create_account, container, false)
////    }
////
////    override fun onResume() {
////        super.onResume()
////        // Turn OFF swipe to refresh while filling out the form
////        (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
////    }
////
////    override fun onPause() {
////        super.onPause()
////        // Turn it back ON when they leave this screen
////        (requireActivity() as MainActivity).setSwipeRefreshEnabled(true)
////    }
////
//////    override fun onSaveInstanceState(outState: Bundle) {
//////        super.onSaveInstanceState(outState)
//////        outState.putInt("current_step", viewFlipper.displayedChild)
//////        outState.putString("saved_email", userEmail)
//////        outState.putString("saved_fav_foods", finalFavFoods)
//////        outState.putString("saved_allergies", finalAllergies)
//////    }
////
////    @SuppressLint("ResourceType")
////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////        super.onViewCreated(view, savedInstanceState)
////
////        viewFlipper = view.findViewById(R.id.viewFlipper)
////        btnOTP = view.findViewById(R.id.send_otp_button)
////        btnSubmitOTP = view.findViewById(R.id.submit_otp_button)
////        btnContinue = view.findViewById(R.id.btnContinue)
////        btnContinue2 = view.findViewById(R.id.btnContinue2)
////        btnGetStarted = view.findViewById(R.id.btnGetStarted)
////        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
////
////        viewFlipper.displayedChild = viewModel.currentStep
////
////        setupObservers()
////
//////        if (savedInstanceState != null) {
//////            val lastStep = savedInstanceState.getInt("current_step", 0)
//////            userEmail = savedInstanceState.getString("saved_email", "")
//////            finalFavFoods = savedInstanceState.getString("saved_fav_foods", "")
//////            finalAllergies = savedInstanceState.getString("saved_allergies", "")
//////
//////            // Move the flipper back to where the user was
//////            viewFlipper.displayedChild = lastStep
//////        }
////
////        val emailEditText = view.findViewById<android.widget.EditText>(R.id.email_id)
////
////        emailEditText.doOnTextChanged { text, _, _, _ ->
////            val email = text.toString().trim()
////            btnOTP.isEnabled =
////                email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
////        }
////
//////        // Switch to the next view
//////        btnOTP.setOnClickListener {
//////            viewFlipper.showNext()
//////        }
//////        // Switch to the next view
//////        btnSubmitOTP.setOnClickListener {
//////            viewFlipper.showNext()
//////        }
//////        // Switch to the next view
//////        btnContinue.setOnClickListener {
//////            viewFlipper.showNext()
//////        }
////        // Switch to the next view
//////        btnContinue2.setOnClickListener {
//////            viewFlipper.showNext()
//////        }
////
//////        btnOTP.setOnClickListener {
//////            viewFlipper.setInAnimation(this.context, android.R.anim.slide_in_right)
//////            viewFlipper.setOutAnimation(this.context, android.R.anim.slide_out_left)
//////
//////            viewFlipper.showNext()
//////        }
////
////        setupOtpLogic(view)
//////
//////        btnOTP.setOnClickListener {
//////            // 1. Get the email from the layout
//////            // TODO: Ensure this matches the ID of your EditText in enter_number_layout2
////////            val emailInput = view.findViewById<android.widget.EditText>(R.id.email_id)?.text.toString().trim()
//////
//////            val tvErrorMessage = view.findViewById<TextView>(R.id.tvErrorMessage)
//////            val emailEditText =
//////                view.findViewById<android.widget.EditText>(R.id.email_id) // <-- Check this ID!
//////            val emailInput = emailEditText?.text?.toString()?.trim() ?: ""
//////
//////
//////            if (emailInput.isBlank()) {
//////                Toast.makeText(requireContext(), "Please enter an email", Toast.LENGTH_SHORT).show()
//////                return@setOnClickListener
//////            }
//////
//////            if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
//////                Toast.makeText(
//////                    requireContext(),
//////                    "Please enter a valid email address",
//////                    Toast.LENGTH_SHORT
//////                ).show()
//////                return@setOnClickListener
//////            }
//////
//////            userEmail = emailInput // Save it globally for the next step
//////
//////            // 2. Call the API to send the OTP
//////            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//////                try {
//////                    val request = EmailOtpRequest(email = userEmail)
//////                    val response = RetrofitClient.myBackendApi.requestOtp(request)
//////
//////                    withContext(Dispatchers.Main) {
//////                        if (response.isSuccessful && response.body()?.success == true) {
//////                            Toast.makeText(requireContext(), "OTP Sent!", Toast.LENGTH_LONG).show()
//////
//////                            // Flip the screen ONLY if the email actually sent!
//////                            viewFlipper.setInAnimation(
//////                                requireContext(),
//////                                android.R.anim.slide_in_left
//////                            )
//////                            viewFlipper.setOutAnimation(
//////                                requireContext(),
//////                                android.R.anim.slide_out_right
//////                            )
//////                            viewFlipper.showNext()
//////                        } else {
//////                            Toast.makeText(
//////                                requireContext(),
//////                                "Failed to send OTP",
//////                                Toast.LENGTH_LONG
//////                            ).show()
//////                        }
//////                    }
//////                } catch (e: Exception) {
//////                    withContext(Dispatchers.Main) {
////////                        Toast.makeText(requireContext(), "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
////////                     Display the Network Error/Timeout in the TextView
//////                        tvErrorMessage.text = "Network Error: ${e.message}"
//////                        tvErrorMessage.visibility = View.VISIBLE
//////                    }
//////                }
//////            }
//////        }
////
////        btnOTP.setOnClickListener {
////            val emailInput = emailEditText.text.toString().trim()
////            if (emailInput.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
////                tvErrorMessage.visibility = View.GONE
////                viewModel.sendOtp(emailInput) // The ViewModel now handles the API call
////            }
////        }
////
////        // 1. Find the boxes
////        val etOtp1 =
////            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp1)
////        val etOtp2 =
////            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp2)
////        val etOtp3 =
////            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp3)
////        val etOtp4 =
////            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp4)
////        val etOtp5 =
////            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp5)
////        val etOtp6 =
////            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp6)
////
////        // 2. Add the Auto-Advance Logic!
////        etOtp1.doOnTextChanged { text, _, _, _ -> if (text?.length == 1) etOtp2.requestFocus() }
////        etOtp2.doOnTextChanged { text, _, _, _ -> if (text?.length == 1) etOtp3.requestFocus() }
////        etOtp3.doOnTextChanged { text, _, _, _ -> if (text?.length == 1) etOtp4.requestFocus() }
////        etOtp4.doOnTextChanged { text, _, _, _ -> if (text?.length == 1) etOtp5.requestFocus() }
////        etOtp5.doOnTextChanged { text, _, _, _ -> if (text?.length == 1) etOtp6.requestFocus() }
////
////        // Create a list for easier handling
////        val otpBoxes = listOf(etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6)
////
////        otpBoxes.forEachIndexed { index, editText ->
////            editText.setOnKeyListener { _, keyCode, event ->
////                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
////                    if (editText.text.isNullOrEmpty() && index > 0) {
////                        otpBoxes[index - 1].requestFocus()
////                        otpBoxes[index - 1].text?.clear()
////                        updateOtpButtonState(otpBoxes)
////                        return@setOnKeyListener true
////                    }
////                }
////                false
////            }
////            editText.doOnTextChanged { _, _, _, _ ->
////                updateOtpButtonState(otpBoxes)
////            }
////
//////
//////        btnSubmitOTP.setOnClickListener {
//////            // Because we found the boxes above, we can just grab their text directly!
//////            val fullOtp = "${etOtp1.text}${etOtp2.text}${etOtp3.text}${etOtp4.text}${etOtp5.text}${etOtp6.text}"
//////
//////            if (fullOtp.length < 6) {
//////                Toast.makeText(requireContext(), "Please enter the full 6-digit code", Toast.LENGTH_SHORT).show()
//////                return@setOnClickListener
//////            }
////            btnSubmitOTP.setOnClickListener {
////                // 1. Grab all 6 digits and combine them into one string
////                val otp1 =
////                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp1).text.toString()
////                val otp2 =
////                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp2).text.toString()
////                val otp3 =
////                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp3).text.toString()
////                val otp4 =
////                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp4).text.toString()
////                val otp5 =
////                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp5).text.toString()
////                val otp6 =
////                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp6).text.toString()
////
////                val fullOtp = "$otp1$otp2$otp3$otp4$otp5$otp6"
////
////                if (fullOtp.length < 6) {
////                    Toast.makeText(
////                        requireContext(),
////                        "Please enter the full 6-digit code",
////                        Toast.LENGTH_SHORT
////                    ).show()
////                    return@setOnClickListener
////                }
////                val emailToVerify = viewModel.userEmail
////
////                // 2. Call the server to verify the OTP
////                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////                    try {
////                        val request = VerifyEmailRequest(email = emailToVerify, otp = fullOtp)
////                        val response = RetrofitClient.myBackendApi.verifyOtp(request)
////
////                        withContext(Dispatchers.Main) {
////                            if (response.isSuccessful && response.body()?.success == true) {
////
////                                // 3. CACHE THE DATA!
////                                val jwtToken = response.body()?.token
////                                val serverFavFoods = response.body()?.profileData?.favFoods
////                                val serverAllergies = response.body()?.profileData?.allergies
//////                                val isComplete = response.body()?.isProfileComplete ?: false
////
////                                Log.d(
////                                    "AUTH_DEBUG",
////                                    "Saving this token to SharedPreferences: $jwtToken"
////                                )
////
////                                val sharedPref = requireActivity().getSharedPreferences(
////                                    "MyAppPrefs",
////                                    android.content.Context.MODE_PRIVATE
////                                )
////                                sharedPref.edit().apply {
////                                    putString("JWT_TOKEN", jwtToken)
////                                    val isComplete =
////                                        !serverFavFoods.isNullOrBlank() && !serverAllergies.isNullOrBlank()
////                                    putBoolean("isProfileComplete", isComplete)
////                                    apply() // Save asynchronously
////                                }
////
////                                Toast.makeText(
////                                    requireContext(),
////                                    "Login Successful!",
////                                    Toast.LENGTH_SHORT
////                                ).show()
////
////                                Log.d(
////                                    "LOGIN_CHECK",
////                                    "TOKEN IS: '$jwtToken' | FavFoods from server: '$serverFavFoods' | Allergies from server: '$serverAllergies'"
////                                )
////                                // CHECK THE SERVER DATA TO DECIDE WHERE TO GO!
//////                                if (isComplete) {
////                                if (!serverFavFoods.isNullOrBlank() && !serverAllergies.isNullOrBlank()) {
////                                    // RETURNING USER: Their profile is already done.
////                                    // Skip the form and go straight to the Main App (Home)
////                                    val navOptions = NavOptions.Builder()
////                                        .setEnterAnim(R.anim.slide_in_right)
////                                        .setExitAnim(R.anim.slide_out_left)
////                                        .setPopEnterAnim(R.anim.slide_in_left)
////                                        .setPopExitAnim(R.anim.slide_out_right)
////                                        .build()
////
////                                    findNavController().navigate(
////                                        R.id.action_login_to_main_app,
////                                        null,
////                                        navOptions
////                                    )
////
////                                } else {
////                                    // NEW USER: Their profile is NOT complete.
////                                    // Move the ViewFlipper to the next screen to ask for Fav Foods
////                                    viewFlipper.setInAnimation(
////                                        requireContext(),
////                                        android.R.anim.slide_in_left
////                                    )
////                                    viewFlipper.setOutAnimation(
////                                        requireContext(),
////                                        android.R.anim.slide_out_right
////                                    )
////                                    viewFlipper.showNext()
////                                }
//////                                // Flip the screen
//////                                viewFlipper.setInAnimation(requireContext(), android.R.anim.slide_in_left)
//////                                viewFlipper.setOutAnimation(requireContext(), android.R.anim.slide_out_right)
//////                                viewFlipper.showNext()
////
////                            } else {
////                                Toast.makeText(requireContext(), "Invalid OTP!", Toast.LENGTH_LONG)
////                                    .show()
////                            }
////                        }
////                    } catch (e: Exception) {
////                        withContext(Dispatchers.Main) {
////                            Toast.makeText(
////                                requireContext(),
////                                "Network Error: ${e.message}",
////                                Toast.LENGTH_LONG
////                            ).show()
////                        }
////                    } finally {
////                        withContext(Dispatchers.Main) {
////                            (requireActivity() as MainActivity).stopRefreshAnimation()
////                        }
////                    }
////                }
////            }
////
//////        btnSubmitOTP.setOnClickListener {
//////            viewFlipper.setInAnimation(this.context, android.R.anim.slide_in_left)
//////            viewFlipper.setOutAnimation(this.context, android.R.anim.slide_out_right)
//////
//////            viewFlipper.showNext()
//////        }
////
////
//////        view.findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
//////            val navOptions = NavOptions.Builder()
//////                .setEnterAnim(R.anim.slide_in_right)
//////                .setExitAnim(R.anim.slide_out_left)
//////                .setPopEnterAnim(R.anim.slide_in_left)
//////                .setPopExitAnim(R.anim.slide_out_right)
//////                .build()
//////
//////            findNavController().navigate(
//////                R.id.action_login_to_main_app,
//////                null,
//////                navOptions
//////            )
//////            parentFragmentManager.commit {
//////                // Set animations BEFORE adding/replacing
//////                setCustomAnimations(
//////                    R.anim.slide_in_right, // enter
//////                    R.anim.slide_out_left, // exit
//////                    R.anim.slide_in_left,  // popEnter
//////                    R.anim.slide_out_right // popExit
//////                )
//////                replace(R.id.fragmentContainerView, HomeFragment())
//////                addToBackStack(null) // Allows the user to go back
////
////            btnGetStarted.setOnClickListener {
////                // 1. Get the JWT token we saved during the OTP step
////                val sharedPref = requireActivity().getSharedPreferences(
////                    "MyAppPrefs",
////                    android.content.Context.MODE_PRIVATE
////                )
////                val jwtToken = sharedPref.getString("JWT_TOKEN", "") ?: ""
////
////                if (jwtToken.isEmpty()) {
////                    Toast.makeText(
////                        requireContext(),
////                        "Authentication error. Please log in again.",
////                        Toast.LENGTH_SHORT
////                    ).show()
////                    return@setOnClickListener
////                }
////
////                // 2. Convert our Strings into OkHttp RequestBodies so Retrofit can send them as Multipart data
////                val textType = "text/plain".toMediaTypeOrNull()
//////                        val jwtBody = jwtToken.toRequestBody(textType)
////                val favFoodsBody = finalFavFoods.toRequestBody(textType)
////                val allergiesBody = finalAllergies.toRequestBody(textType)
////                val tvErrorMessage2 = view.findViewById<TextView>(R.id.tvErrorMessage2)
////
////                // We set this to "false" because they just finished the onboarding flow! but haven't completed the profile
////                val isCompleteBody = "false".toRequestBody(textType)
////
////                // 3. Make the API Call!
////                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////                    try {
////                        // Notice we pass 'null' for things like bio and dob because we haven't asked for them yet!
////                        val response = RetrofitClient.ktorApi.updateProfile(
////                            token = "Bearer $jwtToken",
////                            name = null,
////                            country = null,
////                            username = null,
////                            dob = null,
////                            bio = null,
////                            favFoods = favFoodsBody,
////                            allergies = allergiesBody,
////                            isCompleteCache = isCompleteBody,
////                            image = null,
////                            email = null
////                        )
////
////                        withContext(Dispatchers.Main) {
////                            if (response.isSuccessful && response.body()?.success == true) {
////
////                                // UPDATE CACHE: The profile is now fully complete!
////                                sharedPref.edit { putBoolean("isProfileComplete", false) }
////
////                                // Navigate to the Home Screen!
////                                val navOptions = NavOptions.Builder()
////                                    .setEnterAnim(R.anim.slide_in_right)
////                                    .setExitAnim(R.anim.slide_out_left)
////                                    .setPopEnterAnim(R.anim.slide_in_left)
////                                    .setPopExitAnim(R.anim.slide_out_right)
////                                    .build()
////
////                                findNavController().navigate(
////                                    R.id.action_login_to_main_app,
////                                    null,
////                                    navOptions
////                                )
////
////                            } else {
////                                Toast.makeText(
////                                    requireContext(),
////                                    "Failed to save profile: ${response.body()?.message}",
////                                    Toast.LENGTH_LONG
////                                ).show()
////                            }
////                        }
////                    } catch (e: Exception) {
////                        withContext(Dispatchers.Main) {
//////                                    Toast.makeText(requireContext(), "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
////                            tvErrorMessage2.text = "Network Error: ${e.message}"
////                            tvErrorMessage2.visibility = View.VISIBLE
////                        }
////                    }
////                }
////            }
////
////
//////        Allergy Selection
////
////            val recyclerViewallergy = view.findViewById<RecyclerView>(R.id.recyclerViewAllergy)
////
////            val allergyView = ArrayList<AllergyView>()
////
////            allergyView.add(
////                AllergyView(
////                    R.drawable.milk,
////                    "Milk"
////                )
////            )
////            allergyView.add(
////                AllergyView(
////                    R.drawable.egg,
////                    "Eggs"
////                )
////            )
////            allergyView.add(
////                AllergyView(
////                    R.drawable.peanut,
////                    "Peanuts"
////                )
////            )
////            allergyView.add(
////                AllergyView(
////                    R.drawable.tree_nuts,
////                    "Tree Nuts"
////                )
////            )
////            allergyView.add(
////                AllergyView(
////                    R.drawable.fish,
////                    "Fish"
////                )
////            )
////            allergyView.add(
////                AllergyView(
////                    R.drawable.sesame,
////                    "Sesame"
////                )
////            )
////            allergyView.add(
////                AllergyView(
////                    R.drawable.wheat,
////                    "Wheat"
////                )
////            )
////            allergyView.add(
////                AllergyView(
////                    R.drawable.soybean,
////                    "Soybeans"
////                )
////            )
////            allergyView.add(
////                AllergyView(
////                    R.drawable.block,
////                    "No Allergy"
////                )
////            )
////
//////        recyclerView.layoutManager = LinearLayoutManager(requireContext())
////
////            val adapterallergy = AdapterAllergy(allergyView)
////            recyclerViewallergy.adapter = adapterallergy
////
////
//////        Fav Food Selection
////
////            val recyclerViewfavfood = view.findViewById<RecyclerView>(R.id.recyclerViewFavFoods)
////
////            val favFoodView = ArrayList<FavFoodView>()
////
////            favFoodView.add(
////                FavFoodView(
////                    R.drawable.vegetable,
////                    "Vegetables"
////                )
////            )
////            favFoodView.add(
////                FavFoodView(
////                    R.drawable.fruits,
////                    "Fruits"
////                )
////            )
////            favFoodView.add(
////                FavFoodView(
////                    R.drawable.dairy_products,
////                    "Dairy Products"
////                )
////            )
////            favFoodView.add(
////                FavFoodView(
////                    R.drawable.chicken,
////                    "Chicken"
////                )
////            )
////            favFoodView.add(
////                FavFoodView(
////                    R.drawable.wheat,
////                    "Grains/Staples"
////                )
////            )
////            favFoodView.add(
////                FavFoodView(
////                    R.drawable.fish,
////                    "Seafood"
////                )
////            )
////            favFoodView.add(
////                FavFoodView(
////                    R.drawable.dessertssweets,
////                    "Desserts/Sweets"
////                )
////            )
////            favFoodView.add(
////                FavFoodView(
////                    R.drawable.soybean,
////                    "Legumes/Beans"
////                )
////            )
////            favFoodView.add(
////                FavFoodView(
////                    R.drawable.tree_nuts,
////                    "Nuts & Seeds"
////                )
////            )
////
//////        recyclerView.layoutManager = LinearLayoutManager(requireContext())
////
////            val adapterfavfood = AdapterFavFood(favFoodView)
////            recyclerViewfavfood.adapter = adapterfavfood
////
////
//////        SELECTING FAV FOOD
////
////            btnContinue.setOnClickListener {
////                val selectedItemsFavFood = adapterfavfood.getSelectedFavFoods()
////
////                if (selectedItemsFavFood.isEmpty()) {
////                    Toast.makeText(
////                        requireContext(),
////                        "Please select at least one item",
////                        Toast.LENGTH_SHORT
////                    ).show()
////                } else {
//////                for (food in selectedItemsFavFood) {
//////                    println("User selected Fav Food Items: ${food.nameText}")
//////                }
////                    finalFavFoods =
////                        selectedItemsFavFood.joinToString(separator = ", ") { it.nameText }
////                    println("User selected Fav Food Items: $finalFavFoods")
////
////                    viewFlipper.showNext()
////                }
////            }
////
//////        SELECTING ALLERGY
////
////            btnContinue2.setOnClickListener {
////                val selectedItemsAllergy = adapterallergy.getSelectedAllergyFoods()
////
////                if (selectedItemsAllergy.isEmpty()) {
////                    Toast.makeText(
////                        requireContext(),
////                        "Please select at least one item",
////                        Toast.LENGTH_SHORT
////                    ).show()
////                } else {
//////                for (food in selectedItemsAllergy) {
//////                    println("User selected Allergy Items: ${food.nameText}")
//////                }
////                    finalAllergies =
////                        selectedItemsAllergy.joinToString(separator = ", ") { it.nameText }
////                    println("User selected Allergy Items: $finalAllergies")
////
////                    viewFlipper.showNext()
////                }
////            }
////
//////            val selectedItems = AdapterFavFood.getSelectedFoods()
//////
//////            if (selectedItems.isEmpty()) {
//////                // Show a Toast telling the user to select at least one item
//////            } else {
//////                // Do whatever you want with the selected items!
//////                // Example: pass them to the next fragment or save them
//////                for (food in selectedItems) {
//////                    println("User selected: ${food.nameText}")
//////                }
//////            }
//////        }
////
////
////        }
////    }
////    private fun updateOtpButtonState(otpBoxes: List<com.google.android.material.textfield.TextInputEditText>) {
////        val isComplete = otpBoxes.all { it.text?.length == 1 }
////        btnSubmitOTP.isEnabled = isComplete
////    }
////
////    private fun setupObservers() {
////        viewModel.otpSentSuccess.observe(viewLifecycleOwner) { success ->
//////            if (success) {
//////                Toast.makeText(requireContext(), "OTP Sent!", Toast.LENGTH_SHORT).show()
//////                showNextStep()
//////            }
////            if (success == true && viewFlipper.displayedChild == 0) {
////                Toast.makeText(requireContext(), "OTP Sent!", Toast.LENGTH_SHORT).show()
////                showNextStep()
////                viewModel.otpSentSuccess.value = false
////            } else {
////                Toast.makeText(requireContext(), "Failed to send OTP", Toast.LENGTH_SHORT).show()
////            }
////        }
////
////        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
////            error?.let {
////                tvErrorMessage.text = it
////                tvErrorMessage.visibility = View.VISIBLE
////                viewModel.errorMessage.value = null
////            }
////        }
////    }
////
////    private fun setupOtpLogic(view: View) {
////        val etOtp1 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp1)
////        val etOtp2 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp2)
////        val etOtp3 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp3)
////        val etOtp4 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp4)
////        val etOtp5 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp5)
////        val etOtp6 = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOtp6)
////
////        val otpBoxes = listOf(etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6)
////
////        otpBoxes.forEachIndexed { index, editText ->
////            // Auto-advance
////            editText.doOnTextChanged { text, _, _, _ ->
////                if (text?.length == 1 && index < otpBoxes.size - 1) {
////                    otpBoxes[index + 1].requestFocus()
////                }
////                updateOtpButtonState(otpBoxes)
////            }
////
////            // Backspace logic
////            editText.setOnKeyListener { _, keyCode, event ->
////                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
////                    if (editText.text.isNullOrEmpty() && index > 0) {
////                        otpBoxes[index - 1].requestFocus()
////                        otpBoxes[index - 1].text?.clear()
////                        updateOtpButtonState(otpBoxes)
////                        return@setOnKeyListener true
////                    }
////                }
////                false
////            }
////        }
////    }
////
////    private fun showNextStep() {
////        viewFlipper.setInAnimation(requireContext(), android.R.anim.slide_in_left)
////        viewFlipper.setOutAnimation(requireContext(), android.R.anim.slide_out_right)
////        viewFlipper.showNext()
////        viewModel.currentStep = viewFlipper.displayedChild // Save to ViewModel
////    }
////}
////
//////        // Switch to the previous view
//////        btnPrevious.setOnClickListener {
//////            viewFlipper.showPrevious()
//////        }
////
////        /* * Optional: If you want it to act like a slideshow that plays automatically
////         * viewFlipper.flipInterval = 2000 // 2000ms = 2 seconds
////         * viewFlipper.startFlipping()
////         */
////
//////        view.findViewById<Button>(R.id.button_flipper).setOnClickListener {
//////            parentFragmentManager.commit {
//////                setCustomAnimations(
//////                    R.anim.slide_in_right, // enter
//////                    R.anim.slide_out_left, // exit
//////                    R.anim.slide_in_left,  // popEnter
//////                    R.anim.slide_out_right // popExit
//////                )
//////                replace(R.id.fragmentContainerView, LoginCreateAccountFragment())
////////                addToBackStack(null) // Allows the user to go back
//////            }
//////        }
//////    }
//////}}}
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
////// * Use the [LoginCreateAccountFragment.newInstance] factory method to
////// * create an instance of this fragment.
////// */
//////class LoginCreateAccountFragment : Fragment() {
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
//////        return inflater.inflate(R.layout.fragment_login_create_account, container, false)
//////    }
//////
//////    companion object {
//////        /**
//////         * Use this factory method to create a new instance of
//////         * this fragment using the provided parameters.
//////         *
//////         * @param param1 Parameter 1.
//////         * @param param2 Parameter 2.
//////         * @return A new instance of fragment LoginCreateAccountFragment.
//////         */
//////        // TODO: Rename and change types and number of parameters
//////        @JvmStatic
//////        fun newInstance(param1: String, param2: String) =
//////            LoginCreateAccountFragment().apply {
//////                arguments = Bundle().apply {
//////                    putString(ARG_PARAM1, param1)
//////                    putString(ARG_PARAM2, param2)
//////                }
//////            }
//////    }
//////}