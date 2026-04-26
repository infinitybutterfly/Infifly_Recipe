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
import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsForYou
import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsPopular

class RvAdapterForYouRecipe(
    private var recipeList: List<RecipeItemsForYou>,
    private val onItemClick: (RecipeItemsForYou) -> Unit

) :
    RecyclerView.Adapter<RvAdapterForYouRecipe.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<RecipeItemsForYou>) {
        this.recipeList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recipe_for_you_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = recipeList[position]

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }

        Glide.with(holder.itemView.context)
            .load(currentItem.strMealThumb)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_background)
            )
            .into(holder.foodImage)

        holder.titleText.text = currentItem.strMeal
        holder.countryString.text = currentItem.strArea
        holder.idmel.text = currentItem.idMeal
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val idmel: TextView = itemView.findViewById(R.id.idmealnumberforyou)
        val foodImage: ImageView = itemView.findViewById(R.id.for_you_recipe_dish_img)
        val titleText: TextView = itemView.findViewById(R.id.for_you_recipe_dish_name)
        val countryString: TextView = itemView.findViewById(R.id.for_you_recipe_country_name)
    }
}
