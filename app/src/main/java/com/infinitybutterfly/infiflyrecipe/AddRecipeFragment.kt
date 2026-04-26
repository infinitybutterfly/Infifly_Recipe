package com.infinitybutterfly.infiflyrecipe

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infinitybutterfly.infiflyrecipe.databinding.FragmentAddRecipeBinding
import com.infinitybutterfly.infiflyrecipe.utils.LoadingDialog
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

class AddRecipeFragment : Fragment() {
    private var _binding: FragmentAddRecipeBinding? = null
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog
    private var selectedImageUri: Uri? = null
    private var tempCameraUri: Uri? = null
    private var originalImageUrl: String = ""
    private var editingRecipeId: Int = -1

    // 1. GALLERY LAUNCHER
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.imagePreview.setImageURI(uri)
        }
    }

    // 2. CAMERA LAUNCHER
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            selectedImageUri = tempCameraUri
            binding.imagePreview.setImageURI(tempCameraUri)
        }
    }

    // 3. PERMISSION LAUNCHER
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- PROTECT THE CAMERA FROM RESETTING ---
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tempCameraUri?.let { outState.putString("SAVED_TEMP_URI", it.toString()) }
        selectedImageUri?.let { outState.putString("SAVED_SELECTED_URI", it.toString()) }
        outState.putString("ORIGINAL_IMAGE_URL", originalImageUrl)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingDialog = LoadingDialog(requireActivity())

        // --- RESTORE SURVIVOR DATA ---
        if (savedInstanceState != null) {
            val savedTemp = savedInstanceState.getString("SAVED_TEMP_URI")
            if (savedTemp != null) tempCameraUri = Uri.parse(savedTemp)

            originalImageUrl = savedInstanceState.getString("ORIGINAL_IMAGE_URL") ?: ""

            val savedSelected = savedInstanceState.getString("SAVED_SELECTED_URI")
            if (savedSelected != null) {
                selectedImageUri = Uri.parse(savedSelected)
                view.post { binding.imagePreview.setImageURI(selectedImageUri) }
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupCountryDropdown()
        setupCategoryDropdown()

        editingRecipeId = arguments?.getInt("EDIT_RECIPE_ID", -1) ?: -1

        if (editingRecipeId != -1) {
            // WE ARE IN EDIT MODE!
            val editName = arguments?.getString("EDIT_RECIPE_NAME")
            val editImageUrl = arguments?.getString("EDIT_RECIPE_IMAGE") ?: ""
            val editCategory = arguments?.getString("EDIT_RECIPE_CATEGORY")
            val editCountry = arguments?.getString("EDIT_RECIPE_COUNTRY")
            val editTags = arguments?.getString("EDIT_RECIPE_TAGS")
            val editInstructions = arguments?.getString("EDIT_RECIPE_INSTRUCTIONS")
            val editIngredientsName = arguments?.getString("EDIT_RECIPE_INGREDIENTS_NAME")
            val editIngredientsQuantity = arguments?.getString("EDIT_RECIPE_INGREDIENTS_QUANTITY")

            // Save original image URL for Full Screen Viewer
            if (originalImageUrl.isEmpty()) originalImageUrl = editImageUrl

            // Only overwrite UI if we aren't recovering from the camera
            if (savedInstanceState == null) {
                binding.uploadRecipeName.setText(editName)
                binding.uploadCategory.setText(editCategory ?: "", false)
                binding.uploadCountryName.setText(editCountry ?: "", false)
                binding.uploadTags.setText(editTags)
                binding.uploadInstructions.setText(editInstructions)
                binding.uploadIngredientsName.setText(editIngredientsName)
                binding.uploadIngredientsQuantity.setText(editIngredientsQuantity)

                if (originalImageUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(originalImageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.imagePreview)
                }
            }

            binding.submitAddRecipeButton.text = "Update Recipe"
            binding.rDText.text = "Edit Recipe"
        }

        checkProfileStatusFromServer()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })

        // NEW SMART DIALOG
        binding.imagePreview.setOnClickListener {
            showImageSourceDialog()
        }

        // Unified Submit Button Click Listener
        binding.submitAddRecipeButton.setOnClickListener {
            val title = binding.uploadRecipeName.text.toString()
            val country = binding.uploadCountryName.text.toString()
            val category = binding.uploadCategory.text.toString()
            val tags = binding.uploadTags.text.toString()
            val instructions = binding.uploadInstructions.text.toString()
            val ingredientsName = binding.uploadIngredientsName.text.toString()
            val ingredientsQty = binding.uploadIngredientsQuantity.text.toString()

            if (title.isEmpty() || country.isEmpty() || category.isEmpty() || tags.isEmpty() || instructions.isEmpty() || ingredientsName.isEmpty() || ingredientsQty.isEmpty()) {
                Toast.makeText(requireContext(), "Please Fill All the Recipe Details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (editingRecipeId != -1) {
                updateRecipeOnServer(editingRecipeId, title, country, category, tags, instructions, ingredientsName, ingredientsQty)
            } else {
                if (selectedImageUri == null) {
                    Toast.makeText(requireContext(), "Image is required for new recipes", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                uploadDataToServer(title, country, category, tags, instructions, ingredientsName, ingredientsQty)
            }
        }
    }

    // --- NEW: SMART IMAGE DIALOG ---
    private fun showImageSourceDialog() {
        val hasImage = selectedImageUri != null || originalImageUrl.isNotEmpty()

        val options = if (hasImage) {
            arrayOf("View Photo", "Take Photo", "Choose from Gallery")
        } else {
            arrayOf("Take Photo", "Choose from Gallery")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Recipe Photo")
            .setItems(options) { _, which ->
                if (hasImage) {
                    when (which) {
                        0 -> showFullScreenImage()
                        1 -> checkCameraPermissionAndLaunch()
                        2 -> pickImageLauncher.launch("image/*")
                    }
                } else {
                    when (which) {
                        0 -> checkCameraPermissionAndLaunch()
                        1 -> pickImageLauncher.launch("image/*")
                    }
                }
            }
            .show()
    }

    // --- NEW: FULL SCREEN IMAGE VIEWER ---
    private fun showFullScreenImage() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.BLACK)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        if (selectedImageUri != null) {
            imageView.setImageURI(selectedImageUri)
        } else if (originalImageUrl.isNotEmpty()) {
            Glide.with(requireContext()).load(originalImageUrl).into(imageView)
        }

        imageView.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(imageView)
        dialog.show()
    }

    // --- NEW: CAMERA PERMISSIONS & LAUNCHER ---
    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val tempFile = File.createTempFile("recipe_cam_", ".jpg", requireContext().cacheDir)
            tempCameraUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", tempFile)
            takePictureLauncher.launch(tempCameraUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCountryDropdown() {
        val countries = resources.getStringArray(R.array.country_list)
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, countries) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        return FilterResults().apply { values = countries; count = countries.size }
                    }
                    @Suppress("UNCHECKED_CAST")
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) { notifyDataSetChanged() }
                }
            }
        }
        binding.uploadCountryName.setAdapter(adapter)
    }

    private fun setupCategoryDropdown() {
        val categories = resources.getStringArray(R.array.category)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.uploadCategory.setAdapter(adapter)
    }

    private fun updateRecipeOnServer(
        recipeId: Int, title: String, country: String, category: String, tags: String,
        instructions: String, ingredientsName: String, ingredientsQty: String
    ) {
        loadingDialog.startLoading()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
                val savedToken = sharedPref.getString("JWT_TOKEN", null)

                if (savedToken == null) {
                    withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Not logged in!", Toast.LENGTH_SHORT).show() }
                    return@launch
                }
                val authHeader = "Bearer $savedToken"

                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
                val tagsBody = tags.toRequestBody("text/plain".toMediaTypeOrNull())
                val instructionsBody = instructions.toRequestBody("text/plain".toMediaTypeOrNull())
                val ingredientsNameBody = ingredientsName.toRequestBody("text/plain".toMediaTypeOrNull())
                val ingredientsQtyBody = ingredientsQty.toRequestBody("text/plain".toMediaTypeOrNull())

                var imagePart: MultipartBody.Part? = null
                var imageFile: File? = null

                if (selectedImageUri != null) {
                    imageFile = getFileFromUri(selectedImageUri!!)
                    val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)
                }

                val response = RetrofitClient.myBackendApi.editRecipe(
                    token = authHeader, recipeId = recipeId.toString(), name = titleBody, country = countryBody,
                    category = categoryBody, tags = tagsBody, instructions = instructionsBody,
                    ingredientsName = ingredientsNameBody, ingredientsQuantity = ingredientsQtyBody, image = imagePart
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Recipe Updated Successfully!", Toast.LENGTH_SHORT).show()
                        imageFile?.delete()
                        findNavController().popBackStack()
                    } else if (response.code() == 401) {
                        Toast.makeText(requireContext(), "Session expired.", Toast.LENGTH_LONG).show()
                        sharedPref.edit().remove("JWT_TOKEN").apply()
                    } else {
                        Toast.makeText(requireContext(), "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_LONG).show() }
            } finally {
                // 2. DISMISS LOADER HERE
                withContext(Dispatchers.Main) {
                    loadingDialog.isDismiss()
                }
            }
        }
    }

    private fun uploadDataToServer(
        title: String, country: String, category: String, tags: String, instructions: String, ingredientsName: String, ingredientsQty: String
    ) {
        loadingDialog.startLoading()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
                val savedToken = sharedPref.getString("JWT_TOKEN", null)

                if (savedToken == null) {
                    withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Not logged in!", Toast.LENGTH_SHORT).show() }
                    return@launch
                }

                val authHeader = "Bearer $savedToken"
                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
                val tagsBody = tags.toRequestBody("text/plain".toMediaTypeOrNull())
                val instructionsBody = instructions.toRequestBody("text/plain".toMediaTypeOrNull())
                val ingredientsNameBody = ingredientsName.toRequestBody("text/plain".toMediaTypeOrNull())
                val ingredientsQtyBody = ingredientsQty.toRequestBody("text/plain".toMediaTypeOrNull())

                val imageFile = getFileFromUri(selectedImageUri!!)
                val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)

                val response = RetrofitClient.myBackendApi.addRecipes(
                    token = authHeader, name = titleBody, country = countryBody, category = categoryBody, tags = tagsBody,
                    instructions = instructionsBody, ingredientsName = ingredientsNameBody, ingredientsQuantity = ingredientsQtyBody, image = imagePart
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Recipe Uploaded Successfully!", Toast.LENGTH_SHORT).show()
                        imageFile.delete()
                        findNavController().popBackStack()
                    } else if (response.code() == 401) {
                        Toast.makeText(requireContext(), "Session expired.", Toast.LENGTH_LONG).show()
                        sharedPref.edit().remove("JWT_TOKEN").apply()
                    } else {
                        Toast.makeText(requireContext(), "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show() }
            } finally {
                // 2. DISMISS LOADER HERE
                withContext(Dispatchers.Main) {
                    loadingDialog.isDismiss()
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

//    private fun checkProfileStatusFromServer() {
//        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
//        val jwtToken = sharedPref.getString("JWT_TOKEN", null)
//
//        if (jwtToken.isNullOrEmpty()) {
//            binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
//            toggleFormVisibility(View.INVISIBLE)
//            return
//        }
//
//        loadingDialog.startLoading()
//
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitClient.myBackendApi.getUserProfile("Bearer $jwtToken")
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful && response.body() != null) {
//                        val isCompleteOnServer = response.body()?.isProfileComplete ?: false
//                        val isLockedorNot = response.body()?.isLocked ?: false
//                        sharedPref.edit().putBoolean("isProfileComplete", isCompleteOnServer).apply()
//                        sharedPref.edit().putBoolean("isLocked", isLockedorNot).apply()
//
//                        if (isCompleteOnServer) {
//                            binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
////                            toggleFormVisibility(View.VISIBLE)
//                            if (isLockedorNot == true){
//                                binding.accountNotPremium.visibility = View.GONE
//                                toggleFormVisibility(View.VISIBLE)
//                            } else {
//                                    binding.accountNotPremium.visibility = View.VISIBLE
//                                }
//                        } else {
//                            binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
//                            toggleFormVisibility(View.INVISIBLE)
//                        }
//                    } else {
//                        binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
//                        toggleFormVisibility(View.INVISIBLE)
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    val localCacheComplete = sharedPref.getBoolean("isProfileComplete", false)
//                    val localCacheIsLocked = sharedPref.getBoolean("isLocked", false)
//                    if (localCacheComplete && localCacheIsLocked) {
//                        binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
//                        binding.accountNotPremium.visibility = View.GONE
//                        toggleFormVisibility(View.VISIBLE)
//                    } else if (!localCacheComplete && localCacheIsLocked) {
//                        binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
//                        binding.accountNotPremium.visibility = View.VISIBLE
//                        toggleFormVisibility(View.INVISIBLE)
//                    } else {
//                        binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
//                        toggleFormVisibility(View.INVISIBLE)
//                    }
//                }
//            } finally {
//                withContext(Dispatchers.Main) {
//                    loadingDialog.isDismiss()
//                }
//            }
//        }
//    }

    private fun checkProfileStatusFromServer() {
        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
        val jwtToken = sharedPref.getString("JWT_TOKEN", null)

        if (jwtToken.isNullOrEmpty()) {
            // Not logged in at all. Treat as incomplete/free.
            binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
            binding.accountNotPremium.visibility = View.GONE
            toggleFormVisibility(View.INVISIBLE)
            return
        }

        loadingDialog.startLoading()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.myBackendApi.getUserProfile("Bearer $jwtToken")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val isCompleteOnServer = response.body()?.isProfileComplete ?: false

                        // Note: In the backend Profile route, isLocked maps to Users.isPremium
                        val isPremium = response.body()?.isLocked ?: false

                        // Cache the results for offline use later
                        sharedPref.edit().putBoolean("isProfileComplete", isCompleteOnServer).apply()
                        sharedPref.edit().putBoolean("isLocked", isPremium).apply()

                        // --- THE 3 SCENARIOS ---

                        if (!isPremium) {
                            // Scenario 1: Free Tier (Profile completeness doesn't matter)
                            binding.accountNotPremium.visibility = View.VISIBLE
                            binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
                            toggleFormVisibility(View.INVISIBLE)

                        } else if (!isCompleteOnServer) {
                            // Scenario 2: Premium Tier, but Profile is INCOMPLETE
                            binding.accountNotPremium.visibility = View.GONE
                            binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
                            toggleFormVisibility(View.INVISIBLE)

                        } else {
                            // Scenario 3: Premium Tier AND Profile is COMPLETE!
                            binding.accountNotPremium.visibility = View.GONE
                            binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
                            toggleFormVisibility(View.VISIBLE)
                        }

                    } else {
                        // Fallback if server returns an error code
                        binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
                        binding.accountNotPremium.visibility = View.GONE
                        toggleFormVisibility(View.INVISIBLE)
                    }
                }
            } catch (e: Exception) {
                // --- OFFLINE FALLBACK (Same 3 Scenarios using SharedPreferences) ---
                withContext(Dispatchers.Main) {
                    val localCacheComplete = sharedPref.getBoolean("isProfileComplete", false)
                    val localCacheIsPremium = sharedPref.getBoolean("isLocked", false)

                    if (!localCacheIsPremium) {
                        // Scenario 1 (Offline)
                        binding.accountNotPremium.visibility = View.VISIBLE
                        binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
                        toggleFormVisibility(View.INVISIBLE)
                    } else if (!localCacheComplete) {
                        // Scenario 2 (Offline)
                        binding.accountNotPremium.visibility = View.GONE
                        binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
                        toggleFormVisibility(View.INVISIBLE)
                    } else {
                        // Scenario 3 (Offline)
                        binding.accountNotPremium.visibility = View.GONE
                        binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
                        toggleFormVisibility(View.VISIBLE)
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.isDismiss()
                }
            }
        }
    }
    private fun toggleFormVisibility(visibilityState: Int) {
        binding.imagePreview.visibility = visibilityState
        binding.uRN.visibility = visibilityState
        binding.uCN.visibility = visibilityState
        binding.uC.visibility = visibilityState
        binding.uT.visibility = visibilityState
        binding.uI.visibility = visibilityState
        binding.iqLl.visibility = visibilityState
        binding.submitAddRecipeButton.visibility = visibilityState
        binding.hintTextview.visibility = visibilityState
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
    }

    override fun onPause() {
        super.onPause()
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
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.activity.OnBackPressedCallback
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import androidx.lifecycle.lifecycleScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.RequestBody.Companion.asRequestBody
//import okhttp3.RequestBody.Companion.toRequestBody
//import java.io.File
//import java.io.FileOutputStream
//import android.net.Uri
//import android.widget.ArrayAdapter
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.navigation.fragment.findNavController
//import com.bumptech.glide.Glide
//import com.infinitybutterfly.infiflyrecipe.databinding.FragmentAddRecipeBinding
//import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
//import android.Manifest
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import androidx.core.content.ContextCompat
//
//class AddRecipeFragment : Fragment() {
//    private var _binding: FragmentAddRecipeBinding? = null
//    private val binding get() = _binding!!
//    private var selectedImageUri: Uri? = null
//    private var editingRecipeId: Int = -1
//    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        if (uri != null) {
//            selectedImageUri = uri
//
//            // This line instantly shows the selected photo on the screen!
//            binding.imagePreview.setImageURI(uri)
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
////        return inflater.inflate(R.layout.fragment_add_recipe, container, false)
//        _binding = FragmentAddRecipeBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding.btnBack.setOnClickListener {
//            findNavController().popBackStack()
//        }
//
//        setupCountryDropdown()
//        setupCategoryDropdown()
//
//        editingRecipeId = arguments?.getInt("EDIT_RECIPE_ID", -1) ?: -1
//
//        if (editingRecipeId != -1) {
//            // WE ARE IN EDIT MODE!
//
//            // 2. Extract the data from the Bundle
//            val editName = arguments?.getString("EDIT_RECIPE_NAME")
//            val editImageUrl = arguments?.getString("EDIT_RECIPE_IMAGE")
//            val editCategory = arguments?.getString("EDIT_RECIPE_CATEGORY")
//            val editCountry = arguments?.getString("EDIT_RECIPE_COUNTRY")
//            val editTags = arguments?.getString("EDIT_RECIPE_TAGS")
//            val editInstructions = arguments?.getString("EDIT_RECIPE_INSTRUCTIONS")
//            val editIngredientsName = arguments?.getString("EDIT_RECIPE_INGREDIENTS_NAME")
//            val editIngredientsQuantity = arguments?.getString("EDIT_RECIPE_INGREDIENTS_QUANTITY")
//            // val editInstructions = arguments?.getString("EDIT_RECIPE_INSTRUCTIONS")
//
//            // 3. Pre-fill your EditTexts and UI
//             binding.uploadRecipeName.setText(editName)
////             binding.imagePreview.setImageResource(editImageUrl)
//// Make sure the image URL actually exists before trying to load it
//            if (!editImageUrl.isNullOrEmpty()) {
//                Glide.with(requireContext())
//                    .load(editImageUrl)
//                    // Optional: Add a placeholder while the image loads
//                    .placeholder(R.drawable.ic_launcher_background)
//                    .into(binding.imagePreview)
//            }
//             binding.uploadCategory.setText(editCategory ?: "", false)
//             binding.uploadCountryName.setText(editCountry ?: "", false)
//             binding.uploadTags.setText(editTags)
//             binding.uploadInstructions.setText(editInstructions)
//             binding.uploadIngredientsName.setText(editIngredientsName)
//             binding.uploadIngredientsQuantity.setText(editIngredientsQuantity)
//
//            // Optionally, load the existing image into your preview ImageView
//             Glide.with(this).load(editImageUrl).into(binding.imagePreview)
//
//            // 4. Change the submit button text to make sense for editing
//             binding.submitAddRecipeButton.text = "Update Recipe"
//            binding.rDText.text = "Edit Recipe"
//        }
//
////        checkUserCache()
//        checkProfileStatusFromServer()
//
//        requireActivity().onBackPressedDispatcher.addCallback(
//            viewLifecycleOwner,
//            object : OnBackPressedCallback(true) {
//                override fun handleOnBackPressed() {
//                    findNavController().popBackStack()
//
////                    val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
////                    bottomNav.selectedItemId = R.id.homeFragment
//                }
//            }
//        )
//
//        binding.imagePreview.setOnClickListener {
//            pickImageLauncher.launch("image/*")
//        }
//
////        binding.btnSelectImage.setOnClickListener {
////            pickImageLauncher.launch("image/*") // Ask for image files only
////        }
//
//        // Unified Submit Button Click Listener
//        binding.submitAddRecipeButton.setOnClickListener {
//            // 1. Get all the text from the fields first! (Both Add and Edit need this)
//            val title = binding.uploadRecipeName.text.toString()
//            val country = binding.uploadCountryName.text.toString()
//            val category = binding.uploadCategory.text.toString()
//            val tags = binding.uploadTags.text.toString()
//            val instructions = binding.uploadInstructions.text.toString()
//            val ingredientsName = binding.uploadIngredientsName.text.toString()
//            val ingredientsQty = binding.uploadIngredientsQuantity.text.toString()
//
//            // 2. Basic Validation (Title is always required)
//            if (title.isEmpty() || country.isEmpty() || category.isEmpty() || tags.isEmpty() || instructions.isEmpty() || ingredientsName.isEmpty() || ingredientsQty.isEmpty()) {
//                Toast.makeText(requireContext(), "Please Fill All the Recipe Details", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            // 3. Route to the correct API function
//            if (editingRecipeId != -1) {
//                // --- EDIT MODE ---
//                // We don't check for selectedImageUri here, because they might just be fixing text!
//                updateRecipeOnServer(
//                    editingRecipeId,
//                    title,
//                    country,
//                    category,
//                    tags,
//                    instructions,
//                    ingredientsName,
//                    ingredientsQty
//                )
////                findNavController().popBackStack()
//            } else {
//                // --- ADD MODE ---
//                if (title.isEmpty()  || selectedImageUri == null || country.isEmpty() || category.isEmpty() || tags.isEmpty() || instructions.isEmpty() || ingredientsName.isEmpty() || ingredientsQty.isEmpty()) {
//                    Toast.makeText(requireContext(), "Please Fill All the Recipe Details", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }
//
//                uploadDataToServer(
//                    title,
//                    country,
//                    category,
//                    tags,
//                    instructions,
//                    ingredientsName,
//                    ingredientsQty
//                )
////                findNavController().popBackStack()
//            }
//        }
//
//        // CAN NOT USE THIS LOGIC AS IT WILL PREVENT FROM ADDING NEW RECIPES
////        // ... Set up your save/submit button logic
////        binding.submitAddRecipeButton.setOnClickListener {
////            if (editingRecipeId != -1) {
////                // Call your PUT / Update API
////            } else {
////                val title = binding.uploadRecipeName.text.toString()
////                val country = binding.uploadCountryName.text.toString()
////                val category = binding.uploadCategory.text.toString()
////                val tags = binding.uploadTags.text.toString()
////                val instructions = binding.uploadInstructions.text.toString()
////                val ingredientsName = binding.uploadIngredientsName.text.toString()
////                val ingredientsQty = binding.uploadIngredientsQuantity.text.toString()
////
////                if (title.isEmpty() || selectedImageUri == null) {
////                    Toast.makeText(requireContext(), "Title and Image are required", Toast.LENGTH_SHORT).show()
////                    return@setOnClickListener
////                }
////
////                uploadDataToServer(title, country, category, tags, instructions, ingredientsName, ingredientsQty)
////
////                // Call your POST / Create API
////            }
////        }
//
//        // 2. Button to submit the data
////        binding.submitAddRecipeButton.setOnClickListener {
////            val title = binding.uploadRecipeName.text.toString()
////            val country = binding.uploadCountryName.text.toString()
////            val category = binding.uploadCategory.text.toString()
////            val tags = binding.uploadTags.text.toString()
////            val instructions = binding.uploadInstructions.text.toString()
////            val ingredientsName = binding.uploadIngredientsName.text.toString()
////            val ingredientsQty = binding.uploadIngredientsQuantity.text.toString()
////
////            if (title.isEmpty() || selectedImageUri == null) {
////                Toast.makeText(requireContext(), "Title and Image are required", Toast.LENGTH_SHORT).show()
////                return@setOnClickListener
////            }
////
////            uploadDataToServer(title, country, category, tags, instructions, ingredientsName, ingredientsQty)
////        }
////            val titleInput = binding.uploadRecipeName.text.toString()
////            val servingsInput = binding.uploadCountryName.text.toString()
////
////            // Basic validation
////            if (titleInput.isEmpty() || servingsInput.isEmpty() || selectedImageUri == null) {
////                Toast.makeText(requireContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
////                return@setOnClickListener
////            }
////
////            uploadDataToServer(titleInput, servingsInput)
////        }
//    }
//
////    private fun uploadDataToServer(
////        title: String,
////        country: String,
////        category: String,
////        tags: String,
////        instructions: String,
////        ingredientsName: String,
////        ingredientsQty: String
////    ) {
////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////            try {
////                // 1. Convert Strings/Integers to Retrofit RequestBody
////                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
////                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
////                val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
////                val tagsBody = tags.toRequestBody("text/plain".toMediaTypeOrNull())
////                val instructionsBody = instructions.toRequestBody("text/plain".toMediaTypeOrNull())
////                val ingredientsNameBody = ingredientsName.toRequestBody("text/plain".toMediaTypeOrNull())
////                val ingredientsQtyBody = ingredientsQty.toRequestBody("text/plain".toMediaTypeOrNull())
////
////                // 2. Convert the image URI to a real File using a helper function
////                val imageFile = getFileFromUri(selectedImageUri!!)
////                val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
////
////                // 3. Package the image into a MultipartBody.Part
////                // "image" is the key your server expects to see. Change it if your server requires a different name!
////                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)
////
////                // 4. Make the network call
////                val response = RetrofitInstanceAddRecipe.api.uploadRecipe(titleBody, countryBody, categoryBody, tagsBody, instructionsBody, ingredientsNameBody, ingredientsQtyBody, imagePart)
////
////                withContext(Dispatchers.Main) {
////                    if (response.isSuccessful) {
////                        Toast.makeText(requireContext(), "Upload Successful!", Toast.LENGTH_SHORT).show()
////                        imageFile.delete()
////                    } else {
////                        Toast.makeText(requireContext(), "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
////                    }
////                }
////            } catch (e: Exception) {
////                withContext(Dispatchers.Main) {
////                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
////                }
////            }
////        }
////    }
//
//    private fun setupCountryDropdown() {
////        val editLayout = requireView().findViewById<View>(R.id.addRecipeFragment)
////        val countryDropdown = editLayout.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.upload_country_name)
//        val countries = resources.getStringArray(R.array.country_list)
//
//        val adapter = ArrayAdapter(
//            requireContext(),
//            android.R.layout.simple_dropdown_item_1line,
//            countries
//        )
////        countryDropdown.setAdapter(adapter)
//        binding.uploadCountryName.setAdapter(adapter)
//    }
//
//    private fun setupCategoryDropdown() {
////        val editLayout = requireView().findViewById<View>(R.id.addRecipeFragment)
////        val categoryDropdown = editLayout.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.upload_category)
//        val categories = resources.getStringArray(R.array.category)
//
//        val adapter = ArrayAdapter(
//            requireContext(),
//            android.R.layout.simple_dropdown_item_1line,
//            categories
//        )
////        categoryDropdown.setAdapter(adapter)
//        binding.uploadCategory.setAdapter(adapter)
//    }
//
//    private fun updateRecipeOnServer(
//        recipeId: Int,
//        title: String,
//        country: String,
//        category: String,
//        tags: String,
//        instructions: String,
//        ingredientsName: String,
//        ingredientsQty: String
//    ) {
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                // 1. Get the Token
//                val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
//                val savedToken = sharedPref.getString("JWT_TOKEN", null)
//
//                if (savedToken == null) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(requireContext(), "Not logged in! Please log in first.", Toast.LENGTH_SHORT).show()
//                    }
//                    return@launch
//                }
//                val authHeader = "Bearer $savedToken"
//
//                // 2. Prepare the Text Data
//                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
//                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
//                val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
//                val tagsBody = tags.toRequestBody("text/plain".toMediaTypeOrNull())
//                val instructionsBody = instructions.toRequestBody("text/plain".toMediaTypeOrNull())
//                val ingredientsNameBody = ingredientsName.toRequestBody("text/plain".toMediaTypeOrNull())
//                val ingredientsQtyBody = ingredientsQty.toRequestBody("text/plain".toMediaTypeOrNull())
//
//                // 3. Prepare the Image (ONLY if the user selected a new one)
//                var imagePart: MultipartBody.Part? = null
//                var imageFile: File? = null // Keep a reference to delete it later
//
//                if (selectedImageUri != null) {
//                    imageFile = getFileFromUri(selectedImageUri!!)
//                    val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
//                    imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)
//                }
//
//                // 4. Make the PUT Network Call
//                val response = RetrofitClient.myBackendApi.editRecipe(
//                    token = authHeader,
//                    recipeId = recipeId.toString(),
//                    name = titleBody,
//                    country = countryBody,
//                    category = categoryBody,
//                    tags = tagsBody,
//                    instructions = instructionsBody,
//                    ingredientsName = ingredientsNameBody,
//                    ingredientsQuantity = ingredientsQtyBody,
//                    image = imagePart // This will safely send null if they didn't pick a new photo!
//                )
//
//                // 5. Handle Response
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful) {
//                        Toast.makeText(requireContext(), "Recipe Updated Successfully!", Toast.LENGTH_SHORT).show()
//                        imageFile?.delete() // Clean up the temp file if we created one
//
//                        // Go back to the previous screen (My Recipes list)
//                        findNavController().popBackStack()
//
//                    } else if (response.code() == 401) {
//                        Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
//                        sharedPref.edit().remove("JWT_TOKEN").apply()
//                    } else {
//                        Toast.makeText(requireContext(), "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private fun uploadDataToServer(
//        title: String,
//        country: String,
//        category: String,
//        tags: String,
//        instructions: String,
//        ingredientsName: String,
//        ingredientsQty: String
//    ) {
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                // --- 1. THE JWT WRISTBAND CHECK ---
//                // Dig into the pocket (SharedPreferences) and get the saved token
//                val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
//                val savedToken = sharedPref.getString("JWT_TOKEN", null)
//
//                // If they don't have a token, they can't upload!
//                if (savedToken == null) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(requireContext(), "Not logged in! Please log in first.", Toast.LENGTH_SHORT).show()
//                    }
//                    return@launch
//                }
//
//                // Format the wristband. (It MUST say "Bearer " before the token string)
//                val authHeader = "Bearer $savedToken"
//
//
//                // --- 2. PREPARE THE DATA ---
//                // Convert Strings/Integers to Retrofit RequestBody
//                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
//                val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
//                val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
//                val tagsBody = tags.toRequestBody("text/plain".toMediaTypeOrNull())
//                val instructionsBody = instructions.toRequestBody("text/plain".toMediaTypeOrNull())
//                val ingredientsNameBody = ingredientsName.toRequestBody("text/plain".toMediaTypeOrNull())
//                val ingredientsQtyBody = ingredientsQty.toRequestBody("text/plain".toMediaTypeOrNull())
//
//                // Convert the image URI to a real File using your helper function
//                val imageFile = getFileFromUri(selectedImageUri!!)
//                val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
//
//                // Package the image into a MultipartBody.Part
//                // "image" must match the name Ktor expects in receiveMultipart() -> PartData.FormItem
//                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)
//
//
//                // --- 3. MAKE THE SECURE NETWORK CALL ---
//                // NOTE: Make sure your Retrofit API interface method is named 'addRecipe' and accepts the token first!
////                val response = RetrofitClient.myBackendApi.addRecipe(
////                    authHeader, // <-- PASSING THE JWT HERE!
////                    name = titleBody,
////                    countryBody,
////                    categoryBody,
////                    tagsBody,
////                    instructionsBody,
////                    ingredientsNameBody,
////                    ingredientsQuantity = ingredientsQtyBody,
////                    imagePart
////                )
//                val response = RetrofitClient.myBackendApi.addRecipes(
//                    token = authHeader,
//                    name = titleBody,
//                    country = countryBody,
//                    category = categoryBody,
//                    tags = tagsBody,
//                    instructions = instructionsBody,
//                    ingredientsName = ingredientsNameBody,
//                    ingredientsQuantity = ingredientsQtyBody,
//                    image = imagePart
////                    title = titleBody,
////                    ingredientsQty = ingredientsQtyBody
//                )
//
//                // --- 4. HANDLE THE RESPONSE ---
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful) {
//                        Toast.makeText(requireContext(), "Recipe Uploaded Successfully!", Toast.LENGTH_SHORT).show()
//                        imageFile.delete() // Clean up the temp file
//
//                        // Optional: Clear the form so they can add another!
//                        binding.uploadRecipeName.text?.clear()
//                        binding.imagePreview.setImageResource(android.R.drawable.ic_menu_gallery)
//                        selectedImageUri = null
//                        binding.uploadCountryName.text?.clear()
//                        binding.uploadCategory.text?.clear()
//                        binding.uploadTags.text?.clear()
//                        binding.uploadInstructions.text?.clear()
//                        binding.uploadIngredientsName.text?.clear()
//                        binding.uploadIngredientsQuantity.text?.clear()
//
//                        findNavController().popBackStack()
//                    } else if (response.code() == 401) {
//                        // 401 Unauthorized means the token is fake or expired (older than 7 days)
//                        Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
//
//                        // Clear the dead token from SharedPreferences
//                        sharedPref.edit().remove("JWT_TOKEN").apply()
//
//                        // Optional: Write code here to navigate back to your Login screen!
//
//                    } else {
//                        Toast.makeText(requireContext(), "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private fun getFileFromUri(uri: Uri): File {
//        val inputStream = requireContext().contentResolver.openInputStream(uri)
//        val tempFile = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
//        val outputStream = FileOutputStream(tempFile)
//
//        inputStream?.copyTo(outputStream)
//
//        inputStream?.close()
//        outputStream.close()
//
//        return tempFile
//    }
//
//    // Helper function to check the local cache
////    private fun checkUserCache() {
////        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
////
////        val isProfileComplete = sharedPref.getBoolean("isProfileComplete", false)
////
////
////        if (isProfileComplete) {
////            binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
////            toggleFormVisibility(View.VISIBLE)
////        } else {
////            binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
////            toggleFormVisibility(View.INVISIBLE)
////        }
////    }
//    private fun checkProfileStatusFromServer() {
//        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
//        val jwtToken = sharedPref.getString("JWT_TOKEN", null)
//
//        // 1. If there's no token at all, hide the form immediately
//        if (jwtToken.isNullOrEmpty()) {
//            binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
//            toggleFormVisibility(View.INVISIBLE)
//            return
//        }
//
//        // 2. Make the API Call to check the server
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                // ⚠️ IMPORTANT: Replace `getProfile` with the actual name of your GET endpoint in your Retrofit API Interface!
//                val response = RetrofitClient.myBackendApi.getUserProfile("Bearer $jwtToken")
//
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful && response.body() != null) {
//
//                        // 3. Read the boolean directly from the server response
//                        val isCompleteOnServer = response.body()?.isProfileComplete ?: false
//
//                        // Optional: Update your local cache to keep it in sync with the server
//                        sharedPref.edit().putBoolean("isProfileComplete", isCompleteOnServer).apply()
//
//                        // 4. Toggle the UI based on the server's truth
//                        if (isCompleteOnServer) {
//                            binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
//                            toggleFormVisibility(View.VISIBLE)
//                        } else {
//                            binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
//                            toggleFormVisibility(View.INVISIBLE)
//                        }
//                    } else {
//                        // If the server returns an error (like 401 Unauthorized), hide the form
//                        binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
//                        toggleFormVisibility(View.INVISIBLE)
//                    }
//                }
//            } catch (e: Exception) {
//                // 5. SMART FALLBACK: If there is no internet, rely on the local cache!
//                withContext(Dispatchers.Main) {
//                    val localCacheComplete = sharedPref.getBoolean("isProfileComplete", false)
//
//                    if (localCacheComplete) {
//                        binding.fillAccountDetailsTextAddRecipe.visibility = View.GONE
//                        toggleFormVisibility(View.VISIBLE)
//                    } else {
//                        binding.fillAccountDetailsTextAddRecipe.visibility = View.VISIBLE
//                        toggleFormVisibility(View.INVISIBLE)
//                    }
//                }
//            }
//        }
//    }
//    private fun toggleFormVisibility(visibilityState: Int) {
//        binding.imagePreview.visibility = visibilityState
//        binding.uRN.visibility = visibilityState // u_r_n
//        binding.uCN.visibility = visibilityState // u_c_n
//        binding.uC.visibility = visibilityState  // u_c
//        binding.uT.visibility = visibilityState  // u_t
//        binding.uI.visibility = visibilityState  // u_i
//        binding.iqLl.visibility = visibilityState // iq_ll
//        binding.submitAddRecipeButton.visibility = visibilityState
//        binding.hintTextview.visibility = visibilityState
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // Turn OFF swipe to refresh while filling out the form
//        (requireActivity() as MainActivity).setSwipeRefreshEnabled(false)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        // Turn it back ON when they leave this screen
//        (requireActivity() as MainActivity).setSwipeRefreshEnabled(true)
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
//
//
//
//
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
//// * Use the [AddRecipeFragment.newInstance] factory method to
//// * create an instance of this fragment.
//// */
////class AddRecipeFragment : Fragment() {
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
////        return inflater.inflate(R.layout.fragment_add_recipe, container, false)
////    }
////
////    companion object {
////        /**
////         * Use this factory method to create a new instance of
////         * this fragment using the provided parameters.
////         *
////         * @param param1 Parameter 1.
////         * @param param2 Parameter 2.
////         * @return A new instance of fragment AddRecipeFragment.
////         */
////        // TODO: Rename and change types and number of parameters
////        @JvmStatic
////        fun newInstance(param1: String, param2: String) =
////            AddRecipeFragment().apply {
////                arguments = Bundle().apply {
////                    putString(ARG_PARAM1, param1)
////                    putString(ARG_PARAM2, param2)
////                }
////            }
////    }
////}