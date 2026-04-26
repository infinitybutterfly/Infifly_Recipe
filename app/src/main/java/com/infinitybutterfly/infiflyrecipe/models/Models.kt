package com.infinitybutterfly.infiflyrecipe.models

import com.google.gson.annotations.SerializedName

// ==========================================
// 1. UI RECYCLERVIEW MODELS
// Used for displaying the lists on your login screens
// ==========================================

data class AllergyView(
    var profileImage: Int,
    var nameText: String,
    var isSelected: Boolean = false
)

data class FavFoodView (
    var profileImage: Int,
    var nameText: String,
    var isSelected: Boolean = false
)


// ==========================================
// 2. KTOR BACKEND MODELS (USER & AUTH)
// ==========================================

data class EmailOtpRequest(val email: String)
data class VerifyEmailRequest(val email: String, val otp: String)
data class SimpleMessageResponse(val success: Boolean, val message: String)
data class PinUpgradeRequest(
    val pin: String
)
data class ProfileListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("results")
    val results: List<UserProfiles>
)

// 2. The actual User Profile data
data class UserProfiles(
    @SerializedName("name")
    val name: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("country")
    val country: String,

    @SerializedName("dob")
    val dob: String?,

    @SerializedName("bio")
    val bio: String?,

    @SerializedName("profileImageUrl")
    val profilePicUrl: String?,

    @SerializedName("isProfileComplete")
    val isProfileComplete: Boolean?,

    @SerializedName("allergies")
    val allergies: String?,

    @SerializedName("favFoods")
    val favFoods: String?,

    @SerializedName("email")
    val email: String,

    @SerializedName("recipes") val uploadedRecipes: List<RecipeSummary>?
)

data class RecipeSummary(
    @SerializedName("id") val id: String,
    @SerializedName("recipeName") val title: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("countryName") val countryName: String?,
    @SerializedName("category") val category: String?
)

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val message: String?,
    val isProfileComplete: Boolean?,
    val isLocked: Boolean,
    @SerializedName("profileData")
    val profileData: UserProfile?
)

data class UserProfile(
    val name: String?,
    val username: String?,
    val country: String?,
    val dob: String?,
    val bio: String?,
    val profileImageUrl: String?,
    @SerializedName("favFoods")
    val favFoods: String? = null,   // Found them!
    @SerializedName("allergies")
    val allergies: String? = null // Found them!
)

data class UserProfileResponse(
    @SerializedName("name") val name: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("profileImageUrl") val profilePicUrl: String? = null,
    @SerializedName("isProfileComplete") val isProfileComplete: Boolean? = null,
    @SerializedName("favFoods") val favFoods: String? = null,
    @SerializedName("allergies") val allergies: String? = null,
    @SerializedName("isLocked") val isLocked: Boolean? = null,
    @SerializedName("viewcount") val viewcount: Int
)

data class UsernameCheckResponse(
    val available: Boolean
)

data class SubmitProblemResponse(
    val title: String,
    val description: String
)


// ==========================================
// 3. KTOR BACKEND MODELS (RECIPES)
// ==========================================

data class RecipeFeedResponse(
    val success: Boolean,
    val isLocked: Boolean,
    val viewcount: Int,
    val recipes: List<RecipeResponse>
)

data class RecipeSearchResponse(
    val success: Boolean,
    val isLocked: Boolean,
    val viewcount: Int,
    val results: List<RecipeResponse>
)
data class RecipeSearchResponseId(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val category: String,
    val country: String,
    val tags: String,
    val instructions: String,
    val ingredientsName: String,
    val ingredientsQuantity: String,
    val userName: String?,

    val isLocked: Boolean,
    val viewcount: Int
)

data class RecipeResponse(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val category: String,
    val country: String,
    val tags: String,
    val instructions: String,
    val ingredientsName: String,
    val ingredientsQuantity: String,
    val userName: String?,

    val isLocked: Boolean
)

data class MyRecipeResponse(
    val success: Boolean,
    val isLocked: Boolean,
    val viewcount: Int,
    @SerializedName("recipes") val results: List<Recipe>?
)

data class Recipe(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val userName: String,
    val category: String,
    val country: String,
    val tags: String,
    val instructions: String,
    val ingredientsName: String,
    val ingredientsQuantity: String
)


// ==========================================
// 4. PUBLIC MEALDB API MODELS
// ==========================================

// Details
data class RecipeResponseDetails(val meals: List<RecipeItemsDetails>?)
data class RecipeItemsDetails(
    val idMeal: String,
    val strMealThumb: String,
    val strArea: String,
    val strMeal: String,
    val strCategory: String,
    val strIngredient1: String, val strIngredient2: String, val strIngredient3: String, val strIngredient4: String,
    val strIngredient5: String, val strIngredient6: String, val strIngredient7: String, val strIngredient8: String,
    val strIngredient9: String, val strIngredient10: String, val strIngredient11: String,
    val strMeasure1: String, val strMeasure2: String, val strMeasure3: String, val strMeasure4: String,
    val strMeasure5: String, val strMeasure6: String, val strMeasure7: String, val strMeasure8: String,
    val strMeasure9: String, val strMeasure10: String, val strMeasure11: String,
    val strInstructions: String
)

// Discovery
data class RecipeResponseDiscovery(val categories: List<RecipeItemsDiscovery>?)
data class RecipeItemsDiscovery(
    val strCategoryThumb: String,
    val strCategoryDescription: String,
    val strCategory: String
)

// For You
data class RecipeResponseForYou(val meals: List<RecipeItemsForYou>?)
data class RecipeItemsForYou(
    val idMeal: String,
    val strMealThumb: String,
    val strArea: String,
    val strMeal: String
)

// Popular
data class RecipeResponsePopular(val meals: List<RecipeItemsPopular>?)
data class RecipeItemsPopular(
    val id: String,
    val idMeal: String,
    val strMealThumb: String,
    val strArea: String,
    val strMeal: String
)

// Search (Raw data from MealDB)
data class RecipeResponseSearch(val meals: List<RecipeItemsSearch>?)
data class RecipeItemsSearch(
    val idMeal: String,
    val strMealThumb: String,
    val strArea: String,
    val strMeal: String
)

// ==========================================
// 5. UNIFIED UI MODELS
// Used to combine different API responses into one list
// ==========================================
data class UnifiedRecipeItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val isFromMyBackend: Boolean,
    val country: String
)

data class UserProfileItems(
    val name: String,
    val username: String,
    val email: String,
    val profilePicUrl: String,
    val isProfileComplete: Boolean
)

//// Search
//data class RecipeResponseSearch(val meals: List<UnifiedRecipeItem>?)
//data class UnifiedRecipeItem(
//    val idMeal: String,
//    val strMealThumb: String,
//    val strArea: String,
//    val strMeal: String
//)