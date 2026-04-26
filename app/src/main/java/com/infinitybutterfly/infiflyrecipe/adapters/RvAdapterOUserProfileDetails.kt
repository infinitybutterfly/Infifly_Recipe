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
import com.infinitybutterfly.infiflyrecipe.models.RecipeSummary

class RvAdapterOUserProfileDetails(
    private var recipeList: List<RecipeSummary>,
    private val onItemClick: (RecipeSummary) -> Unit
) : RecyclerView.Adapter<RvAdapterOUserProfileDetails.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<RecipeSummary>) {
        this.recipeList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recipe_for_you_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = recipeList[position]

        holder.titleText.text = currentItem.title
        holder.countryText.text = currentItem.countryName

        // Hide the ID text just in case, but assign it
        holder.idText.text = currentItem.id
        holder.idText.visibility = View.GONE

        // Load the image safely
        val imageUrl = currentItem.imageUrl ?: ""
        if (imageUrl.startsWith("http")) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_background)
                )
                .into(holder.recipeImage)
        } else {
            holder.recipeImage.setImageResource(R.drawable.ic_launcher_background)
        }

        // Handle clicks
        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }
    }

    override fun getItemCount(): Int = recipeList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.for_you_recipe_dish_img)
        val titleText: TextView = itemView.findViewById(R.id.for_you_recipe_dish_name)
        val countryText: TextView = itemView.findViewById(R.id.for_you_recipe_country_name)
        val idText: TextView = itemView.findViewById(R.id.idmealnumberforyou)
    }
}


//package com.infinitybutterfly.infiflyrecipe.adapters
//
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import com.infinitybutterfly.infiflyrecipe.models.Recipe
//
////class RvAdapterOUserProfileDetails (
////    private val recipeList: List<Recipe>,
////    private val onListChanged: (Int) -> Unit // Pass the size back
////    ) : RecyclerView.Adapter<RvAdapterOUserProfileDetails.ViewHolder>() {
////    override fun onCreateViewHolder(
////        p0: ViewGroup,
////        p1: Int
////    ): RvAdapterOUserProfileDetails.ViewHolder {
////        TODO("Not yet implemented")
////    }
////
////    override fun onBindViewHolder(p0: RvAdapterOUserProfileDetails.ViewHolder, p1: Int) {
////        TODO("Not yet implemented")
////    }
////
////    override fun getItemCount(): Int {
////            val size = recipeList.size
////            onListChanged(size) // Call the update whenever the count is requested
////            return size
////        }
////    }
