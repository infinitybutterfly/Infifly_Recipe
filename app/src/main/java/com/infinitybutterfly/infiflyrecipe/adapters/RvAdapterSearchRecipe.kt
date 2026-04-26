package com.infinitybutterfly.infiflyrecipe.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.infinitybutterfly.infiflyrecipe.R
import com.infinitybutterfly.infiflyrecipe.models.UnifiedRecipeItem // IMPORT UNIFIED ITEM
import com.infinitybutterfly.infiflyrecipe.models.UserProfileItems

class RvAdapterSearchRecipe(
    private var recipeList: List<UnifiedRecipeItem>,
    private val onItemClick: (UnifiedRecipeItem) -> Unit
) : RecyclerView.Adapter<RvAdapterSearchRecipe.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<UnifiedRecipeItem>) {
        this.recipeList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_recipe_layout, parent, false)
//            .inflate(R.layout.my_recipe_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = recipeList[position]

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }

        if (currentItem.isFromMyBackend) {
            // It's from Ktor! Show the premium icon.
            holder.premiumIcon.visibility = View.VISIBLE
        } else {
            // It's from MealDB! Hide the premium icon.
            holder.premiumIcon.visibility = View.GONE
        }

        val safeImageUrl = currentItem.imageUrl ?: ""

        if (safeImageUrl.startsWith("http")) {
        Glide.with(holder.itemView.context)
            .load(currentItem.imageUrl) // NOW USING imageUrl
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_background)
            )
            .into(holder.foodImage)
        } else {
            // It's bad data (like "gg"). Skip Glide entirely and just show the error image!
            holder.foodImage.setImageResource(R.drawable.outline_account_circle_24)
        }

        holder.titleText.text = currentItem.title // NOW USING title
        holder.idmelString.text = currentItem.id // NOW USING id

        // Note: I left country blank because MealDB doesn't return country on standard searches
        // and our Ktor mapping doesn't have it yet. You can hide it or update it later!
        holder.countryString.text = currentItem.country
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val idmelString: TextView = itemView.findViewById(R.id.idmealnumbersearch)
        val foodImage: ImageView = itemView.findViewById(R.id.search_recipe_dish_img)
        val titleText: TextView = itemView.findViewById(R.id.search_recipe_dish_name)
        val countryString: TextView = itemView.findViewById(R.id.search_recipe_country_name)
        val premiumIcon: ImageView = itemView.findViewById(R.id.premium_icon_search_result)
    }
}



//package com.infinitybutterfly.infiflyrecipe.adapters
//
//import android.annotation.SuppressLint
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.bumptech.glide.request.RequestOptions
//import com.infinitybutterfly.infiflyrecipe.R
//import com.infinitybutterfly.infiflyrecipe.models.UnifiedRecipeItem
//
//class RvAdapterSearchRecipe(
//    private var recipeList: List<UnifiedRecipeItem>,
//    private val onItemClick: (UnifiedRecipeItem) -> Unit
//) :
//    RecyclerView.Adapter<RvAdapterSearchRecipe.ViewHolder>() {
//
//    @SuppressLint("NotifyDataSetChanged")
//    fun updateData(newList: List<UnifiedRecipeItem>) {
//        this.recipeList = newList
//        notifyDataSetChanged()
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val itemView = LayoutInflater.from(parent.context)
//            .inflate(R.layout.search_recipe_layout, parent, false)
//        return ViewHolder(itemView)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val currentItem = recipeList[position]
//
//        holder.itemView.setOnClickListener {
//            onItemClick(currentItem)
//        }
//
//        Glide.with(holder.itemView.context)
//            .load(currentItem.strMealThumb)
//            .apply(
//                RequestOptions()
//                    .placeholder(R.drawable.ic_launcher_foreground)
//                    .error(R.drawable.ic_launcher_background)
//            )
//            .into(holder.foodImage)
//
//        holder.titleText.text = currentItem.strMeal
//        holder.countryString.text = currentItem.strArea
//        holder.idmelString.text = currentItem.idMeal
//
//    }
//
//    override fun getItemCount(): Int {
//        return recipeList.size
//    }
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val idmelString: TextView = itemView.findViewById(R.id.idmealnumbersearch)
//        val foodImage: ImageView = itemView.findViewById(R.id.search_recipe_dish_img)
//        val titleText: TextView = itemView.findViewById(R.id.search_recipe_dish_name)
//        val countryString: TextView = itemView.findViewById(R.id.search_recipe_country_name)
//    }
//}
