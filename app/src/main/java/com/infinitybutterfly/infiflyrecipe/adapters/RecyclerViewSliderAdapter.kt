package com.infinitybutterfly.infiflyrecipe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.infinitybutterfly.infiflyrecipe.R
import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsPopular

class RecyclerViewSliderAdapter(
    private var recipeList: List<RecipeItemsPopular>,
    private val onItemClick: (RecipeItemsPopular) -> Unit
) : RecyclerView.Adapter<RecyclerViewSliderAdapter.SliderViewHolder>() {

    // This function allows the fragment to update the data!
    fun updateData(newList: List<RecipeItemsPopular>) {
        this.recipeList = newList
        notifyDataSetChanged()
    }

    class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // FIXED: Using the actual IDs from your popular_recipe_layout.xml
        val imageView: ImageView = itemView.findViewById(R.id.popular_recipe_dish_img)
        val titleText: TextView = itemView.findViewById(R.id.popular_recipe_dish_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.popular_recipe_layout, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val currentItem = recipeList[position]

        // Load image using Glide
        Glide.with(holder.itemView.context)
            .load(currentItem.strMealThumb)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_background)
            )
            .into(holder.imageView)

        // Set the recipe title
        holder.titleText.text = currentItem.strMeal

        // Handle item clicks
        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }
}


//package com.infinitybutterfly.infiflyrecipe.adapters
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.bumptech.glide.request.RequestOptions
//import com.infinitybutterfly.infiflyrecipe.R
//import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsPopular // Use your actual model
//
//class RecyclerViewSliderAdapter(
//    private var recipeList: List<RecipeItemsPopular>,
//    private val onItemClick: (RecipeItemsPopular) -> Unit
//) : RecyclerView.Adapter<RecyclerViewSliderAdapter.SliderViewHolder>() {
//
//    fun updateData(newList: List<RecipeItemsPopular>) {
//        this.recipeList = newList
//        notifyDataSetChanged()
//    }
//
//    class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val imageView: ImageView = itemView.findViewById(R.id.popular_recipe_dish_img)
//        val titleText: TextView = itemView.findViewById(R.id.popular_recipe_dish_name)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.popular_recipe_layout, parent, false)
//        return SliderViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
//        val currentItem = recipeList[position]
//
//        // Load the image
//        Glide.with(holder.itemView.context)
//            .load(currentItem.strMealThumb)
//            .apply(
//                RequestOptions()
//                    .placeholder(R.drawable.ic_launcher_foreground)
//                    .error(R.drawable.ic_launcher_background)
//            )
//            .into(holder.imageView)
//
//        // Set the text
//        holder.titleText.text = currentItem.strMeal
//
//        // Handle clicks
//        holder.itemView.setOnClickListener {
//            onItemClick(currentItem)
//        }
//    }
//
//    override fun getItemCount(): Int = recipeList.size
//}