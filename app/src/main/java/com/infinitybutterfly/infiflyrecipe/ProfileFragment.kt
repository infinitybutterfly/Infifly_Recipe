package com.infinitybutterfly.infiflyrecipe

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.datepicker.MaterialDatePicker
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.graphics.Color
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.graphics.toColorInt
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.infinitybutterfly.infiflyrecipe.databinding.FragmentAddRecipeBinding
import com.infinitybutterfly.infiflyrecipe.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.infinitybutterfly.infiflyrecipe.utils.LoadingDialog
import kotlin.math.abs
import kotlin.math.max
import androidx.fragment.app.setFragmentResultListener

class ProfileFragment : Fragment(), RefreshableFragment {
    private var _binding: FragmentProfileBinding? = null
    private lateinit var flipper: ViewFlipper
    private lateinit var loadingDialog: LoadingDialog
    private var selectedImageUri: Uri? = null
    private var usernameCheckJob: Job? = null
    private var originalUsername: String = ""
    private var originalName: String = ""
    private var originalCountry: String = ""
    private var originalBio: String = ""
    private var originalEmail: String = ""
    private var originalDob: String = ""
    private var originalImageUrl: String = ""
    private var tempCameraUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            updateImageViews(uri)
        }
    }

    // 2. CAMERA LAUNCHER (Waits for the camera to finish taking the full-size photo)
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            selectedImageUri = tempCameraUri
            updateImageViews(tempCameraUri!!)
        }
    }

    // 3. PERMISSION LAUNCHER (Asks the user for Camera access)
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to update both images at once
    private fun updateImageViews(uri: Uri) {
        val headerLayout = requireView().findViewById<View>(R.id.header_layout)
        val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
        headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
        editLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
    }

//    private var hasShownToast = true

//    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        if (uri != null) {
//            selectedImageUri = uri
//
//            val headerLayout = requireView().findViewById<View>(R.id.header_layout)
//            val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
//
//            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
//            editLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
//        }
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener("premium_upgrade") { _, bundle ->
            val isSuccess = bundle.getBoolean("success")
            if (isSuccess) {
                fetchAndDisplayProfile(view, false)
            }
        }

        loadingDialog = LoadingDialog(requireActivity())
        flipper = view.findViewById(R.id.profile_flipper)

        if (savedInstanceState != null) {
            val savedTemp = savedInstanceState.getString("SAVED_TEMP_URI")
            if (savedTemp != null) {
                tempCameraUri = Uri.parse(savedTemp)
            }

            val savedSelected = savedInstanceState.getString("SAVED_SELECTED_URI")
            if (savedSelected != null) {
                selectedImageUri = Uri.parse(savedSelected)
                // We must use a small delay or post() because the views might not be fully inflated yet
                view.post {
                    updateImageViews(selectedImageUri!!)
                }
            }
            val savedFlipperState = savedInstanceState.getInt("FLIPPER_STATE", 0)
            view.post { switchProfileScreen(savedFlipperState) }

            originalUsername = savedInstanceState.getString("ORIG_USERNAME") ?: ""
            originalName = savedInstanceState.getString("ORIG_NAME") ?: ""
            originalCountry = savedInstanceState.getString("ORIG_COUNTRY") ?: ""
            originalBio = savedInstanceState.getString("ORIG_BIO") ?: ""
            originalEmail = savedInstanceState.getString("ORIG_EMAIL") ?: ""
            originalDob = savedInstanceState.getString("ORIG_DOB") ?: ""
            originalImageUrl = savedInstanceState.getString("ORIG_IMAGE_URL") ?: ""
        }

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            // If the keyboard is open, add padding to the bottom equal to the keyboard height.
            // If closed, default to the normal system bar height.
            val paddingBottom = if (imeVisible) imeHeight else systemBars

            v.setPadding(0, 0, 0, paddingBottom)
            insets
        }

        flipper = view.findViewById(R.id.profile_flipper)

//        fetchAndDisplayProfile(view)
        val isRestoring = savedInstanceState != null
        fetchAndDisplayProfile(view, isRestoring)

        setupUsernameValidation()

        setupCountryDropdown()

        val headerLayout = view.findViewById<View>(R.id.header_layout)
        val editLayout = view.findViewById<View>(R.id.edit_profile_include)

//        headerLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
//            pickImageLauncher.launch("image/*")
//        }
//
//        editLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
//            pickImageLauncher.launch("image/*")
//        }
        headerLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
            showFullScreenImage()
        }

        editLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
            showImageSourceDialog()
        }

//        view.findViewById<Button>(R.id.edit_profile).setOnClickListener {
////            flipper.displayedChild = 1
//            switchProfileScreen(1)
//            (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
//        }

//        //Checking only the Sharedpref
//        view.findViewById<Button>(R.id.edit_profile).setOnClickListener {
//            val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
//            val isPremium = sharedPref.getBoolean("isLocked", false)
//
//            if (isPremium) {
//                switchProfileScreen(1)
//                (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
//            } else {
//                val paywallSheet = ProfilePaywallBottomSheet()
//                paywallSheet.isCancelable = false // Prevent clicking in the background to dismiss
//                paywallSheet.show(parentFragmentManager, "ProfilePaywall")
//            }
//        }

        //Not only checking the sharedpref but also the with server
        view.findViewById<Button>(R.id.edit_profile).setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val jwtToken = sharedPref.getString("JWT_TOKEN", "") ?: ""

            // 1. Show the loader because a network call takes a split second
            loadingDialog.startLoading()

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 2. Ask the server for the live truth!
                    val response = RetrofitClient.profileApi.getUserProfile("Bearer $jwtToken")

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            // The server answered! Grab the live Premium status
                            val isPremiumLive = response.body()!!.isLocked

                            // Immediately update the local cache so it stays accurate
                            sharedPref.edit().putBoolean("isLocked", isPremiumLive ?: false).apply()

                            // 3. Route the user based on the LIVE server response
                            if (isPremiumLive ?: false) {
                                switchProfileScreen(1)
                                (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
                            } else {
                                val paywallSheet = ProfilePaywallBottomSheet()
                                paywallSheet.isCancelable = false
                                paywallSheet.show(parentFragmentManager, "ProfilePaywall")
                            }
                        } else {
                            // Server glitch (e.g. 500 error). Fallback to local cache!
                            handleEditProfileCacheFallback(sharedPref)
                        }
                    }
                } catch (e: Exception) {
                    // Network crash (e.g. user is in airplane mode). Fallback to local cache!
                    withContext(Dispatchers.Main) {
                        handleEditProfileCacheFallback(sharedPref)
                    }
                } finally {
                    // 4. ALWAYS hide the loader, no matter what happens
                    withContext(Dispatchers.Main) {
                        loadingDialog.isDismiss()
                    }
                }
            }
        }

//        view.findViewById<Button>(R.id.help_center).setOnClickListener { flipper.displayedChild = 2 }
        view.findViewById<Button>(R.id.help_center).setOnClickListener {
            requireView().findViewById<EditText>(R.id.problem_title).text.clear()
            requireView().findViewById<EditText>(R.id.problem_description).text.clear()

//            flipper.displayedChild = 2
            switchProfileScreen(2)
            (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
        }
        view.findViewById<Button>(R.id.privacy_policy).setOnClickListener {
            switchProfileScreen(3)
            (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
        }

        view.findViewById<Button>(R.id.rate_the_app).setOnClickListener {
            val appPackageName = requireContext().packageName
            try {
                startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri()))
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$appPackageName".toUri()))
            }
        }

//        view.findViewById<Button>(R.id.logout).setOnClickListener {
//            AlertDialog.Builder(requireContext())
//                .setTitle("Log Out")
//                .setMessage("Are you sure you want to log out?")
//                .setPositiveButton("Yes") { _, _ ->
//                    // FIXED: Now clears the correct MyAppPrefs file!
//                    requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).edit { clear() }
//                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
//
//                    // Optional: Navigate back to the login screen here!
//                }
//                .setNegativeButton("No", null)
//                .show()
//        }
//
//        binding.btnLogout.setOnClickListener {
//            // 1. Wipe the SharedPreferences clean!
//            val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
//            sharedPref.edit().clear().apply()
//
//            // 2. Kick them back to the Login graph and destroy the back stack
//            // so they can't press the physical back button to get back into the app!
//            val navOptions = NavOptions.Builder()
//                .setPopUpTo(R.id.main_nav_graph, true) // Destroy the whole app history
//                .build()
//
//            findNavController().navigate(R.id.loginWelcomeFragment, null, navOptions)
//        }

        view.findViewById<Button>(R.id.logout).setOnClickListener {
//            AlertDialog.Builder(requireContext())
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->

                    val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()

                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.auth_nav, true) // Destroy the whole app history
                        .build()

                    findNavController().navigate(R.id.loginWelcomeFragment, null, navOptions)
                }
                .setNegativeButton("No", null)
                .show()
        }

        val dobEditText = editLayout.findViewById<EditText>(R.id.date_of_birth_edit)

        dobEditText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date of Birth")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = sdf.format(Date(selection))
                dobEditText.setText(formattedDate)
            }

            datePicker.show(parentFragmentManager, "DOB_PICKER")
        }

        //It will leave with out showing anything, even when the changes are made to profile
//        editLayout.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
//            switchProfileScreen(0)
//            (requireActivity() as MainActivity).setSwipeRefreshEnabled(true)
//        }

        editLayout.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            attemptToLeaveEditScreen(editLayout)
        }

        view.findViewById<Button>(R.id.button).setOnClickListener {
            val updatedName = editLayout.findViewById<EditText>(R.id.chef_name_edit).text.toString()
            val updatedUsername = editLayout.findViewById<EditText>(R.id.user_name_edit).text.toString()
            val updatedCountry = editLayout.findViewById<EditText>(R.id.country_name_edit).text.toString()
            val updatedEmail = editLayout.findViewById<EditText>(R.id.user_email_id).text.toString()
            val updatedBio = editLayout.findViewById<EditText>(R.id.user_bio_edit).text.toString()
            val updatedDob = dobEditText.text.toString()

            // FIXED: Using the exact correct keys to pull the JWT!
            val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val jwtToken = sharedPref.getString("JWT_TOKEN", "") ?: ""

            saveProfileData(jwtToken, updatedName, updatedUsername, updatedCountry, updatedDob, updatedBio, updatedEmail)
        }

        val nameEditText = editLayout.findViewById<EditText>(R.id.chef_name_edit)
        val usernameEditText = editLayout.findViewById<EditText>(R.id.user_name_edit)
        val countryEditText = editLayout.findViewById<EditText>(R.id.country_name_edit)
        val saveButton = view.findViewById<Button>(R.id.button)

        fun validateSaveButton() {
            val isNameFilled = nameEditText.text.toString().trim().isNotEmpty()
            val isUsernameFilled = usernameEditText.text.toString().trim().isNotEmpty()
            val isCountryFilled = countryEditText.text.toString().trim().isNotEmpty()
            val isDobFilled = dobEditText.text.toString().trim().isNotEmpty()

            val allValid = isNameFilled && isUsernameFilled && isCountryFilled && isDobFilled

            saveButton.isEnabled = allValid
        }

        nameEditText.doAfterTextChanged { validateSaveButton() }
        usernameEditText.doAfterTextChanged { validateSaveButton() }
        countryEditText.doAfterTextChanged { validateSaveButton() }
        dobEditText.doAfterTextChanged { validateSaveButton() }

        // Run it once immediately when the screen loads to disable the button initially if needed
        validateSaveButton()

        view.findViewById<Button>(R.id.send_problem).setOnClickListener {
            val title = view.findViewById<EditText>(R.id.problem_title).text.toString()
            val desc = view.findViewById<EditText>(R.id.problem_description).text.toString()
            submitHelpTicket(title, desc)
        }

//        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
////                if (flipper.displayedChild != 0) {
////                    flipper.displayedChild = 0
////
//                if (flipper.displayedChild != 0) {
//                    switchProfileScreen(0)
//                    (requireActivity() as MainActivity).setSwipeRefreshEnabled(true)
//                } else {
//                    isEnabled = false
//                    requireActivity().onBackPressedDispatcher.onBackPressed()
//                }
//            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (flipper.displayedChild) {
                    1 -> {
                        // We are on the Edit Profile screen
                        attemptToLeaveEditScreen(editLayout)
                    }
                    0 -> {
                        // We are on the Main Profile screen, let Android close the app/tab
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                    else -> {
                        // We are on Help Center or Privacy, just go back instantly
                        switchProfileScreen(0)
                        (requireActivity() as MainActivity).setSwipeRefreshEnabled(true)
                    }
                }
            }
        })
    }

    private fun setupCountryDropdown() {
        val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
        val countryDropdown = editLayout.findViewById<MaterialAutoCompleteTextView>(R.id.country_name_edit)
        val countries = resources.getStringArray(R.array.country_list)

        // Create a custom adapter that disables the search filter completely!
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, countries) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        // Always return the full list of countries
                        return FilterResults().apply {
                            values = countries
                            count = countries.size
                        }
                    }
                    @Suppress("UNCHECKED_CAST")
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        countryDropdown.setAdapter(adapter)
    }
//    private fun setupCountryDropdown() {
//        // 1. Find the AutoCompleteTextView
//        val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
////        val countryDropdown = editLayout.findViewById<AutoCompleteTextView>(R.id.country_name_edit)
//        val countryDropdown = editLayout.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.country_name_edit)
//
//        // 2. Grab the list of countries from your strings.xml
//        val countries = resources.getStringArray(R.array.country_list)
//
//        // 3. Create the adapter. We use a built-in Android layout for the dropdown items!
//        val adapter = ArrayAdapter(
//            requireContext(),
//            android.R.layout.simple_dropdown_item_1line,
//            countries
//        )
//
//        // 4. Attach the adapter to the dropdown
//        countryDropdown.setAdapter(adapter)
//    }
//    private fun setupUsernameValidation() {
//        val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
//        val usernameEditText = editLayout.findViewById<EditText>(R.id.user_name_edit)
//        val textInputLayout = editLayout.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.un)
//
//        usernameEditText.doAfterTextChanged { text ->
//            val usernameval = text.toString().trim()
//
//            // Cancel the previous timer immediately!
//            usernameCheckJob?.cancel()
//
//            if (usernameval.isEmpty()) {
//                Log.d("user_name_check", "is empty")
//
//                // 1. EMPTY: Clear everything and turn the box back to standard Gray
//                textInputLayout.error = null
//                textInputLayout.helperText = null
//                textInputLayout.boxStrokeColor = android.graphics.Color.GRAY
//                return@doAfterTextChanged
//            }
//
//            // 3. Start a new timer!
//            usernameCheckJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
//
//                // THE DEBOUNCE: Wait 500 milliseconds
//                delay(500)
//
//                try {
//                    val response = RetrofitClient.ktorApi.checkUsernameAvailability(usernameval)
//
//                    if (response.isSuccessful && response.body() != null) {
//                        val isAvailable = response.body()!!.available
//
//                        if (isAvailable) {
//                            // 2. SUCCESS: Force the Box and the Helper Text to be Green!
//                            textInputLayout.error = null
//                            textInputLayout.helperText = "Username available!"
//
//                            // Change the helper text to Green
//                            val greenColorList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00C853"))
//                            textInputLayout.setHelperTextColor(greenColorList)
//
//                            // Change the outline box to Green (using standard Int!)
//                            textInputLayout.boxStrokeColor = android.graphics.Color.parseColor("#00C853")
//                        } else {
//                            // 3. TAKEN: Clear helper text and trigger the Error
//                            textInputLayout.helperText = null
//
//                            // We don't even need to set Color.RED manually.
//                            // Activating `.error` automatically forces the box and text to turn Red!
//                            textInputLayout.error = "This username is already taken"
//                        }
//                    }
//                } catch (e: Exception) {
//                    Log.e("API_ERROR", "Failed to check username: ${e.message}")
//                }
//            }
//        }
//    }

//    private fun setupUsernameValidation() {
//        val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
//        val usernameEditText = editLayout.findViewById<EditText>(R.id.user_name_edit)
//        val textInputLayout = editLayout.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.un)
//
//        // 1. Build the Default Color List (What it looks like normally)
//        // Note: You can change Color.BLACK to your app's main theme color (e.g., Color.parseColor("#6200EE"))
//        val defaultColorStateList = android.content.res.ColorStateList(
//            arrayOf(
//                intArrayOf(android.R.attr.state_focused), // When the user is typing
//                intArrayOf()                              // When the user clicks away
//            ),
//            intArrayOf(
//                android.graphics.Color.BLACK, // Color when typing
//                android.graphics.Color.GRAY   // Color when clicked away
//            )
//        )
//
//        // 2. Build the Green Color List for Success
//        val greenColorStateList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00C853"))
//
//        usernameEditText.doAfterTextChanged { text ->
//            val usernameval = text.toString().trim()
//
//            // Cancel the previous timer immediately!
//            usernameCheckJob?.cancel()
//
//            if (usernameval.isEmpty()) {
//                Log.d("user_name_check", "is empty")
//
//                textInputLayout.error = null
//                textInputLayout.helperText = null
//
//                // Apply our manual Default State!
//                textInputLayout.setBoxStrokeColorStateList(defaultColorStateList)
//                return@doAfterTextChanged
//            }
//
//            // 3. Start a new timer!
//            usernameCheckJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
//
//                // THE DEBOUNCE: Wait 500 milliseconds
//                delay(500)
//
//                try {
//                    val response = RetrofitClient.ktorApi.checkUsernameAvailability(usernameval)
//
//                    if (response.isSuccessful && response.body() != null) {
//                        val isAvailable = response.body()!!.available
//                        Log.d("user_name_check", "response body successfully")
//
//                        if (isAvailable) {
//                            // SUCCESS: Force the Box and the Helper Text to be Green!
//                            textInputLayout.error = null
//                            textInputLayout.helperText = "Username available!"
//                            Log.d("user_name_check", "username available")
//
//                            textInputLayout.setHelperTextColor(greenColorStateList)
//                            textInputLayout.setBoxStrokeColorStateList(greenColorStateList)
//                        } else {
//                            // TAKEN: Clear helper text
//                            textInputLayout.helperText = null
//
//                            // Restore Default State before triggering the error so it turns Red naturally!
//                            textInputLayout.setBoxStrokeColorStateList(defaultColorStateList)
//                            textInputLayout.error = "This username is already taken"
//                            Log.d("user_name_check", "username not available")
//                        }
//                    }
//                } catch (e: Exception) {
//                    Log.e("API_ERROR", "Failed to check username: ${e.message}")
//                }
//            }
//        }
//    }

//    private fun setupUsernameValidation() {
//        val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
//        val usernameEditText = editLayout.findViewById<EditText>(R.id.user_name_edit)
//        val textInputLayout = editLayout.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.un)
//
//        // 🚨 CRITICAL FIX 1: Capture the default colors BEFORE the user starts typing!
//        val defaultBoxColors = textInputLayout.boxStrokeColorStateList
//
//        usernameEditText.doAfterTextChanged { text ->
//            val usernameval = text.toString().trim()
//
//            // Cancel the previous timer immediately!
//            usernameCheckJob?.cancel()
//
//            if (usernameval.isEmpty()) {
//                Log.d("user_name_check", "is empty")
//
//                // Reset text messages
//                textInputLayout.error = null
//                textInputLayout.helperText = null
//
//                // 🚨 CRITICAL FIX 2: Restore the default state! DO NOT use Color.GRAY
//                if (defaultBoxColors != null) {
//                    textInputLayout.setBoxStrokeColorStateList(defaultBoxColors)
//                }
//                return@doAfterTextChanged
//            }
//
//            // 3. Start a new timer!
//            usernameCheckJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
//
//                // THE DEBOUNCE: Wait 500 milliseconds
//                delay(500)
//
//                try {
//                    val response = RetrofitClient.ktorApi.checkUsernameAvailability(usernameval)
//
//                    if (response.isSuccessful && response.body() != null) {
//                        val isAvailable = response.body()!!.available
//
//                        if (isAvailable) {
//                            // SUCCESS: Force the Box and the Helper Text to be Green!
//                            textInputLayout.error = null
//                            textInputLayout.helperText = "Username available!"
//
//                            val greenColorList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00C853"))
//                            textInputLayout.setHelperTextColor(greenColorList)
//                            textInputLayout.setBoxStrokeColorStateList(greenColorList)
//                        } else {
//                            // TAKEN: Clear helper text
//                            textInputLayout.helperText = null
//
//                            // 🚨 CRITICAL FIX 3: Restore the default state before triggering the error! DO NOT use Color.RED
//                            if (defaultBoxColors != null) {
//                                textInputLayout.setBoxStrokeColorStateList(defaultBoxColors)
//                            }
//
//                            // Triggering the error automatically turns the box and text Red
//                            textInputLayout.error = "This username is already taken"
//                        }
//                    }
//                } catch (e: Exception) {
//                    Log.e("API_ERROR", "Failed to check username: ${e.message}")
//                }
//            }
//        }
//    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save our temporary camera URI as a string so it survives death
        tempCameraUri?.let {
            outState.putString("SAVED_TEMP_URI", it.toString())
        }
        // Also save the selected image so they don't lose it if they rotate the screen
        selectedImageUri?.let {
            outState.putString("SAVED_SELECTED_URI", it.toString())
        }
        if (::flipper.isInitialized) {
            outState.putInt("FLIPPER_STATE", flipper.displayedChild)
        }
        outState.putString("ORIG_USERNAME", originalUsername)
        outState.putString("ORIG_NAME", originalName)
        outState.putString("ORIG_COUNTRY", originalCountry)
        outState.putString("ORIG_BIO", originalBio)
        outState.putString("ORIG_EMAIL", originalEmail)
        outState.putString("ORIG_DOB", originalDob)
        outState.putString("ORIG_IMAGE_URL", originalImageUrl)
    }

    private fun setupUsernameValidation() {
        // 1. Find the views manually just like you did in the rest of this Fragment
        val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
        val usernameEditText = editLayout.findViewById<EditText>(R.id.user_name_edit)

        //To change the outline color, we need the TextInputLayout, NOT the EditText!
        val textInputLayout = editLayout.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.un)

        // 2. Listen to every keystroke on the EditText
        usernameEditText.doAfterTextChanged { text ->
            val usernameval = text.toString().trim()

            // Cancel the previous timer immediately!
            usernameCheckJob?.cancel()

            if (usernameval.isEmpty()) {
                Log.d("user_name_check", "is empty")

                // Reset to default colors if the box is empty (Apply it to the layout!)
                textInputLayout.boxStrokeColor = Color.GRAY
                textInputLayout.error = null
                textInputLayout.helperText = null
                return@doAfterTextChanged
            }

            if (usernameval == originalUsername) {
                textInputLayout.error = null
                textInputLayout.helperText = "Current Username"

                val blueColor = android.content.res.ColorStateList.valueOf(Color.BLUE)
                textInputLayout.setHelperTextColor(blueColor)
                textInputLayout.setBoxStrokeColorStateList(blueColor)

                return@doAfterTextChanged
            }

            // 3. Start a new timer!
            usernameCheckJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {

                // THE DEBOUNCE: Wait 500 milliseconds.
                delay(500)

                // 4. If we survived 500ms of silence, ask the server!
                try {
                    val response = RetrofitClient.ktorApi.checkUsernameAvailability(usernameval)

                    if (response.isSuccessful && response.body() != null) {
                        val isAvailable = response.body()!!.available

                        if (isAvailable) {
                            // SUCCESS! Turn the outline GREEN and clear any errors
//                            textInputLayout.boxStrokeColor = Color.parseColor("#00C853") // Material Green
                            textInputLayout.error = null
                            textInputLayout.helperText = "Username available!"

                            val greenColor = android.content.res.ColorStateList.valueOf("#00C853".toColorInt())
                            textInputLayout.setHelperTextColor(greenColor)
                            textInputLayout.setBoxStrokeColorStateList(greenColor)
                        } else {
                            // TAKEN! Turn the outline RED and show error
                            textInputLayout.boxStrokeColor = Color.RED
                            textInputLayout.error = "This username is already taken"
                            textInputLayout.helperText = null
                        }
                    }
                } catch (e: Exception) {
                    Log.e("API_ERROR", "Failed to check username: ${e.message}")
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

        // Load the current image into it
        if (selectedImageUri != null) {
            imageView.setImageURI(selectedImageUri)
        } else if (originalImageUrl.isNotEmpty()) {
            Glide.with(requireContext()).load(originalImageUrl).into(imageView)
        } else {
            imageView.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        var startX = 0f
        var startY = 0f
        var isDragging = false
        val swipeThreshold = 300f // How far in pixels they have to drag to trigger the dismiss

        // 1. Standard click listener for accessibility and quick taps
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        // 2. Spotify-style swipe-to-dismiss listener
        imageView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    isDragging = false
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY

                    // If they moved their finger more than 20 pixels, count it as a drag
                    if (abs(deltaX) > 20 || abs(deltaY) > 20) {
                        isDragging = true
                    }

                    if (isDragging) {
                        v.translationX = deltaX
                        v.translationY = deltaY

                        // Fade out the image the further they drag it
                        val maxDistance = max(abs(deltaX), abs(deltaY))
                        v.alpha = 1f - (maxDistance / 1500f)
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY

                    if (isDragging) {
                        if (abs(deltaX) > swipeThreshold || abs(deltaY) > swipeThreshold) {
                            // Dragged far enough: Animate off-screen and dismiss
                            v.animate()
                                .translationX(deltaX * 5)
                                .translationY(deltaY * 5)
                                .alpha(0f)
                                .setDuration(250)
                                .withEndAction { dialog.dismiss() }
                                .start()
                        } else {
                            // Didn't drag far enough: Snap back to center
                            v.animate()
                                .translationX(0f)
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(250)
                                .start()
                        }
                    } else {
                        // It was just a tap! Trigger the standard click listener.
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

//
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
//        // Load the current image into it
//        if (selectedImageUri != null) {
//            imageView.setImageURI(selectedImageUri)
//        } else if (originalImageUrl.isNotEmpty()) {
//            Glide.with(requireContext()).load(originalImageUrl).into(imageView)
//        } else {
//            imageView.setImageResource(android.R.drawable.sym_def_app_icon)
//        }
//
//        // Close the full-screen view when tapped
//        imageView.setOnClickListener { dialog.dismiss() }
//
//        dialog.setContentView(imageView)
//        dialog.show()
//    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Update Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch() // Option 1: Camera
                    1 -> pickImageLauncher.launch("image/*") // Option 2: Gallery
                }
            }
            .show()
    }

    // Checks permission before opening camera
    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Creates a blank file and tells the camera to fill it
    private fun launchCamera() {
        try {
            // Create a temporary file in the cache directory
            val tempFile = File.createTempFile("profile_cam_", ".jpg", requireContext().cacheDir)

            // Get a secure URI for this file using the FileProvider we made in Step 1
            tempCameraUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                tempFile
            )

            // Launch the camera and tell it to save the photo to our secure URI
            takePictureLauncher.launch(tempCameraUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAndDisplayProfile(view: View, isRestoring: Boolean) {
        val headerLayout = view.findViewById<View>(R.id.header_layout)
        val editLayout = view.findViewById<View>(R.id.edit_profile_include)

//        headerLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
//            showImageSourceDialog()
//        }
//
//        editLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
//            showImageSourceDialog()
//        }
        loadingDialog.startLoading()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                val jwtToken = sharedPref.getString("JWT_TOKEN", "") ?: ""

                val response = RetrofitClient.profileApi.getUserProfile("Bearer $jwtToken")

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    if (response.isSuccessful && response.body() != null) {
                        val userProfile = response.body()!!
//                        sharedPref.edit().putBoolean("isLocked", userProfile.isLocked ?: false).apply()

                        originalUsername = userProfile.username ?: ""

                        originalName = userProfile.name ?: ""
                        originalCountry = userProfile.country ?: ""
                        originalBio = userProfile.bio ?: ""
                        originalEmail = userProfile.email ?: ""
                        originalDob = userProfile.dob ?: ""

                        headerLayout.findViewById<TextView>(R.id.user_name).text = userProfile.name ?: "Unknown User"
                        headerLayout.findViewById<TextView>(R.id.country_name).text = userProfile.country ?: "Unknown Country"
                        headerLayout.findViewById<TextView>(R.id.user_bio).text = userProfile.bio ?: "No bio available."

//                        editLayout.findViewById<EditText>(R.id.chef_name_edit).setText(userProfile.name ?: "")
//                        editLayout.findViewById<EditText>(R.id.user_name_edit).setText(userProfile.username ?: "")
////                        editLayout.findViewById<EditText>(R.id.country_name_edit).setText(userProfile.country ?: "")
//                        editLayout.findViewById<MaterialAutoCompleteTextView>(R.id.country_name_edit)
//                            .setText(userProfile.country ?: "", false)
//                        editLayout.findViewById<EditText>(R.id.user_bio_edit).setText(userProfile.bio ?: "")
//                        editLayout.findViewById<EditText>(R.id.user_email_id).setText(userProfile.email ?: "")
//                        editLayout.findViewById<EditText>(R.id.date_of_birth_edit).setText(userProfile.dob ?: "")

                        if (!isRestoring) {
                            editLayout.findViewById<EditText>(R.id.chef_name_edit).setText(userProfile.name ?: "")
                            editLayout.findViewById<EditText>(R.id.user_name_edit).setText(userProfile.username ?: "")
                            editLayout.findViewById<MaterialAutoCompleteTextView>(R.id.country_name_edit).setText(userProfile.country ?: "", false)
                            editLayout.findViewById<EditText>(R.id.user_bio_edit).setText(userProfile.bio ?: "")
                            editLayout.findViewById<EditText>(R.id.user_email_id).setText(userProfile.email ?: "")
                            editLayout.findViewById<EditText>(R.id.date_of_birth_edit).setText(userProfile.dob ?: "")
                        }

                        val imageUrl = userProfile.profilePicUrl ?: ""
                        originalImageUrl = imageUrl

                        Toast.makeText(requireContext(), "Profile Loaded Successfully", Toast.LENGTH_SHORT).show()

//                        Everytime I am gonna open the profile fragment the hasShownToast will be reset and I will get the toast message every time. As i am on switching between fragment.
//                        if (hasShownToast) {
//                            Toast.makeText(requireContext(), "Profile Loaded Successfully", Toast.LENGTH_SHORT).show()
//                            hasShownToast = false
//                        }

                        if (imageUrl.isNotEmpty()) {

                            val glideListener = object : RequestListener<Drawable> {
                                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean { return false }
                                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean { return false }
//                                override fun onLoadFailed(
//                                    e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
//                                ): Boolean {
//                                    Log.e("GlideError", "Image failed to load! URL: $imageUrl", e)
//                                    Toast.makeText(requireContext(), "Failed to load Profile Image", Toast.LENGTH_SHORT).show()
//                                    return false
//                                }
//
//                                override fun onResourceReady(
//                                    resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean
//                                ): Boolean {
//                                    return false
//                                }
                            }

                            Glide.with(requireContext())
                                .load(imageUrl)
                                .listener(glideListener)
                                .into(headerLayout.findViewById(R.id.user_pic))

//                            Glide.with(requireContext())
//                                .load(imageUrl)
//                                .listener(glideListener)
//                                .into(editLayout.findViewById(R.id.user_pic))

                            if (selectedImageUri == null) {
                                Glide.with(requireContext()).load(imageUrl).listener(glideListener).into(editLayout.findViewById(R.id.user_pic))
                            }
                        }
//                        if (imageUrl.isNotEmpty()) {
//                            Glide.with(requireContext()).load(imageUrl).into(headerLayout.findViewById<ImageView>(R.id.user_pic))
//                            Glide.with(requireContext()).load(imageUrl).into(editLayout.findViewById<ImageView>(R.id.user_pic))
//                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to load / No profile data available.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Network error: Could not fetch profile.", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.isDismiss()
                    if (isAdded){
                    (activity as? MainActivity)?.stopRefreshAnimation()
                }}
            }
        }
    }

    private fun handleEditProfileCacheFallback(sharedPref: android.content.SharedPreferences) {
        // If the server fails, we trust whatever we saved last
        val isPremiumCached = sharedPref.getBoolean("isLocked", false)

        if (isPremiumCached) {
            switchProfileScreen(1)
            (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
        } else {
            val paywallSheet = ProfilePaywallBottomSheet()
            paywallSheet.isCancelable = false
            paywallSheet.show(parentFragmentManager, "ProfilePaywall")
        }
    }

    private fun hasUnsavedChanges(editLayout: View): Boolean {
        val currentName = editLayout.findViewById<EditText>(R.id.chef_name_edit).text.toString()
        val currentUsername = editLayout.findViewById<EditText>(R.id.user_name_edit).text.toString()
        val currentCountry = editLayout.findViewById<EditText>(R.id.country_name_edit).text.toString()
        val currentBio = editLayout.findViewById<EditText>(R.id.user_bio_edit).text.toString()
        val currentEmail = editLayout.findViewById<EditText>(R.id.user_email_id).text.toString()
        val currentDob = editLayout.findViewById<EditText>(R.id.date_of_birth_edit).text.toString()

        return currentName != originalName ||
                currentUsername != originalUsername ||
                currentCountry != originalCountry ||
                currentBio != originalBio ||
                currentEmail != originalEmail ||
                currentDob != originalDob ||
                selectedImageUri != null // Check if they picked a new photo!
    }

    private fun attemptToLeaveEditScreen(editLayout: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editLayout.windowToken, 0)

        editLayout.scrollTo(0, 0)
        if (hasUnsavedChanges(editLayout)) {
//            AlertDialog.Builder(requireContext())
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Unsaved Changes")
                .setMessage("Are you sure you want to go back? Your changes won't be saved.")
                .setPositiveButton("Yes") { _, _ ->

                    //Revert the changes made back to normal
                    editLayout.findViewById<EditText>(R.id.chef_name_edit).setText(originalName)
                    editLayout.findViewById<EditText>(R.id.user_name_edit).setText(originalUsername)
                    editLayout.findViewById<EditText>(R.id.user_bio_edit).setText(originalBio)
                    editLayout.findViewById<EditText>(R.id.user_email_id).setText(originalEmail)
                    editLayout.findViewById<EditText>(R.id.date_of_birth_edit).setText(originalDob)

                    // (Remember to pass 'false' to the country dropdown so it doesn't filter!)
                    editLayout.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.country_name_edit)
                        .setText(originalCountry, false)

                    //REVERT THE IMAGE
                    selectedImageUri = null
                    val headerLayout = requireView().findViewById<View>(R.id.header_layout)

                    if (originalImageUrl.isNotEmpty()) {
                        Glide.with(requireContext()).load(originalImageUrl).into(headerLayout.findViewById(R.id.user_pic))
                        Glide.with(requireContext()).load(originalImageUrl).into(editLayout.findViewById(R.id.user_pic))
                    } else {
                        // If they didn't have a picture to begin with, restore the default icon
                        headerLayout.findViewById<ImageView>(R.id.user_pic).setImageResource(android.R.drawable.sym_def_app_icon)
                        editLayout.findViewById<ImageView>(R.id.user_pic).setImageResource(android.R.drawable.sym_def_app_icon)
                    }
                    // They want to leave. Wipe the pending image and go back.
                    selectedImageUri = null
                    switchProfileScreen(0)
                    (requireActivity() as MainActivity).setSwipeRefreshEnabled(true)
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            // No changes were made, so just go back instantly without nagging them
            switchProfileScreen(0)
            (requireActivity() as MainActivity).setSwipeRefreshEnabled(true)
        }
    }

    private fun saveProfileData(jwt: String, name: String, username: String, country: String, dob: String, bio: String, email: String) {
        val headerLayout = requireView().findViewById<View>(R.id.header_layout)

        val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // The JWT is no longer converted into a RequestBody!
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
                val dobBody = dob.toRequestBody("text/plain".toMediaTypeOrNull())
                val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
                val bioBody = bio.toRequestBody("text/plain".toMediaTypeOrNull())
                val cacheStatusBody = "true".toRequestBody("text/plain".toMediaTypeOrNull())
                val isLocked = "false".toRequestBody("text/plain".toMediaTypeOrNull())

                var imagePart: MultipartBody.Part? = null
                if (selectedImageUri != null) {
                    val imageFile = getFileFromUri(selectedImageUri!!)
                    val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                }

                // FIXED: Using Named Parameters and passing 'null' for the missing food/allergy parts!
                val response = RetrofitClient.profileApi.updateProfile(
                    token = "Bearer $jwt", // Passed as a String header!
                    email = emailBody,
                    name = nameBody,
                    country = countryBody,
                    username = usernameBody,
                    dob = dobBody,
                    bio = bioBody,
                    favFoods = null, // We pass null because we don't edit foods here
                    allergies = null, // We pass null because we don't edit allergies here
                    isCompleteCache = cacheStatusBody,
                    image = imagePart,
                    isLocked = isLocked
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()

                        // FIXED: Saves to the correct file
                        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                        sharedPref.edit { putBoolean("isProfileComplete", true) }

                        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(editLayout.windowToken, 0)
                        editLayout.scrollTo(0, 0)

                        headerLayout.findViewById<TextView>(R.id.user_name).text = name
                        headerLayout.findViewById<TextView>(R.id.country_name).text = country
                        headerLayout.findViewById<TextView>(R.id.user_bio).text = bio

                        if (selectedImageUri != null) {
                            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(selectedImageUri)
                        }

//                        flipper.displayedChild = 0
                        switchProfileScreen(0)
                        selectedImageUri = null

                        fetchAndDisplayProfile(requireView(), false)
                    } else {
//                        AlertDialog.Builder(requireContext())
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Error")
                            .setMessage("Failed to save profile. Please try again.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun submitHelpTicket(title: String, description: String) {
        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Please Enter Title and A Description!", Toast.LENGTH_SHORT).show()
            return
        }
//            return Toast.makeText(requireContext(), "Please Enter Title and A Description!", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = KtorBackendApi.HelpTicketRequest(title, description)
                val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                val jwtToken = sharedPref.getString("JWT_TOKEN", "") ?: ""

                val response = RetrofitClient.profileApi.submitHelpTicket("Bearer $jwtToken", request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Ticket Submitted!", Toast.LENGTH_SHORT).show()
                        requireView().findViewById<EditText>(R.id.problem_title).text.clear()
                        requireView().findViewById<EditText>(R.id.problem_description).text.clear()
//                        flipper.displayedChild = 0
                        switchProfileScreen(0)
                    }else {
                        val errorMessage = response.errorBody()?.string()
                        Toast.makeText(requireContext(), "Server Error ${response.code()}: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to send ticket $e", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return tempFile
    }

//    private fun switchProfileScreen(childIndex: Int) {
//        flipper.displayedChild = childIndex
//
//        // Find the MainActivity's navigation views
//        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
//        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.add_button_nav)
//
//        if (childIndex == 0) {
//            // We are on the Main Profile screen. Show the nav bar!
//            bottomNav.visibility = View.VISIBLE
//            fab.show()
//        } else {
//            // We are in Edit, Help, or Privacy. Hide the nav bar!
//            bottomNav.visibility = View.GONE
//            fab.hide()
//        }
//    }

    private fun switchProfileScreen(childIndex: Int) {
        flipper.displayedChild = childIndex

        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.add_button_nav)

        if (childIndex == 0) {
            // Main Profile: Show Nav, Enable Swipe Refresh
            bottomNav.visibility = View.VISIBLE
            fab.show()
            (requireActivity() as? MainActivity)?.setSwipeRefreshEnabled(true)
        } else {
            // Edit/Help/Privacy: Hide Nav, Disable Swipe Refresh
            bottomNav.visibility = View.GONE
            fab.hide()
            (requireActivity() as? MainActivity)?.setSwipeRefreshEnabled(false)
        }
    }

    override fun onRefreshAction() {
        fetchAndDisplayProfile(requireView(), false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog.isDismiss()
        _binding = null
    }
}



//package com.infinitybutterfly.infiflyrecipe
//
//import android.app.AlertDialog
//import android.content.ActivityNotFoundException
//import android.content.Context
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import android.widget.ViewFlipper
//import androidx.activity.OnBackPressedCallback
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.edit
//import androidx.core.net.toUri
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import com.bumptech.glide.Glide
//import com.google.android.material.datepicker.MaterialDatePicker
//import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.RequestBody.Companion.asRequestBody
//import okhttp3.RequestBody.Companion.toRequestBody
//import java.io.File
//import java.io.FileOutputStream
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//class ProfileFragment : Fragment() {
//
//    private lateinit var flipper: ViewFlipper
//    private var selectedImageUri: Uri? = null
//
//    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        if (uri != null) {
//            selectedImageUri = uri
//
//            val headerLayout = requireView().findViewById<View>(R.id.header_layout)
//            val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
//
//            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
//            editLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
//        }
//    }
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return inflater.inflate(R.layout.fragment_profile, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        flipper = view.findViewById(R.id.profile_flipper)
//
//        fetchAndDisplayProfile(view)
//
//        val headerLayout = view.findViewById<View>(R.id.header_layout)
//        val editLayout = view.findViewById<View>(R.id.edit_profile_include)
//
//        headerLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
//            pickImageLauncher.launch("image/*")
//        }
//
//        editLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
//            pickImageLauncher.launch("image/*")
//        }
//
//        view.findViewById<Button>(R.id.edit_profile).setOnClickListener {
//            flipper.displayedChild = 1
//        }
//
//        view.findViewById<Button>(R.id.help_center).setOnClickListener { flipper.displayedChild = 2 }
//        view.findViewById<Button>(R.id.privacy_policy).setOnClickListener { flipper.displayedChild = 3 }
//
//        view.findViewById<Button>(R.id.rate_the_app).setOnClickListener {
//            val appPackageName = requireContext().packageName
//            try {
//                startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri()))
//            } catch (_: ActivityNotFoundException) {
//                startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$appPackageName".toUri()))
//            }
//        }
//
//        view.findViewById<Button>(R.id.logout).setOnClickListener {
//            AlertDialog.Builder(requireContext())
//                .setTitle("Log Out")
//                .setMessage("Are you sure you want to log out?")
//                .setPositiveButton("Yes") { _, _ ->
//                    // FIXED: KTX Warning
//                    requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE).edit { clear() }
//                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
//                }
//                .setNegativeButton("No", null)
//                .show()
//        }
//
//        val dobEditText = editLayout.findViewById<EditText>(R.id.date_of_birth_edit)
//
//        dobEditText.setOnClickListener {
//            val datePicker = MaterialDatePicker.Builder.datePicker()
//                .setTitleText("Select Date of Birth")
//                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
//                .build()
//
//            datePicker.addOnPositiveButtonClickListener { selection ->
//                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                val formattedDate = sdf.format(Date(selection))
//                dobEditText.setText(formattedDate)
//            }
//
//            datePicker.show(parentFragmentManager, "DOB_PICKER")
//        }
//
//        view.findViewById<Button>(R.id.button).setOnClickListener {
//            val updatedName = editLayout.findViewById<EditText>(R.id.chef_name_edit).text.toString()
//            val updatedUsername = editLayout.findViewById<EditText>(R.id.user_name_edit).text.toString()
//            val updatedCountry = editLayout.findViewById<EditText>(R.id.country_name_edit).text.toString()
//            val updatedBio = editLayout.findViewById<EditText>(R.id.user_bio_edit).text.toString()
//            val updatedDob = dobEditText.text.toString()
//
//            // FIXED: Fetch the JWT from cache (or change to however you store your JWT)
//            val sharedPref = requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE)
//            val jwtToken = sharedPref.getString("jwt_token", "") ?: ""
//
//            // FIXED: Passing all 6 parameters successfully
//            saveProfileData(jwtToken, updatedName, updatedUsername, updatedCountry, updatedDob, updatedBio)
//        }
//
//        view.findViewById<Button>(R.id.send_problem).setOnClickListener {
//            val title = view.findViewById<EditText>(R.id.problem_title).text.toString()
//            val desc = view.findViewById<EditText>(R.id.problem_description).text.toString()
//            submitHelpTicket(title, desc)
//        }
//
//        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                if (flipper.displayedChild != 0) {
//                    flipper.displayedChild = 0
//                } else {
//                    isEnabled = false
//                    requireActivity().onBackPressedDispatcher.onBackPressed()
//                }
//            }
//        })
//    }
//
//    private fun fetchAndDisplayProfile(view: View) {
//        val headerLayout = view.findViewById<View>(R.id.header_layout)
//        val editLayout = view.findViewById<View>(R.id.edit_profile_include)
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val sharedPref = requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE)
//                val jwtToken = sharedPref.getString("jwt_token", "") ?: ""
//
//                val response = RetrofitClient.profileApi.getUserProfile("Bearer $jwtToken")
//
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful && response.body() != null) {
//                        val userProfile = response.body()!!
//
//                        headerLayout.findViewById<TextView>(R.id.user_name).text = userProfile.name ?: "Unknown User"
//                        headerLayout.findViewById<TextView>(R.id.country_name).text = userProfile.country ?: "Unknown Country"
//                        headerLayout.findViewById<TextView>(R.id.user_bio).text = userProfile.bio ?: "No bio available."
//
//                        editLayout.findViewById<EditText>(R.id.chef_name_edit).setText(userProfile.name ?: "")
//                        editLayout.findViewById<EditText>(R.id.user_name_edit).setText(userProfile.username ?: "")
//                        editLayout.findViewById<EditText>(R.id.country_name_edit).setText(userProfile.country ?: "")
//                        editLayout.findViewById<EditText>(R.id.user_bio_edit).setText(userProfile.bio ?: "")
//
//                        // FIXED: Email ID reference
//                        editLayout.findViewById<EditText>(R.id.user_email_id).setText(userProfile.emailid ?: "")
//
//                        editLayout.findViewById<EditText>(R.id.date_of_birth_edit).setText(userProfile.dob ?: "")
//
//                        val imageUrl = userProfile.profilePicUrl ?: ""
//
//                        if (imageUrl.isNotEmpty()) {
//                            Glide.with(requireContext()).load(imageUrl).into(headerLayout.findViewById<ImageView>(R.id.user_pic))
//                            Glide.with(requireContext()).load(imageUrl).into(editLayout.findViewById<ImageView>(R.id.user_pic))
//                        }
//                    } else {
//                        Toast.makeText(requireContext(), "Failed to load / No profile data available.", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } catch (_: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "Network error: Could not fetch profile.", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private fun saveProfileData(jwt: String, name: String, username: String, country: String, dob: String, bio: String) {
//        val headerLayout = requireView().findViewById<View>(R.id.header_layout)
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val jwToken = jwt.toRequestBody("text/plain".toMediaTypeOrNull())
//                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
//                val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
//                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
//                val dobBody = dob.toRequestBody("text/plain".toMediaTypeOrNull())
//                val bioBody = bio.toRequestBody("text/plain".toMediaTypeOrNull())
//                val cacheStatusBody = "true".toRequestBody("text/plain".toMediaTypeOrNull())
//
//                var imagePart: MultipartBody.Part? = null
//                if (selectedImageUri != null) {
//                    val imageFile = getFileFromUri(selectedImageUri!!)
//                    val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
//                    imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
//                }
//
//                val response = RetrofitClient.profileApi.updateProfile(
//                    jwToken, nameBody, countryBody, usernameBody, dobBody, bioBody, cacheStatusBody, imagePart
//                )
//
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful) {
//                        Toast.makeText(requireContext(), "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
//
//                        // FIXED: KTX Warning
//                        val sharedPref = requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE)
//                        sharedPref.edit { putBoolean("isProfileComplete", true) }
//
//                        headerLayout.findViewById<TextView>(R.id.user_name).text = name
//                        headerLayout.findViewById<TextView>(R.id.country_name).text = country
//                        headerLayout.findViewById<TextView>(R.id.user_bio).text = bio
//
//                        if (selectedImageUri != null) {
//                            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(selectedImageUri)
//                        }
//
//                        flipper.displayedChild = 0
//                    } else {
//                        AlertDialog.Builder(requireContext())
//                            .setTitle("Error")
//                            .setMessage("Failed to save profile. Please try again.")
//                            .setPositiveButton("OK", null)
//                            .show()
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private fun submitHelpTicket(title: String, description: String) {
//        if (title.isEmpty() || description.isEmpty()) return
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitClient.profileApi.submitHelpTicket(title, description)
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful) {
//                        Toast.makeText(requireContext(), "Ticket Submitted!", Toast.LENGTH_SHORT).show()
//                        requireView().findViewById<EditText>(R.id.problem_title).text.clear()
//                        requireView().findViewById<EditText>(R.id.problem_description).text.clear()
//                        flipper.displayedChild = 0
//                    }
//                }
//            } catch (_: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "Failed to send ticket", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    private fun getFileFromUri(uri: Uri): File {
//        val inputStream = requireContext().contentResolver.openInputStream(uri)
//        val tempFile = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
//        val outputStream = FileOutputStream(tempFile)
//        inputStream?.copyTo(outputStream)
//        inputStream?.close()
//        outputStream.close()
//        return tempFile
//    }
//}
//
//
//
//
////package com.infinitybutterfly.infiflyrecipe
////
////import android.app.AlertDialog
////import android.content.ActivityNotFoundException
////import android.content.Context
////import android.content.Intent
////import android.net.Uri
////import android.os.Bundle
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////import android.widget.Button
////import android.widget.EditText
////import android.widget.ImageView
////import android.widget.TextView
////import android.widget.Toast
////import android.widget.ViewFlipper
////import androidx.activity.OnBackPressedCallback
////import androidx.activity.result.contract.ActivityResultContracts
////import androidx.core.net.toUri
////import androidx.fragment.app.Fragment
////import androidx.lifecycle.lifecycleScope
////import com.bumptech.glide.Glide
////import com.infinitybutterfly.infiflyrecipe.utils.RetrofitInstanceProfile
////import kotlinx.coroutines.Dispatchers
////import kotlinx.coroutines.launch
////import kotlinx.coroutines.withContext
////import okhttp3.MediaType.Companion.toMediaTypeOrNull
////import okhttp3.MultipartBody
////import okhttp3.RequestBody.Companion.asRequestBody
////import okhttp3.RequestBody.Companion.toRequestBody
////import java.io.File
////import java.io.FileOutputStream
////import com.google.android.material.datepicker.MaterialDatePicker
////import java.text.SimpleDateFormat
////import java.util.Date
////import java.util.Locale
////
////class ProfileFragment : Fragment() {
////
////    private lateinit var flipper: ViewFlipper
////    private var selectedImageUri: Uri? = null
////
////    // 1. Image Picker Launcher
////    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
////        if (uri != null) {
////            selectedImageUri = uri
////
////            val headerLayout = requireView().findViewById<View>(R.id.header_layout)
////            val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
////
////            // Update BOTH ImageViews instantly so they always match
////            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
////            editLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
////        }
////    }
////
////    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
////        return inflater.inflate(R.layout.fragment_profile, container, false)
////    }
////
////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////        super.onViewCreated(view, savedInstanceState)
////
////        flipper = view.findViewById(R.id.profile_flipper)
////
////        // Fetch data and display it
////        fetchAndDisplayProfile(view)
////
////        val headerLayout = view.findViewById<View>(R.id.header_layout)
////        val editLayout = view.findViewById<View>(R.id.edit_profile_include)
////
////        //  Click Listeners for Images
////        headerLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
////            pickImageLauncher.launch("image/*")
////        }
////
////        editLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
////            pickImageLauncher.launch("image/*")
////        }
////
////
////        //  BUTTON CLICKS FROM MAIN PROFILE (INDEX 0)
////
////        view.findViewById<Button>(R.id.edit_profile).setOnClickListener {
////            flipper.displayedChild = 1
////        }
////
////        view.findViewById<Button>(R.id.help_center).setOnClickListener { flipper.displayedChild = 2 }
////        view.findViewById<Button>(R.id.privacy_policy).setOnClickListener { flipper.displayedChild = 3 }
////
////        view.findViewById<Button>(R.id.rate_the_app).setOnClickListener {
////            val appPackageName = requireContext().packageName
////            try {
////                startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri()))
////            } catch (_: ActivityNotFoundException) {
////                startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$appPackageName".toUri()))
////            }
////        }
////
////        view.findViewById<Button>(R.id.logout).setOnClickListener {
////            AlertDialog.Builder(requireContext())
////                .setTitle("Log Out")
////                .setMessage("Are you sure you want to log out?")
////                .setPositiveButton("Yes") { _, _ ->
////                    requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE).edit().clear().apply()
////                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
////                }
////                .setNegativeButton("No", null)
////                .show()
////        }
////
////
////        //  BUTTON CLICKS FROM INCLUDED LAYOUTS
////
////        //  CALENDAR DIALOG LOGIC
////        val dobEditText = editLayout.findViewById<EditText>(R.id.date_of_birth_edit)
////
////        dobEditText.setOnClickListener {
////            val datePicker = MaterialDatePicker.Builder.datePicker()
////                .setTitleText("Select Date of Birth")
////                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
////                .build()
////
////            datePicker.addOnPositiveButtonClickListener { selection ->
////                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
////                val formattedDate = sdf.format(Date(selection))
////                dobEditText.setText(formattedDate)
////            }
////
////            datePicker.show(parentFragmentManager, "DOB_PICKER")
////        }
////
////        //  RESTORED: Save Profile Button
////        view.findViewById<Button>(R.id.button).setOnClickListener {
////            val updatedName = editLayout.findViewById<EditText>(R.id.chef_name_edit).text.toString()
////            val updatedUsername = editLayout.findViewById<EditText>(R.id.user_name_edit).text.toString()
////            val updatedCountry = editLayout.findViewById<EditText>(R.id.country_name_edit).text.toString()
////            val updatedBio = editLayout.findViewById<EditText>(R.id.user_bio_edit).text.toString()
////
////            // Read the date directly from our new text box
////            val updatedDob = dobEditText.text.toString()
////
////            saveProfileData(updatedName, updatedUsername, updatedCountry, updatedDob, updatedBio)
////        }
////
////        // Submit Help Center Ticket
////        view.findViewById<Button>(R.id.send_problem).setOnClickListener {
////            val title = view.findViewById<EditText>(R.id.problem_title).text.toString()
////            val desc = view.findViewById<EditText>(R.id.problem_description).text.toString()
////            submitHelpTicket(title, desc)
////        }
////
////        //  HANDLE HARDWARE BACK BUTTON
////        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
////            override fun handleOnBackPressed() {
////                if (flipper.displayedChild != 0) {
////                    flipper.displayedChild = 0
////                } else {
////                    isEnabled = false
////                    requireActivity().onBackPressedDispatcher.onBackPressed()
////                }
////            }
////        })
////    }
////
////    // --- API CALLS ---
////
////    private fun fetchAndDisplayProfile(view: View) {
////        val headerLayout = view.findViewById<View>(R.id.header_layout)
////        val editLayout = view.findViewById<View>(R.id.edit_profile_include)
////
////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////            try {
////                val response = RetrofitInstanceProfile.api.getUserProfile("current_user_email")
////
////                withContext(Dispatchers.Main) {
////                    if (response.isSuccessful && response.body() != null) {
////                        val userProfile = response.body()!!
////
////                        headerLayout.findViewById<TextView>(R.id.user_name).text = userProfile.name ?: "Unknown User"
////                        headerLayout.findViewById<TextView>(R.id.country_name).text = userProfile.country ?: "Unknown Country"
////                        headerLayout.findViewById<TextView>(R.id.user_bio).text = userProfile.bio ?: "No bio available."
////
////                        editLayout.findViewById<EditText>(R.id.chef_name_edit).setText(userProfile.name ?: "")
////                        editLayout.findViewById<EditText>(R.id.user_name_edit).setText(userProfile.username ?: "")
////                        editLayout.findViewById<EditText>(R.id.country_name_edit).setText(userProfile.country ?: "")
////                        editLayout.findViewById<EditText>(R.id.user_bio_edit).setText(userProfile.bio ?: "")
////                        editLayout.findViewById<EditText>(R.id.user_email_id).setText(userProfile.emailid ?: "")
////
////                        // Populate the Date of Birth text box
////                        editLayout.findViewById<EditText>(R.id.date_of_birth_edit).setText(userProfile.dob ?: "")
////
////                        val imageUrl = userProfile.profilePicUrl ?: ""
////
////                        if (imageUrl.isNotEmpty()) {
////                            Glide.with(requireContext()).load(imageUrl).into(headerLayout.findViewById<ImageView>(R.id.user_pic))
////                            Glide.with(requireContext()).load(imageUrl).into(editLayout.findViewById<ImageView>(R.id.user_pic))
////                        }
////                    } else {
////                        Toast.makeText(requireContext(), "Failed to load / No profile data available.", Toast.LENGTH_SHORT).show()
////                    }
////                }
////            } catch (_: Exception) {
////                withContext(Dispatchers.Main) {
////                    Toast.makeText(requireContext(), "Network error: Could not fetch profile.", Toast.LENGTH_LONG).show()
////                }
////            }
////        }
////    }
////
////    private fun saveProfileData(jwt:String, name: String, username: String, country: String, dob: String, bio: String) {
////        val headerLayout = requireView().findViewById<View>(R.id.header_layout)
////
////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////            try {
////                val jwToken = jwt.toRequestBody("text/plain".toMediaTypeOrNull())
////                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
////                val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
////                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
////                val dobBody = dob.toRequestBody("text/plain".toMediaTypeOrNull())
////                val bioBody = bio.toRequestBody("text/plain".toMediaTypeOrNull())
////                val cacheStatusBody = "true".toRequestBody("text/plain".toMediaTypeOrNull())
////
////                var imagePart: MultipartBody.Part? = null
////                if (selectedImageUri != null) {
////                    val imageFile = getFileFromUri(selectedImageUri!!)
////                    val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
////                    imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
////                }
////
////                val response = RetrofitInstanceProfile.api.updateProfile(
////                    jwToken,nameBody, countryBody, usernameBody, dobBody, bioBody, cacheStatusBody, imagePart
////                )
////
////                withContext(Dispatchers.Main) {
////                    if (response.isSuccessful) {
////                        Toast.makeText(requireContext(), "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
////
////                        val sharedPref = requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE)
////                        sharedPref.edit().putBoolean("isProfileComplete", true).apply()
////
////                        headerLayout.findViewById<TextView>(R.id.user_name).text = name
////                        headerLayout.findViewById<TextView>(R.id.country_name).text = country
////                        headerLayout.findViewById<TextView>(R.id.user_bio).text = bio
////
////                        if (selectedImageUri != null) {
////                            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(selectedImageUri)
////                        }
////
////                        flipper.displayedChild = 0
////                    } else {
////                        AlertDialog.Builder(requireContext())
////                            .setTitle("Error")
////                            .setMessage("Failed to save profile. Please try again.")
////                            .setPositiveButton("OK", null)
////                            .show()
////                    }
////                }
////            } catch (e: Exception) {
////                withContext(Dispatchers.Main) {
////                    Toast.makeText(requireContext(), "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
////                }
////            }
////        }
////    }
////
////    private fun submitHelpTicket(title: String, description: String) {
////        if (title.isEmpty() || description.isEmpty()) return
////
////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////            try {
////                val response = RetrofitInstanceProfile.api.submitHelpTicket(title, description)
////                withContext(Dispatchers.Main) {
////                    if (response.isSuccessful) {
////                        Toast.makeText(requireContext(), "Ticket Submitted!", Toast.LENGTH_SHORT).show()
////                        requireView().findViewById<EditText>(R.id.problem_title).text.clear()
////                        requireView().findViewById<EditText>(R.id.problem_description).text.clear()
////                        flipper.displayedChild = 0
////                    }
////                }
////            } catch (_: Exception) {
////                withContext(Dispatchers.Main) {
////                    Toast.makeText(requireContext(), "Failed to send ticket", Toast.LENGTH_SHORT).show()
////                }
////            }
////        }
////    }
////
////    private fun getFileFromUri(uri: Uri): File {
////        val inputStream = requireContext().contentResolver.openInputStream(uri)
////        val tempFile = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
////        val outputStream = FileOutputStream(tempFile)
////        inputStream?.copyTo(outputStream)
////        inputStream?.close()
////        outputStream.close()
////        return tempFile
////    }
////}
////
////
////
//////package com.infinitybutterfly.infiflyrecipe
//////
//////import android.app.AlertDialog
//////import android.content.ActivityNotFoundException
//////import android.content.Context
//////import android.content.Intent
//////import android.net.Uri
//////import android.os.Bundle
//////import android.view.LayoutInflater
//////import android.view.View
//////import android.view.ViewGroup
//////import android.widget.Button
//////import android.widget.EditText
//////import android.widget.ImageView
//////import android.widget.TextView
//////import android.widget.Toast
//////import android.widget.ViewFlipper
//////import androidx.activity.OnBackPressedCallback
//////import androidx.activity.result.contract.ActivityResultContracts
//////import androidx.core.net.toUri
//////import androidx.fragment.app.Fragment
//////import androidx.lifecycle.lifecycleScope
//////import com.bumptech.glide.Glide
//////import com.infinitybutterfly.infiflyrecipe.utils.RetrofitInstanceProfile
//////import kotlinx.coroutines.Dispatchers
//////import kotlinx.coroutines.launch
//////import kotlinx.coroutines.withContext
//////import okhttp3.MediaType.Companion.toMediaTypeOrNull
//////import okhttp3.MultipartBody
//////import okhttp3.RequestBody.Companion.asRequestBody
//////import okhttp3.RequestBody.Companion.toRequestBody
//////import java.io.File
//////import java.io.FileOutputStream
//////import androidx.core.content.edit
//////import com.google.android.material.datepicker.MaterialDatePicker
//////import java.text.SimpleDateFormat
//////import java.util.Date
//////import java.util.Locale
//////
//////class ProfileFragment : Fragment() {
//////
//////    private lateinit var flipper: ViewFlipper
//////    private var selectedImageUri: Uri? = null
//////
//////    // 1. Image Picker Launcher (FIXED: Only handles the result now)
//////    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//////        if (uri != null) {
//////            selectedImageUri = uri
//////
//////            val headerLayout = requireView().findViewById<View>(R.id.header_layout)
//////            val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
//////
//////            // Update BOTH ImageViews instantly so they always match
//////            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
//////            editLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
//////        }
//////    }
//////
//////    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//////        return inflater.inflate(R.layout.fragment_profile, container, false)
//////    }
//////
//////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//////        super.onViewCreated(view, savedInstanceState)
//////
//////        flipper = view.findViewById(R.id.profile_flipper)
//////
//////        // Fetch data and display it
//////        fetchAndDisplayProfile(view)
//////
//////        val headerLayout = view.findViewById<View>(R.id.header_layout)
//////        val editLayout = view.findViewById<View>(R.id.edit_profile_include)
//////
//////        // --- FIXED: Click Listeners moved OUTSIDE of the launcher ---
//////        headerLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
//////            pickImageLauncher.launch("image/*")
//////        }
//////
//////        editLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
//////            pickImageLauncher.launch("image/*")
//////        }
//////
//////
//////        // --- BUTTON CLICKS FROM MAIN PROFILE (INDEX 0) ---
//////
//////        view.findViewById<Button>(R.id.edit_profile).setOnClickListener {
//////            // Copy current text to edit fields
//////            editLayout.findViewById<EditText>(R.id.user_name_edit).setText(headerLayout.findViewById<TextView>(R.id.user_name).text)
//////            editLayout.findViewById<EditText>(R.id.country_name_edit).setText(headerLayout.findViewById<TextView>(R.id.country_name).text)
//////            editLayout.findViewById<EditText>(R.id.user_bio_edit).setText(headerLayout.findViewById<TextView>(R.id.user_bio).text)
//////
//////            flipper.displayedChild = 1 // Show Edit Layout
//////        }
//////
//////        view.findViewById<Button>(R.id.help_center).setOnClickListener { flipper.displayedChild = 2 }
//////        view.findViewById<Button>(R.id.privacy_policy).setOnClickListener { flipper.displayedChild = 3 }
//////
//////        view.findViewById<Button>(R.id.rate_the_app).setOnClickListener {
//////            val appPackageName = requireContext().packageName
//////            try {
//////                startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri()))
//////            } catch (_: ActivityNotFoundException) {
//////                startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$appPackageName".toUri()))
//////            }
//////        }
//////
//////        view.findViewById<Button>(R.id.logout).setOnClickListener {
//////            AlertDialog.Builder(requireContext())
//////                .setTitle("Log Out")
//////                .setMessage("Are you sure you want to log out?")
//////                .setPositiveButton("Yes") { _, _ ->
//////                    requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE).edit { clear() }
//////                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
//////                }
//////                .setNegativeButton("No", null)
//////                .show()
//////        }
//////
//////
//////        // --- BUTTON CLICKS FROM INCLUDED LAYOUTS ---
//////
//////        // --- CALENDAR DIALOG LOGIC ---
//////        val dobEditText = editLayout.findViewById<EditText>(R.id.date_of_birth_edit)
//////
//////        dobEditText.setOnClickListener {
//////            val datePicker = MaterialDatePicker.Builder.datePicker()
//////                .setTitleText("Select Date of Birth")
//////                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
//////                .build()
//////
//////            datePicker.addOnPositiveButtonClickListener { selection ->
//////                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//////                val formattedDate = sdf.format(Date(selection))
//////
//////                dobEditText.setText(formattedDate)
//////            }
//////
//////            datePicker.show(parentFragmentManager, "DOB_PICKER")
//////        }
////////        view.findViewById<Button>(R.id.button).setOnClickListener {
////////            val updatedName = editLayout.findViewById<EditText>(R.id.chef_name_edit).text.toString()
////////            val updatedUsername = editLayout.findViewById<EditText>(R.id.user_name_edit).text.toString()
////////            val updatedCountry = editLayout.findViewById<EditText>(R.id.country_name_edit).text.toString()
////////            val updatedBio = editLayout.findViewById<EditText>(R.id.user_bio_edit).text.toString()
////////
////////            val datePicker = editLayout.findViewById<android.widget.DatePicker>(R.id.datePicker_dob)
////////            val dobYear = datePicker.year
////////            val dobMonth = datePicker.month + 1
////////            val dobDay = datePicker.dayOfMonth
////////            val updatedDob = "$dobYear-$dobMonth-$dobDay"
////////
////////            saveProfileData(updatedName, updatedUsername, updatedCountry, updatedDob, updatedBio)
////////        }
//////
//////        view.findViewById<Button>(R.id.send_problem).setOnClickListener {
//////            val title = view.findViewById<EditText>(R.id.problem_title).text.toString()
//////            val desc = view.findViewById<EditText>(R.id.problem_description).text.toString()
//////            submitHelpTicket(title, desc)
//////        }
//////
//////        // --- FIXED: HANDLE HARDWARE BACK BUTTON ---
//////        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
//////            override fun handleOnBackPressed() {
//////                if (flipper.displayedChild != 0) {
//////                    flipper.displayedChild = 0
//////                } else {
//////                    isEnabled = false
//////                    requireActivity().onBackPressedDispatcher.onBackPressed()
//////                }
//////            }
//////        })
//////    }
//////
//////    // --- API CALLS ---
//////
//////    private fun fetchAndDisplayProfile(view: View) {
//////        val headerLayout = view.findViewById<View>(R.id.header_layout)
//////        val editLayout = view.findViewById<View>(R.id.edit_profile_include)
//////
//////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//////            try {
//////                val response = RetrofitInstanceProfile.api.getUserProfile("current_user_id")
//////
//////                withContext(Dispatchers.Main) {
//////                    if (response.isSuccessful && response.body() != null) {
//////                        val userProfile = response.body()!!
//////
//////                        headerLayout.findViewById<TextView>(R.id.user_name).text = userProfile.name ?: "Unknown User"
//////                        headerLayout.findViewById<TextView>(R.id.country_name).text = userProfile.country ?: "Unknown Country"
//////                        headerLayout.findViewById<TextView>(R.id.user_bio).text = userProfile.bio ?: "No bio available."
//////
//////                        editLayout.findViewById<EditText>(R.id.user_name_edit).setText(userProfile.username ?: "")
//////                        editLayout.findViewById<EditText>(R.id.user_number).setText(userProfile.phone ?: "")
//////
////////                        DOB Logic
//////
//////                        val updatedDob = editLayout.findViewById<EditText>(R.id.date_of_birth_edit).text.toString()
//////
//////                        editLayout.findViewById<EditText>(R.id.date_of_birth_edit).setText(userProfile.dob ?: "")
//////
////////                        val dobString = userProfile.dob
////////                        if (!dobString.isNullOrEmpty() && dobString.contains("-")) {
////////                            try {
////////                                val parts = dobString.split("-")
////////                                val year = parts[0].toInt()
////////                                val month = parts[1].toInt() - 1
////////                                val day = parts[2].toInt()
////////
////////                                editLayout.findViewById<android.widget.DatePicker>(R.id.u_dob)
////////                                    .updateDate(year, month, day)
////////                            } catch (e: Exception) {
////////                                e.printStackTrace()
////////                            }
////////                        }
//////
//////                        val imageUrl = userProfile.profilePicUrl ?: ""
//////
//////                        if (imageUrl.isNotEmpty()) {
//////                            Glide.with(requireContext()).load(imageUrl).into(headerLayout.findViewById<ImageView>(R.id.user_pic))
//////                            Glide.with(requireContext()).load(imageUrl).into(editLayout.findViewById<ImageView>(R.id.user_pic))
//////                        }
//////                    } else {
//////                        Toast.makeText(requireContext(), "Failed to load profile data.", Toast.LENGTH_SHORT).show()
//////                    }
//////                }
//////            } catch (_: Exception) {
//////                withContext(Dispatchers.Main) {
//////                    Toast.makeText(requireContext(), "Network error: Could not fetch profile.", Toast.LENGTH_LONG).show()
//////                }
//////            }
//////        }
//////    }
//////
//////    private fun saveProfileData(name: String, username: String, country: String, dob: String, bio: String) {
//////        val headerLayout = requireView().findViewById<View>(R.id.header_layout)
//////
//////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//////            try {
//////                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
//////                val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
//////                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
//////                val dobBody = dob.toRequestBody("text/plain".toMediaTypeOrNull())
//////                val bioBody = bio.toRequestBody("text/plain".toMediaTypeOrNull())
//////                val cacheStatusBody = "true".toRequestBody("text/plain".toMediaTypeOrNull())
//////
//////                var imagePart: MultipartBody.Part? = null
//////                if (selectedImageUri != null) {
//////                    val imageFile = getFileFromUri(selectedImageUri!!)
//////                    val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
//////                    imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
//////                }
//////
//////                val response = RetrofitInstanceProfile.api.updateProfile(
//////                    nameBody, usernameBody, countryBody, dobBody, bioBody, cacheStatusBody, imagePart
//////                )
//////
//////                withContext(Dispatchers.Main) {
//////                    if (response.isSuccessful) {
//////                        Toast.makeText(requireContext(), "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
//////
//////                        val sharedPref = requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE)
//////                        sharedPref.edit { putBoolean("isProfileComplete", true) }
//////
//////                        headerLayout.findViewById<TextView>(R.id.user_name).text = name
//////                        headerLayout.findViewById<TextView>(R.id.country_name).text = country
//////                        headerLayout.findViewById<TextView>(R.id.user_bio).text = bio
//////
//////                        if (selectedImageUri != null) {
//////                            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(selectedImageUri)
//////                        }
//////
//////                        flipper.displayedChild = 0
//////                    } else {
//////                        AlertDialog.Builder(requireContext())
//////                            .setTitle("Error")
//////                            .setMessage("Failed to save profile. Please try again.")
//////                            .setPositiveButton("OK", null)
//////                            .show()
//////                    }
//////                }
//////            } catch (e: Exception) {
//////                withContext(Dispatchers.Main) {
//////                    Toast.makeText(requireContext(), "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
//////                }
//////            }
//////        }
//////    }
//////
//////    private fun submitHelpTicket(title: String, description: String) {
//////        if (title.isEmpty() || description.isEmpty()) return
//////
//////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//////            try {
//////                val response = RetrofitInstanceProfile.api.submitHelpTicket(title, description)
//////                withContext(Dispatchers.Main) {
//////                    if (response.isSuccessful) {
//////                        Toast.makeText(requireContext(), "Ticket Submitted!", Toast.LENGTH_SHORT).show()
//////                        requireView().findViewById<EditText>(R.id.problem_title).text.clear()
//////                        requireView().findViewById<EditText>(R.id.problem_description).text.clear()
//////                        flipper.displayedChild = 0
//////                    }
//////                }
//////            } catch (_: Exception) {
//////                withContext(Dispatchers.Main) {
//////                    Toast.makeText(requireContext(), "Failed to send ticket", Toast.LENGTH_SHORT).show()
//////                }
//////            }
//////        }
//////    }
//////
//////    private fun getFileFromUri(uri: Uri): File {
//////        val inputStream = requireContext().contentResolver.openInputStream(uri)
//////        val tempFile = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
//////        val outputStream = FileOutputStream(tempFile)
//////        inputStream?.copyTo(outputStream)
//////        inputStream?.close()
//////        outputStream.close()
//////        return tempFile
//////    }
//////}
//////
//////
//////
////////package com.infinitybutterfly.infiflyrecipe
////////
////////import android.app.AlertDialog
////////import android.content.ActivityNotFoundException
////////import android.content.Context
////////import android.content.Intent
////////import android.net.Uri
////////import android.os.Bundle
////////import android.view.LayoutInflater
////////import android.view.View
////////import android.view.ViewGroup
////////import android.widget.Button
////////import android.widget.EditText
////////import android.widget.ImageView
////////import android.widget.TextView
////////import android.widget.Toast
////////import android.widget.ViewFlipper
////////import androidx.activity.OnBackPressedCallback
////////import androidx.activity.result.contract.ActivityResultContracts
////////import androidx.fragment.app.Fragment
////////import androidx.lifecycle.lifecycleScope
////////import com.bumptech.glide.Glide // Used for loading API images
////////import com.infinitybutterfly.infiflyrecipe.utils.RetrofitInstanceProfile
////////import kotlinx.coroutines.Dispatchers
////////import kotlinx.coroutines.launch
////////import kotlinx.coroutines.withContext
////////import okhttp3.MediaType.Companion.toMediaTypeOrNull
////////import okhttp3.MultipartBody
////////import okhttp3.RequestBody.Companion.asRequestBody
////////import okhttp3.RequestBody.Companion.toRequestBody
////////import java.io.File
////////import java.io.FileOutputStream
////////import androidx.core.content.edit
////////import androidx.core.net.toUri
////////
////////class ProfileFragment : Fragment() {
////////
////////    private lateinit var flipper: ViewFlipper
////////    private var selectedImageUri: Uri? = null
////////
////////    // 1. Image Picker Launcher
//////////    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//////////        if (uri != null) {
//////////            selectedImageUri = uri
//////////
//////////            // Instantly show the selected image in the EDIT layout
//////////            val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
//////////            editLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
//////////        }
//////////    }
////////
////////    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
////////        if (uri != null) {
////////            selectedImageUri = uri
////////
//////////            // Grab both layouts
//////////            val headerLayout = requireView().findViewById<View>(R.id.header_layout)
//////////            val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
//////////
//////////            // Update BOTH ImageViews instantly so they always match!
//////////            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
//////////            editLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(uri)
////////
////////            // --- BUTTON CLICKS FROM INCLUDED LAYOUTS ---
////////
////////            val headerLayout = requireView().findViewById<View>(R.id.header_layout)
////////            val editLayout = requireView().findViewById<View>(R.id.edit_profile_include)
////////
////////            // Launch Image Picker from the Header Layout
////////            headerLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
////////                pickImageLauncher.launch("image/*")
////////            }
////////
////////            // Launch Image Picker from the Edit Layout (in case they do it from there instead)
////////            editLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
////////                pickImageLauncher.launch("image/*")
////////            }
////////        }
////////    }
////////
////////    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
////////        return inflater.inflate(R.layout.fragment_profile, container, false)
////////    }
////////
////////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////////        super.onViewCreated(view, savedInstanceState)
////////
////////        flipper = view.findViewById(R.id.profile_flipper)
////////
////////        // Fetch data and display it
////////        fetchAndDisplayProfile(view)
////////
////////        // --- SCOPED VIEWS (Handling duplicate IDs in your XML) ---
////////        val headerLayout = view.findViewById<View>(R.id.header_layout)
////////        val editLayout = view.findViewById<View>(R.id.edit_profile_include)
////////
////////        // --- BUTTON CLICKS FROM MAIN PROFILE (INDEX 0) ---
////////
////////        // Open Edit Profile Layout
////////        view.findViewById<Button>(R.id.edit_profile).setOnClickListener {
////////            // Copy current text to edit fields
////////            editLayout.findViewById<EditText>(R.id.user_name_edit).setText(headerLayout.findViewById<TextView>(R.id.user_name).text)
////////            editLayout.findViewById<EditText>(R.id.country_name_edit).setText(headerLayout.findViewById<TextView>(R.id.country_name).text)
////////            editLayout.findViewById<EditText>(R.id.user_bio_edit).setText(headerLayout.findViewById<TextView>(R.id.user_bio).text)
////////
////////            flipper.displayedChild = 1 // Show Edit Layout
////////        }
////////
////////        view.findViewById<Button>(R.id.help_center).setOnClickListener { flipper.displayedChild = 2 }
////////        view.findViewById<Button>(R.id.privacy_policy).setOnClickListener { flipper.displayedChild = 3 }
////////
////////        view.findViewById<Button>(R.id.rate_the_app).setOnClickListener {
////////            val appPackageName = requireContext().packageName
////////            try {
////////                startActivity(Intent(Intent.ACTION_VIEW,
////////                    "market://details?id=$appPackageName".toUri()))
////////            } catch (e: ActivityNotFoundException) {
////////                startActivity(Intent(Intent.ACTION_VIEW,
////////                    "https://play.google.com/store/apps/details?id=$appPackageName".toUri()))
////////            }
////////        }
////////
////////        view.findViewById<Button>(R.id.logout).setOnClickListener {
////////            AlertDialog.Builder(requireContext())
////////                .setTitle("Log Out")
////////                .setMessage("Are you sure you want to log out?")
////////                .setPositiveButton("Yes") { _, _ ->
////////                    requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE).edit { clear() }
////////                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
////////                }
////////                .setNegativeButton("No", null)
////////                .show()
////////        }
////////
////////
////////        // --- BUTTON CLICKS FROM INCLUDED LAYOUTS ---
////////
////////        // Launch Image Picker when tapping the profile pic inside the EDIT layout
////////        editLayout.findViewById<ImageView>(R.id.user_pic).setOnClickListener {
////////            pickImageLauncher.launch("image/*")
////////        }
////////
////////        // Save Profile Button
////////        view.findViewById<Button>(R.id.button).setOnClickListener {
////////            // Get text fields
////////            val updatedName = editLayout.findViewById<EditText>(R.id.chef_name_edit).text.toString()
////////            val updatedUsername = editLayout.findViewById<EditText>(R.id.user_name_edit).text.toString()
////////            val updatedCountry = editLayout.findViewById<EditText>(R.id.country_name_edit).text.toString()
////////            val updatedBio = editLayout.findViewById<EditText>(R.id.user_bio_edit).text.toString()
////////
////////            // Get DatePicker values and format as "YYYY-MM-DD"
////////            val datePicker = editLayout.findViewById<android.widget.DatePicker>(R.id.datePicker_dob)
////////            val dobYear = datePicker.year
////////            val dobMonth = datePicker.month + 1 // Add 1 because Android months start at 0
////////            val dobDay = datePicker.dayOfMonth
////////            val updatedDob = "$dobYear-$dobMonth-$dobDay"
////////
////////            // Pass everything to your save function
////////            saveProfileData(updatedName, updatedUsername, updatedCountry, updatedDob, updatedBio)
////////        }
//////////        view.findViewById<Button>(R.id.button).setOnClickListener {
//////////            val updatedName = editLayout.findViewById<EditText>(R.id.user_name_edit).text.toString()
//////////            val updatedCountry = editLayout.findViewById<EditText>(R.id.country_name_edit).text.toString()
//////////            val updatedBio = editLayout.findViewById<EditText>(R.id.user_bio_edit).text.toString()
//////////
//////////            saveProfileData(updatedName, updatedCountry, updatedBio)
//////////        }
////////
////////        // Submit Help Center Ticket
////////        view.findViewById<Button>(R.id.send_problem).setOnClickListener {
////////            val title = view.findViewById<EditText>(R.id.problem_title).text.toString()
////////            val desc = view.findViewById<EditText>(R.id.problem_description).text.toString()
////////            submitHelpTicket(title, desc)
////////        }
////////
////////        // --- HANDLE HARDWARE BACK BUTTON ---
////////        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
////////            override fun handleOnBackPressed() {
////////                if (flipper.displayedChild != 0) {
////////                    flipper.displayedChild = 0
////////                } else {
////////                    isEnabled = false
////////                    requireActivity().onBackPressed()
////////                }
////////            }
////////        })
////////    }
////////
////////    // --- API CALLS ---
////////
////////// If an actual image is being sent use the below code else use the other one
//////////    import android.graphics.BitmapFactory // Add this to your imports at the top
//////////    import android.util.Base64 // Add this to your imports at the top
//////////
//////////// ... inside fetchAndDisplayProfile() ...
//////////
//////////    withContext(Dispatchers.Main) {
//////////        if (response.isSuccessful && response.body() != null) {
//////////            val userProfile = response.body()!!
//////////
//////////            // Update Texts
//////////            headerLayout.findViewById<TextView>(R.id.user_name).text = userProfile.name ?: "Unknown User"
//////////            headerLayout.findViewById<TextView>(R.id.country_name).text = userProfile.country ?: "Unknown Country"
//////////            headerLayout.findViewById<TextView>(R.id.user_bio).text = userProfile.bio ?: "No bio available."
//////////
//////////            // --- NEW BASE64 DECODING LOGIC ---
//////////            val base64String = userProfile.profilePicBase64
//////////
//////////            if (!base64String.isNullOrEmpty()) {
//////////                try {
//////////                    // 1. Sometimes servers leave the "data:image/jpeg;base64," header attached.
//////////                    // We must strip it out before decoding, or the app will crash.
//////////                    val cleanBase64 = if (base64String.contains(",")) {
//////////                        base64String.substringAfter(",")
//////////                    } else {
//////////                        base64String
//////////                    }
//////////
//////////                    // 2. Decode the text string into a ByteArray
//////////                    val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
//////////
//////////                    // 3. Convert the ByteArray into an Android Bitmap
//////////                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//////////
//////////                    // 4. Apply the Bitmap directly to the ImageViews (No Glide needed!)
//////////                    headerLayout.findViewById<ImageView>(R.id.user_pic).setImageBitmap(decodedImage)
//////////                    editLayout.findViewById<ImageView>(R.id.user_pic).setImageBitmap(decodedImage)
//////////
//////////                } catch (e: Exception) {
//////////                    // If the Base64 string is corrupted, this prevents the app from crashing
//////////                    e.printStackTrace()
//////////                }
//////////            }
//////////        } else {
//////////            Toast.makeText(requireContext(), "Failed to load profile data.", Toast.LENGTH_SHORT).show()
//////////        }
//////////    }
////////
////////
////////    private fun fetchAndDisplayProfile(view: View) {
////////        val headerLayout = view.findViewById<View>(R.id.header_layout)
////////        val editLayout = view.findViewById<View>(R.id.edit_profile_include)
////////
////////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////////            try {
////////                // Note: Make sure your ApiInterface expects Response<UserProfileResponse>
////////                val response = RetrofitInstanceProfile.api.getUserProfile("current_user_id")
////////
////////                withContext(Dispatchers.Main) {
////////                    if (response.isSuccessful && response.body() != null) {
////////
////////                        // 1. Save the API data into a variable
////////                        val userProfile = response.body()!!
////////
////////                        // 2. Update Texts with the actual API fields!
////////                        // We use the ?: operator to provide a fallback in case the API returns null
////////                        headerLayout.findViewById<TextView>(R.id.user_name).text = userProfile.name ?: "Unknown User"
////////                        headerLayout.findViewById<TextView>(R.id.country_name).text = userProfile.country ?: "Unknown Country"
////////                        headerLayout.findViewById<TextView>(R.id.user_bio).text = userProfile.bio ?: "No bio available."
////////
////////                        editLayout.findViewById<EditText>(R.id.user_name_edit).setText(userProfile.username ?: "")
////////                        editLayout.findViewById<EditText>(R.id.user_number).setText(userProfile.phone ?: "")
////////
////////                        val dobString = userProfile.dob
////////                        if (!dobString.isNullOrEmpty() && dobString.contains("-")) {
////////                            try {
////////                                val parts = dobString.split("-")
////////                                val year = parts[0].toInt()
////////                                val month = parts[1].toInt() - 1 // Android months are 0-indexed (Jan = 0)
////////                                val day = parts[2].toInt()
////////
////////                                editLayout.findViewById<android.widget.DatePicker>(R.id.datePicker_dob)
////////                                    .updateDate(year, month, day)
////////                            } catch (e: Exception) {
////////                                e.printStackTrace() // Failsafe if the database date is formatted weirdly
////////                            }
////////                        }
////////
////////                        // 3. Grab the actual image URL from the API response
////////                        val imageUrl = userProfile.profilePicUrl ?: ""
////////
////////                        // 4. Use Glide to load the web image into BOTH screens
////////                        if (imageUrl.isNotEmpty()) {
////////                            Glide.with(requireContext()).load(imageUrl).into(headerLayout.findViewById<ImageView>(R.id.user_pic))
////////                            Glide.with(requireContext()).load(imageUrl).into(editLayout.findViewById<ImageView>(R.id.user_pic))
////////                        }
////////                    } else {
////////                        // Handle the case where the server responds, but with an error code (like 404)
////////                        Toast.makeText(requireContext(), "Failed to load profile data.", Toast.LENGTH_SHORT).show()
////////                    }
////////                }
////////            } catch (e: Exception) {
////////                // IMPORTANT: The catch block runs on the background IO thread!
////////                // We MUST switch back to the Main thread to show a Toast.
////////                withContext(Dispatchers.Main) {
////////                    Toast.makeText(requireContext(), "Network error: Could not fetch profile.", Toast.LENGTH_LONG).show()
////////                }
////////            }
////////        }
////////    }
////////
//////////    private fun fetchAndDisplayProfile(view: View) {
//////////        val headerLayout = view.findViewById<View>(R.id.header_layout)
//////////        val editLayout = view.findViewById<View>(R.id.edit_profile_include)
//////////
//////////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//////////            try {
//////////                val response = RetrofitInstanceProfile.api.getUserProfile("current_user_id")
//////////                withContext(Dispatchers.Main) {
//////////                    if (response.isSuccessful && response.body() != null) {
//////////                        // Update Texts
//////////                        headerLayout.findViewById<TextView>(R.id.user_name).text = "API Name"
//////////                        headerLayout.findViewById<TextView>(R.id.country_name).text = "API Country"
//////////                        headerLayout.findViewById<TextView>(R.id.user_bio).text = "API Bio"
//////////
//////////                        // Assuming your API returns an image URL string
//////////                        val imageUrl = "https://example.com/api_image.jpg" // Replace with actual API field
//////////
//////////                        // Use Glide to load the web image into BOTH the header and edit screen
//////////                        if (imageUrl.isNotEmpty()) {
//////////                            Glide.with(requireContext()).load(imageUrl).into(headerLayout.findViewById<ImageView>(R.id.user_pic))
//////////                            Glide.with(requireContext()).load(imageUrl).into(editLayout.findViewById<ImageView>(R.id.user_pic))
//////////                        }
//////////                    }
//////////                }
//////////            } catch (e: Exception) {
//////////                // Handle Error
//////////            }
//////////        }
//////////    }
////////
////////    private fun saveProfileData(name: String, country: String, bio: String, dob: String, username: String) {
////////        val headerLayout = requireView().findViewById<View>(R.id.header_layout)
////////
////////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////////            try {
////////                // Text Parts
////////                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
////////                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
////////                val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
////////                val dobBody = dob.toRequestBody("text/plain".toMediaTypeOrNull())
////////                val bioBody = bio.toRequestBody("text/plain".toMediaTypeOrNull())
////////                val cacheStatusBody = "true".toRequestBody("text/plain".toMediaTypeOrNull())
////////
////////                // Image Part (Optional, in case they just change text and don't pick a new photo)
////////                var imagePart: MultipartBody.Part? = null
////////                if (selectedImageUri != null) {
////////                    val imageFile = getFileFromUri(selectedImageUri!!)
////////                    val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
////////                    imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
////////                }
////////
//////////                val response = RetrofitInstanceProfile.api.updateProfile(nameBody, countryBody, bioBody, cacheStatusBody, imagePart)
////////
////////                val response = RetrofitInstanceProfile.api.updateProfile(
////////                    nameBody,  countryBody, usernameBody, dobBody, bioBody, cacheStatusBody, imagePart
////////                )
////////
////////                withContext(Dispatchers.Main) {
////////                    if (response.isSuccessful) {
////////                        Toast.makeText(requireContext(), "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
////////
////////                        // Save local cache for AddRecipeFragment
////////                        val sharedPref = requireActivity().getSharedPreferences("UserCache", Context.MODE_PRIVATE)
////////                        sharedPref.edit().putBoolean("isProfileComplete", true).apply()
////////
////////                        // Update main textviews with new data
////////                        headerLayout.findViewById<TextView>(R.id.user_name).text = name
////////                        headerLayout.findViewById<TextView>(R.id.country_name).text = country
////////                        headerLayout.findViewById<TextView>(R.id.user_bio).text = bio
////////
////////                        // If they picked a new image, apply it to the main header layout immediately
////////                        if (selectedImageUri != null) {
////////                            headerLayout.findViewById<ImageView>(R.id.user_pic).setImageURI(selectedImageUri)
////////                        }
////////
////////                        flipper.displayedChild = 0
////////                    } else {
////////                        AlertDialog.Builder(requireContext())
////////                            .setTitle("Error")
////////                            .setMessage("Failed to save profile. Please try again.")
////////                            .setPositiveButton("OK", null)
////////                            .show()
////////                    }
////////                }
////////            } catch (e: Exception) {
////////                withContext(Dispatchers.Main) {
////////                    Toast.makeText(requireContext(), "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
////////                }
////////            }
////////        }
////////    }
////////
////////    private fun submitHelpTicket(title: String, description: String) {
////////        if (title.isEmpty() || description.isEmpty()) return
////////
////////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////////            try {
////////                val response = RetrofitInstanceProfile.api.submitHelpTicket(title, description)
////////                withContext(Dispatchers.Main) {
////////                    if (response.isSuccessful) {
////////                        Toast.makeText(requireContext(), "Ticket Submitted!", Toast.LENGTH_SHORT).show()
////////                        requireView().findViewById<EditText>(R.id.problem_title).text.clear()
////////                        requireView().findViewById<EditText>(R.id.problem_description).text.clear()
////////                        flipper.displayedChild = 0
////////                    }
////////                }
////////            } catch (e: Exception) {
////////                withContext(Dispatchers.Main) {
////////                    Toast.makeText(requireContext(), "Failed to send ticket", Toast.LENGTH_SHORT).show()
////////                }
////////            }
////////        }
////////    }
////////
////////    // Helper function to turn URI into File for Retrofit
////////    private fun getFileFromUri(uri: Uri): File {
////////        val inputStream = requireContext().contentResolver.openInputStream(uri)
////////        val tempFile = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
////////        val outputStream = FileOutputStream(tempFile)
////////        inputStream?.copyTo(outputStream)
////////        inputStream?.close()
////////        outputStream.close()
////////        return tempFile
////////    }
////////}
////////
////////
////////
////////
////////
////////
//////////package com.infinitybutterfly.infiflyrecipe
//////////
//////////import android.os.Bundle
//////////import androidx.fragment.app.Fragment
//////////import android.view.LayoutInflater
//////////import android.view.View
//////////import android.view.ViewGroup
//////////import android.widget.Button
//////////import android.widget.ImageView
//////////import android.widget.TextView
//////////import android.widget.Toast
//////////import androidx.activity.OnBackPressedCallback
//////////import androidx.navigation.NavOptions
//////////import androidx.navigation.fragment.findNavController
//////////import com.google.android.material.bottomnavigation.BottomNavigationView
//////////import com.infinitybutterfly.infiflyrecipe.databinding.FragmentProfileBinding
//////////import com.infinitybutterfly.infiflyrecipe.databinding.FragmentRecipeDetailBinding
//////////import android.app.Dialog
//////////import android.content.ActivityNotFoundException
//////////import android.content.Intent
//////////import android.net.Uri
//////////import androidx.core.content.ContentProviderCompat
//////////import com.infinitybutterfly.infiflyrecipe.databinding.FragmentLoginCreateAccountBinding
//////////import kotlin.jvm.java
//////////
//////////class ProfileFragment : Fragment() {
//////////
//////////    private var _binding: FragmentProfileBinding? = null
//////////    private val binding get() = _binding!!
//////////
//////////    // 1. Initialize Views from the Main Layout
//////////
//////////    val btnHelpCenter = view?.findViewById<Button>(R.id.help_center)
//////////    val btnRateApp = view?.findViewById<Button>(R.id.rate_the_app)
//////////    val btnPrivacyPolicy = view?.findViewById<Button>(R.id.privacy_policy)
//////////    val btnLogout = view?.findViewById<Button>(R.id.logout)
//////////
//////////    // 2. Initialize Views from Included Header
//////////
//////////    val imgUserPic = view?.findViewById<ImageView>(R.id.user_pic)
//////////    val tvUserName = view?.findViewById<TextView>(R.id.user_name)
//////////    val tvCountryName = view?.findViewById<TextView>(R.id.country_name)
//////////    val tvUserBio = view?.findViewById<TextView>(R.id.user_bio)
//////////    var btnEditProfile = view?.findViewById<Button>(R.id.edit_profile)
//////////
//////////    // 3. Initialize Views from Edit Profile Layout
//////////
//////////
//////////    override fun onCreateView(
//////////        inflater: LayoutInflater, container: ViewGroup?,
//////////        savedInstanceState: Bundle?
//////////    ): View {
////////////        return inflater.inflate(R.layout.fragment_profile, container, false)
//////////        _binding = FragmentProfileBinding.inflate(inflater, container, false)
//////////        return binding.root
//////////    }
//////////
//////////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//////////        super.onViewCreated(view, savedInstanceState)
//////////
//////////            // 4. Setup Click Listeners
//////////
//////////            btnEditProfile?.setOnClickListener {
//////////                val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
//////////
//////////                dialog.setContentView(R.layout.edit_profile_layout)
//////////
//////////                dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//////////
//////////                dialog.window?.attributes?.windowAnimations = R.style.SlideDialogAnimation
//////////
//////////                val saveButton = dialog.findViewById<Button>(R.id.button)
//////////                saveButton?.setOnClickListener {
//////////                    // Save and Send data here
//////////                    dialog.dismiss()
//////////                }
//////////
//////////                dialog.show()
//////////            }
//////////
//////////            btnHelpCenter?.setOnClickListener {
//////////                val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
//////////
//////////                dialog.setContentView(R.layout.help_center_layout)
//////////
//////////                dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//////////
//////////                dialog.window?.attributes?.windowAnimations = R.style.SlideDialogAnimation
//////////
//////////                val saveButton = dialog.findViewById<Button>(R.id.send_problem)
//////////                saveButton?.setOnClickListener {
//////////                    dialog.dismiss()
//////////                }
//////////
//////////                dialog.show()
//////////            }
//////////
//////////
//////////        btnRateApp?.setOnClickListener {
//////////            // Opens the Google Play Store to your app's page.
//////////            val appPackageName = requireContext().packageName
//////////            try {
//////////                // Tries to open the Play Store app directly
//////////                startActivity(
//////////                    Intent(
//////////                        Intent.ACTION_VIEW,
//////////                        Uri.parse("market://details?id=$appPackageName")
//////////                    )
//////////                )
//////////            } catch (e: ActivityNotFoundException) {
//////////                // Falls back to the web browser if the Play Store app isn't installed
//////////                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
//////////            }
//////////        }
//////////
//////////        btnPrivacyPolicy?.setOnClickListener {
//////////            // Opens your privacy policy in the device's default web browser
//////////            val privacyPolicyUrl = "https://www.yourwebsite.com/privacy-policy" // Replace with your actual URL
//////////            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
//////////            startActivity(browserIntent)
//////////        }
//////////
//////////
//////////        btnLogout?.setOnClickListener {
//////////            // 1. Clear the user session (Example using Firebase)
//////////            // FirebaseAuth.getInstance().signOut()
//////////
//////////            // 2. Clear local SharedPreferences if you store user data locally
//////////            // val prefs = requireContext().getSharedPreferences("MyPrefs", android.content.Context.MODE_PRIVATE)
//////////            // prefs.edit().clear().apply()
//////////
//////////            // 3. Navigate back to LoginActivity and clear the backstack
//////////            val intent = Intent(requireContext(), FragmentLoginCreateAccountBinding::class.java) // Ensure you have a LoginActivity
//////////            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//////////            startActivity(intent)
//////////        }
//////////
//////////
//////////
//////////        requireActivity().onBackPressedDispatcher.addCallback(
//////////            viewLifecycleOwner,
//////////            object : OnBackPressedCallback(true) {
//////////                override fun handleOnBackPressed() {
//////////                    val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
//////////                    bottomNav.selectedItemId = R.id.homeFragment
//////////                }
//////////            }
//////////        )
//////////        }
//////////}
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
//////////
////////////package com.infinitybutterfly.infiflyrecipe
////////////
////////////import android.os.Bundle
////////////import androidx.fragment.app.Fragment
////////////import android.view.LayoutInflater
////////////import android.view.View
////////////import android.view.ViewGroup
////////////
////////////// TODO: Rename parameter arguments, choose names that match
////////////// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
////////////private const val ARG_PARAM1 = "param1"
////////////private const val ARG_PARAM2 = "param2"
////////////
/////////////**
//////////// * A simple [Fragment] subclass.
//////////// * Use the [ProfileFragment.newInstance] factory method to
//////////// * create an instance of this fragment.
//////////// */
////////////class ProfileFragment : Fragment() {
////////////    // TODO: Rename and change types of parameters
////////////    private var param1: String? = null
////////////    private var param2: String? = null
////////////
////////////    override fun onCreate(savedInstanceState: Bundle?) {
////////////        super.onCreate(savedInstanceState)
////////////        arguments?.let {
////////////            param1 = it.getString(ARG_PARAM1)
////////////            param2 = it.getString(ARG_PARAM2)
////////////        }
////////////    }
////////////
////////////    override fun onCreateView(
////////////        inflater: LayoutInflater, container: ViewGroup?,
////////////        savedInstanceState: Bundle?
////////////    ): View? {
////////////        // Inflate the layout for this fragment
////////////        return inflater.inflate(R.layout.fragment_profile, container, false)
////////////    }
////////////
////////////    companion object {
////////////        /**
////////////         * Use this factory method to create a new instance of
////////////         * this fragment using the provided parameters.
////////////         *
////////////         * @param param1 Parameter 1.
////////////         * @param param2 Parameter 2.
////////////         * @return A new instance of fragment ProfileFragment.
////////////         */
////////////        // TODO: Rename and change types and number of parameters
////////////        @JvmStatic
////////////        fun newInstance(param1: String, param2: String) =
////////////            ProfileFragment().apply {
////////////                arguments = Bundle().apply {
////////////                    putString(ARG_PARAM1, param1)
////////////                    putString(ARG_PARAM2, param2)
////////////                }
////////////            }
////////////    }
////////////}