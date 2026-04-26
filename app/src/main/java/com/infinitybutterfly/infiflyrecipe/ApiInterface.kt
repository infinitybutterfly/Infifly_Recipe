package com.infinitybutterfly.infiflyrecipe

import com.infinitybutterfly.infiflyrecipe.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// ==========================================
// 1. YOUR KTOR BACKEND API
// Everything here goes to http://10.0.2.2:8080/
// ==========================================
interface KtorBackendApi {

    // --- AUTHENTICATION ---
    @POST("api/request-otp")
    suspend fun requestOtp(@Body request: EmailOtpRequest): Response<SimpleMessageResponse>

    @POST("api/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyEmailRequest): Response<LoginResponse>


    // --- PROFILE / USER ---
    @GET("api/profile/me")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfileResponse>

    // Fetches a single User Profile by their exact username
    @GET("api/profile/{username}")
    suspend fun getProfileByUsername(
        @Header("Authorization") token: String,
        @Path("username") username: String
    ): Response<UserProfiles>

    @Multipart
    @POST("api/profile/update")
    suspend fun updateProfile(
//        @Part("jwt") jwt: RequestBody,
        @Header("Authorization") token: String,
        @Part("email") email: RequestBody?,
        @Part("name") name: RequestBody?,
        @Part("country") country: RequestBody?,
        @Part("username") username: RequestBody?,
        @Part("dob") dob: RequestBody?,
        @Part("bio") bio: RequestBody?,
        @Part("fav_foods") favFoods: RequestBody?,
        @Part("allergies") allergies: RequestBody?,
        @Part("is_profile_complete") isCompleteCache: RequestBody,
        @Part("isLocked") isLocked: RequestBody,
        @Part image: MultipartBody.Part?,
    ): Response<SimpleMessageResponse>

    data class HelpTicketRequest(
        val title: String,
        val description: String
    )
    @POST("api/help")
    suspend fun submitHelpTicket(
        @Header("Authorization") token: String,
//        @Query("title") title: String,
//        @Query("description") description: String,
        @Body request: HelpTicketRequest
        ): Response<SubmitProblemResponse>


    // --- KTOR RECIPES ---
    @Multipart
    @POST("api/recipes/add")
    suspend fun addRecipes(
        @Header("Authorization") token: String,
        @Part("recipe_name") name: RequestBody,
        @Part("country") country: RequestBody,
        @Part("category") category: RequestBody,
        @Part("tags") tags: RequestBody,
        @Part("instructions") instructions: RequestBody,
        @Part("ingredients_name") ingredientsName: RequestBody,
        @Part("ingredients_quantity") ingredientsQuantity: RequestBody,
        @Part image: MultipartBody.Part
//        @Part("recipe_name") title: RequestBody,
//        @Part("ingredients_quantity") ingredientsQty: RequestBody
    ): Response<SimpleMessageResponse>

    // Fetches a single recipe by ID from your backend
    @GET("api/recipes/{id}")
    suspend fun getRecipeById(
        @Header("Authorization") token: String,
        @Path("id") recipeId: String
    ): Response<RecipeSearchResponseId>

    @GET("api/recipes/search")
    suspend fun searchRecipes(
        @Header("Authorization") token: String,
        @Query("q") query: String
    ): Response<RecipeSearchResponse>

    @GET("api/check-username")
    suspend fun checkUsernameAvailability(
        @Query("username") username: String
    ): Response<UsernameCheckResponse>

    @DELETE("api/recipes/{id}")
    suspend fun deleteRecipe(
        @Header("Authorization") token: String,
        @Path("id") recipeId: Int
    ): Response<SimpleMessageResponse>

    @Multipart
    @PUT("api/recipes/{id}")
    suspend fun editRecipe(
        @Header("Authorization") token: String,
        @Path("id") recipeId: String,

        // These are optional. Only pass what the user actually changed!
        @Part("recipe_name") name: RequestBody?,
        @Part("country") country: RequestBody?,
        @Part("category") category: RequestBody?,
        @Part("tags") tags: RequestBody?,
        @Part("instructions") instructions: RequestBody?,
        @Part("ingredients_name") ingredientsName: RequestBody?,
        @Part("ingredients_quantity") ingredientsQuantity: RequestBody?,

        // The image is optional too!
        @Part image: MultipartBody.Part? = null
    ): Response<SimpleMessageResponse>

    // Search Profile
    @GET("api/profile")
    suspend fun searchProfiles(
        @Header("Authorization") token: String,

        @Query("search") searchQuery: String? = null, // Optional search text
        @Query("limit") limit: Int = 50,              // Defaults to 50
        @Query("offset") offset: Int = 0              // Defaults to 0
    ): Response<ProfileListResponse>

//    Get All Profiles
//    @GET("/api/profile")
//    suspend fun getAllProfiles(
//    @Header("Authorization") token: String,
//    @Query("search"
//    ) searchQuery: String? = null): Response<ProfileListResponse>

    @POST("api/subscription/upgrade")
    suspend fun upgradeToPremium(
        @Header("Authorization") token: String,
        @Body request: PinUpgradeRequest
    ): Response<SimpleMessageResponse>

//    Shows only users Uploaded Recipes
    @GET("api/recipes/my")
    suspend fun getMyRecipes(@Header("Authorization") token: String): Response<MyRecipeResponse>

//    Shows all the recipes from the mybackend
    @GET("api/recipeswoa")
    suspend fun getRecipeFeed(@Header("Authorization") token: String): Response<RecipeFeedResponse>
}


// ==========================================
// 2. THE MEALDB PUBLIC API
// Everything here goes to https://www.themealdb.com/api/json/v1/1/
// ==========================================
interface MealDbApi {

    @GET("lookup.php")
    suspend fun getDetailRecipe(@Query("i") mealId: String): Response<RecipeResponseDetails>

    @GET("categories.php")
    suspend fun getDiscoveryRecipe(): Response<RecipeResponseDiscovery>

    @GET("search.php")
    suspend fun getForYouRecipe(@Query("s") randomLetter2: String): Response<RecipeResponseForYou>

    @GET("search.php")
    suspend fun getPopularRecipe(@Query("s") randomLetter1: String): Response<RecipeResponsePopular>

    @GET("search.php")
    suspend fun getSearchRecipe(@Query("s") searchQuery: String): Response<RecipeResponseSearch>
}