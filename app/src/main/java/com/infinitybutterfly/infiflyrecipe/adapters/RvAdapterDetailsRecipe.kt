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
import com.infinitybutterfly.infiflyrecipe.models.RecipeItemsDetails

class RvAdapterDetailsRecipe(
    private var recipeList: List<RecipeItemsDetails>
) :
    RecyclerView.Adapter<RvAdapterDetailsRecipe.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<RecipeItemsDetails>) {
        this.recipeList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recipe_details_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = recipeList[position]

        Glide.with(holder.itemView.context)
            .load(currentItem.strMealThumb)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_background)
            )
            .into(holder.foodImage)

//        Dish Name
        holder.titleText.text = currentItem.strMeal

//        Country Name
        holder.countryString.text = currentItem.strArea

        // --- INGREDIENTS (Dynamically hide if empty) ---
        holder.ingre1String.setTextOrHide(currentItem.strIngredient1)
        holder.ingre2String.setTextOrHide(currentItem.strIngredient2)
        holder.ingre3String.setTextOrHide(currentItem.strIngredient3)
        holder.ingre4String.setTextOrHide(currentItem.strIngredient4)
        holder.ingre5String.setTextOrHide(currentItem.strIngredient5)
        holder.ingre6String.setTextOrHide(currentItem.strIngredient6)
        holder.ingre7String.setTextOrHide(currentItem.strIngredient7)
        holder.ingre8String.setTextOrHide(currentItem.strIngredient8)
        holder.ingre9String.setTextOrHide(currentItem.strIngredient9)
        holder.ingre10String.setTextOrHide(currentItem.strIngredient10)
        holder.ingre11String.setTextOrHide(currentItem.strIngredient11)

        // --- MEASUREMENTS - QUANTITY (Dynamically hide if empty) ---
        holder.quant1String.setTextOrHide(currentItem.strMeasure1)
        holder.quant2String.setTextOrHide(currentItem.strMeasure2)
        holder.quant3String.setTextOrHide(currentItem.strMeasure3)
        holder.quant4String.setTextOrHide(currentItem.strMeasure4)
        holder.quant5String.setTextOrHide(currentItem.strMeasure5)
        holder.quant6String.setTextOrHide(currentItem.strMeasure6)
        holder.quant7String.setTextOrHide(currentItem.strMeasure7)
        holder.quant8String.setTextOrHide(currentItem.strMeasure8)
        holder.quant9String.setTextOrHide(currentItem.strMeasure9)
        holder.quant10String.setTextOrHide(currentItem.strMeasure10)
        holder.quant11String.setTextOrHide(currentItem.strMeasure11)

        //Normal, will show everthing even if the strings are empty
////        Ingredients
//        holder.ingre1String.text = currentItem.strIngredient1
//        holder.ingre2String.text = currentItem.strIngredient2
//        holder.ingre3String.text = currentItem.strIngredient3
//        holder.ingre4String.text = currentItem.strIngredient4
//        holder.ingre5String.text = currentItem.strIngredient5
//        holder.ingre6String.text = currentItem.strIngredient6
//        holder.ingre7String.text = currentItem.strIngredient7
//        holder.ingre8String.text = currentItem.strIngredient8
//        holder.ingre9String.text = currentItem.strIngredient9
//        holder.ingre10String.text = currentItem.strIngredient10
//        holder.ingre11String.text = currentItem.strIngredient11
//
////        Measurements - Quantity
//        holder.quant1String.text = currentItem.strMeasure1
//        holder.quant2String.text = currentItem.strMeasure2
//        holder.quant3String.text = currentItem.strMeasure3
//        holder.quant4String.text = currentItem.strMeasure4
//        holder.quant5String.text = currentItem.strMeasure5
//        holder.quant6String.text = currentItem.strMeasure6
//        holder.quant7String.text = currentItem.strMeasure7
//        holder.quant8String.text = currentItem.strMeasure8
//        holder.quant9String.text = currentItem.strMeasure9
//        holder.quant10String.text = currentItem.strMeasure10
//        holder.quant11String.text = currentItem.strMeasure11

//        Instructions
        holder.instructString.text = currentItem.strInstructions

//        ID Meal
        holder.idmelSting.text = currentItem.idMeal
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val idmelSting: TextView = itemView.findViewById(R.id.idmealnumberdetail)
        val foodImage: ImageView = itemView.findViewById(R.id.selected_recipe_details_img)
        val titleText: TextView = itemView.findViewById(R.id.selected_recipe_details_dish_name)
        val countryString: TextView = itemView.findViewById(R.id.selected_recipe_details_country_name)
        val ingre1String: TextView = itemView.findViewById(R.id.ingre_1)
        val ingre2String: TextView = itemView.findViewById(R.id.ingre_2)
        val ingre3String: TextView = itemView.findViewById(R.id.ingre_3)
        val ingre4String: TextView = itemView.findViewById(R.id.ingre_4)
        val ingre5String: TextView = itemView.findViewById(R.id.ingre_5)
        val ingre6String: TextView = itemView.findViewById(R.id.ingre_6)
        val ingre7String: TextView = itemView.findViewById(R.id.ingre_7)
        val ingre8String: TextView = itemView.findViewById(R.id.ingre_8)
        val ingre9String: TextView = itemView.findViewById(R.id.ingre_9)
        val ingre10String: TextView = itemView.findViewById(R.id.ingre_10)
        val ingre11String: TextView = itemView.findViewById(R.id.ingre_11)
        val quant1String: TextView = itemView.findViewById(R.id.quant_1)
        val quant2String: TextView = itemView.findViewById(R.id.quant_2)
        val quant3String: TextView = itemView.findViewById(R.id.quant_3)
        val quant4String: TextView = itemView.findViewById(R.id.quant_4)
        val quant5String: TextView = itemView.findViewById(R.id.quant_5)
        val quant6String: TextView = itemView.findViewById(R.id.quant_6)
        val quant7String: TextView = itemView.findViewById(R.id.quant_7)
        val quant8String: TextView = itemView.findViewById(R.id.quant_8)
        val quant9String: TextView = itemView.findViewById(R.id.quant_9)
        val quant10String: TextView = itemView.findViewById(R.id.quant_10)
        val quant11String: TextView = itemView.findViewById(R.id.quant_11)
        val instructString: TextView = itemView.findViewById(R.id.instruct)
    }
    private fun TextView.setTextOrHide(value: String?) {
        if (value.isNullOrBlank()) {
            this.visibility = View.GONE
        } else {
            this.visibility = View.VISIBLE
            this.text = value
        }
    }
}
