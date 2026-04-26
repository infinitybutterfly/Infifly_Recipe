package com.infinitybutterfly.infiflyrecipe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.infinitybutterfly.infiflyrecipe.R
import com.infinitybutterfly.infiflyrecipe.models.Recipe

class RvAdapterMyRecipe(private var recipeList: List<Recipe>,
                        private val onItemClick: (Recipe) -> Unit,
                        private var onEditClick: (Recipe) -> Unit,
                        private val onDeleteClick: (Int) -> Unit) :
    RecyclerView.Adapter<RvAdapterMyRecipe.RecipeViewHolder>() {


    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipeImage: ImageView = view.findViewById(R.id.imageView2)
        val recipeName: TextView = view.findViewById(R.id.recipe_name_upload)
        val userName: TextView = view.findViewById(R.id.user_name_upload)
        val optionsBtn: ImageButton = view.findViewById(R.id.buttonViewOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.my_recipe_layout, parent, false)
        return RecipeViewHolder(view)
    }

    fun updateData(newRecipeList: List<Recipe>) {
        this.recipeList = newRecipeList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]

        holder.itemView.setOnClickListener {
            onItemClick(recipe)
        }

        holder.recipeName.text = recipe.name
        holder.userName.text = "Uploaded by: ${recipe.userName}"

        // Loading image using Glide
        Glide.with(holder.itemView.context)
            .load(recipe.imageUrl)
            .placeholder(R.drawable.demo_img)
            .into(holder.recipeImage)

        //FOLLOWING CODE FOR BOTTOMSHEET FOR OPTIONS MENU
        holder.optionsBtn.setOnClickListener { view ->

            val bottomSheetDialog = BottomSheetDialog(view.context)

            val bottomSheetView = LayoutInflater.from(view.context)
                .inflate(R.layout.my_recipe_options_bottomsheet_menu, null)

            bottomSheetDialog.setContentView(bottomSheetView)

            val editOption = bottomSheetView.findViewById<TextView>(R.id.tv_edit_recipe)
            val deleteOption = bottomSheetView.findViewById<TextView>(R.id.tv_delete_recipe)

            editOption.setOnClickListener {
                bottomSheetDialog.dismiss()
                onEditClick(recipe)
            }

            deleteOption.setOnClickListener {
                bottomSheetDialog.dismiss()
                onDeleteClick(recipe.id)
            }

            bottomSheetDialog.show()
        }

        //FOLLOWING CODE FOR JUST A SMALL OPTION MENU OPEN
//        holder.optionsBtn.setOnClickListener { view ->
////                val popup = androidx.appcompat.widget.PopupMenu(view.context, view)
//                val popup = PopupMenu(view.context, view)
//                // Inflate the menu resource
//                 popup.menuInflater.inflate(R.menu.upload_recipe_edit_menu, popup.menu)
//
//                // Manual Menu Creation (if you don't want to create an XML file)
////                popup.menu.add("Edit")
////                popup.menu.add("Delete")
//
//                // Set click listener for menu items
//                popup.setOnMenuItemClickListener { menuItem ->
//                    when (menuItem.title) {
//                        "Edit" -> {
//                            onEditClick(recipe)
//                            true
//                        }
//                        "Delete" -> {
//                            onDeleteClick(recipe.id)
//                            true
//                        }
//                        else -> false
//                    }
//                }
//                popup.show()
//        }
    }

    override fun getItemCount(): Int = recipeList.size
}