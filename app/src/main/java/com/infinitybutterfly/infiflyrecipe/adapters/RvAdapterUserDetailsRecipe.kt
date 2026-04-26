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
import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterDetailsRecipe.ViewHolder
import com.infinitybutterfly.infiflyrecipe.models.Recipe
import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsDetails
import com.infinitybutterfly.infiflyrecipe.models.RecipeResponse
import com.infinitybutterfly.infiflyrecipe.models.RecipeSearchResponseId

class RvAdapterUserDetailsRecipe(
    private var recipeList: List<RecipeSearchResponseId>
):
    RecyclerView.Adapter<RvAdapterUserDetailsRecipe.ViewHolder>() {
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<RecipeSearchResponseId>) {
        this.recipeList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.detail_server_id_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = recipeList[position]

        Glide.with(holder.itemView.context)
            .load(currentItem.imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_background)
            )
            .into(holder.foodImage)

//        Dish Name
        holder.titleText.text = currentItem.name

//        Country Name
        holder.countryString.text = currentItem.country

//        Ingredients
        holder.ingre1String.text = currentItem.ingredientsName

//        Measurements - Quantity
        holder.quant1String.text = currentItem.ingredientsQuantity

//        Instructions
        holder.instructString.text = currentItem.instructions

//        ID Meal
        holder.idmelSting.text = currentItem.id.toString()
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val idmelSting: TextView = itemView.findViewById(R.id.idnumberdetailuser)
        val foodImage: ImageView = itemView.findViewById(R.id.selected_recipe_user_details_img_id)
        val titleText: TextView = itemView.findViewById(R.id.selected_recipe_details_dish_name_id)
        val countryString: TextView = itemView.findViewById(R.id.selected_recipe_details_country_name_id)
        val ingre1String: TextView = itemView.findViewById(R.id.ingre_name_id)
        val quant1String: TextView = itemView.findViewById(R.id.quant_name_id)
        val instructString: TextView = itemView.findViewById(R.id.instruct_id)
    }

}