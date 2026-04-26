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
import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsDiscovery
import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsPopular

class RvAdapterDiscoveryRecipe(
    private var recipeList: List<RecipeItemsDiscovery>,
    private val onItemClick: (RecipeItemsDiscovery) -> Unit
) :
    RecyclerView.Adapter<RvAdapterDiscoveryRecipe.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<RecipeItemsDiscovery>) {
        this.recipeList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.discovery_recipe_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = recipeList[position]

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }

        Glide.with(holder.itemView.context)
            .load(currentItem.strCategoryThumb)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_background)
            )
            .into(holder.foodImage)

        holder.titleText.text = currentItem.strCategory
        holder.countryString.text = currentItem.strCategoryDescription
//        holder.idmel.text = currentItem.idMeal
    }

    override fun getItemCount(): Int {
        return recipeList.size
//        return recipeList.size -1
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val idmel: TextView = itemView.findViewById(R.id.idmealnumberpopular)
        val foodImage: ImageView = itemView.findViewById(R.id.discovery_recipe_dish_img)
        val titleText: TextView = itemView.findViewById(R.id.discovery_recipe_dish_name)
        val countryString: TextView = itemView.findViewById(R.id.discovery_recipe_country_name)
    }
}
