package com.infinitybutterfly.infiflyrecipe.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitybutterfly.infiflyrecipe.R
import com.infinitybutterfly.infiflyrecipe.models.AllergyView
import com.infinitybutterfly.infiflyrecipe.models.EmailOtpRequest
import com.infinitybutterfly.infiflyrecipe.models.FavFoodView
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginCreateAccountFragmentViewModel : ViewModel() {

    // Data that survives rotation
    var userEmail: String = ""

    val listAllergy = arrayListOf(
        AllergyView(R.drawable.milk, "Milk"), AllergyView(R.drawable.egg, "Eggs"),
        AllergyView(R.drawable.peanut, "Peanuts"), AllergyView(R.drawable.tree_nuts, "Tree Nuts"),
        AllergyView(R.drawable.fish, "Fish"), AllergyView(R.drawable.sesame, "Sesame"),
        AllergyView(R.drawable.wheat, "Wheat"), AllergyView(R.drawable.soybean, "Soybeans"),
        AllergyView(R.drawable.block, "No Allergy")
    )
    val listFavFood = arrayListOf(
        FavFoodView(R.drawable.vegetable, "Vegetables"), FavFoodView(R.drawable.fruits, "Fruits"),
        FavFoodView(R.drawable.dairy_products, "Dairy Products"), FavFoodView(R.drawable.chicken, "Chicken"),
        FavFoodView(R.drawable.wheat, "Grains/Staples"), FavFoodView(R.drawable.fish, "Seafood"),
        FavFoodView(R.drawable.dessertssweets, "Desserts/Sweets"), FavFoodView(R.drawable.soybean, "Legumes/Beans"),
        FavFoodView(R.drawable.tree_nuts, "Nuts & Seeds")
    )
    var finalFavFoods: String = ""
    var finalAllergies: String = ""
    var selectedFavFoodsList = mutableSetOf<String>()
    var selectedAllergiesList = mutableSetOf<String>()
    var currentStep: Int = 0 // Tracks the ViewFlipper index

    // LiveData to tell the Fragment when the API is done
    val otpSentSuccess = MutableLiveData<Boolean?>()
    val errorMessage = MutableLiveData<String?>()

    fun sendOtp(email: String) {
        this.userEmail = email
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = EmailOtpRequest(email = email)
                val response = RetrofitClient.myBackendApi.requestOtp(request)

                otpSentSuccess.postValue(response.isSuccessful && response.body()?.success == true)
            } catch (e: Exception) {
                errorMessage.postValue("Network Error: ${e.message}")
            }
        }
    }
    fun consumeOtpEvent() {
        otpSentSuccess.value = null
    }
}