package com.infinitybutterfly.infiflyrecipe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.infinitybutterfly.infiflyrecipe.R
import com.infinitybutterfly.infiflyrecipe.models.FavFoodView

class AdapterFavFood(private val favfoodList: ArrayList<FavFoodView>) :
    RecyclerView.Adapter<AdapterFavFood.ViewHolder>() {

//    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val imageView: ImageView = itemView.findViewById(R.id.imageView)
//    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.icon_selection_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val imageUrl = "https://img.freepik.com/premium-photo/png-butterfly-animal-insect-flying_53876-933707.jpg"
//
//        Glide.with(holder.itemView.context)
//            .load(imageUrl)
//            .apply(
//                RequestOptions()
//                    .placeholder(R.drawable.ic_launcher_foreground)
//                    .error(R.drawable.ic_launcher_background)
//            )
//            .into(holder.profileImage)

//        val imageUrl = numberList[position]
//
//        // Use Glide to load the image
//        Glide.with(holder.itemView.context) // Use the context from the view holder's item view
//            .load(imageUrl) // Load the image URL
//            .apply(
//                RequestOptions()
//                    .placeholder(R.drawable.ic_launcher_foreground) // Optional: placeholder while loading
//                    .error(R.drawable.ic_launcher_background) // Optional: error image if loading fails
//            )
//            .into(holder.imageView) // Set the image into the ImageView

        val currentItem = favfoodList[position]

        with(holder) {
            profileImage.setImageResource(currentItem.profileImage)
            nameText.text = currentItem.nameText
        }

        if (currentItem.isSelected) {
            holder.selectedOverlay.visibility = View.VISIBLE
            holder.checkmark.visibility = View.VISIBLE
        } else {
            holder.selectedOverlay.visibility = View.GONE
            holder.checkmark.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            currentItem.isSelected = !currentItem.isSelected

            notifyItemChanged(position)
        }
    }


    override fun getItemCount(): Int {
        return favfoodList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.imgFood)
        val nameText: TextView = itemView.findViewById(R.id.tvFoodName)
        val selectedOverlay: View = itemView.findViewById(R.id.viewSelectedOverlay)
        val checkmark: ImageView = itemView.findViewById(R.id.imgCheckmark)
    }
    fun getSelectedFavFoods(): List<FavFoodView> {
        return favfoodList.filter { it.isSelected }
    }
}





//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//// import com.infinitybutterfly.infiflyrecipe.R
//// import com.infinitybutterfly.infiflyrecipe.models.FoodCategory
//
//class AdapterFavFoods(private val foodList: List<FoodCategory>) :
//    RecyclerView.Adapter<AdapterFavFoods.ViewHolder>() {
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val profileImage: ImageView = itemView.findViewById(R.id.imgFood)
//        val nameText: TextView = itemView.findViewById(R.id.tvFoodName)
//
//        // Find your selection views from icon_selection_layout.xml
//        val selectedOverlay: View = itemView.findViewById(R.id.viewSelectedOverlay)
//        val checkmark: ImageView = itemView.findViewById(R.id.imgCheckmark)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val itemView = LayoutInflater.from(parent.context)
//            .inflate(R.layout.icon_selection_layout, parent, false)
//        return ViewHolder(itemView)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val currentItem = foodList[position]
//
//        holder.nameText.text = currentItem.nameText
//
//        // Load image using Glide
//        Glide.with(holder.itemView.context)
//            .load(currentItem.imageUrl)
//            .placeholder(R.drawable.ic_launcher_foreground)
//            .error(R.drawable.ic_launcher_background)
//            .into(holder.profileImage)
//
//        // 1. Show or hide the checkmark/overlay based on the model's boolean
//        if (currentItem.isSelected) {
//            holder.selectedOverlay.visibility = View.VISIBLE
//            holder.checkmark.visibility = View.VISIBLE
//        } else {
//            holder.selectedOverlay.visibility = View.GONE
//            holder.checkmark.visibility = View.GONE
//        }
//
//        // 2. Handle the click event to toggle selection
//        holder.itemView.setOnClickListener {
//            // Flip the boolean (if true becomes false, if false becomes true)
//            currentItem.isSelected = !currentItem.isSelected
//
//            // Tell the adapter to refresh just this specific row to update the UI
//            notifyItemChanged(position)
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return foodList.size
//    }
//
//    // 3. Create a helper function to grab all selected items whenever you need them
//    fun getSelectedFoods(): List<FoodCategory> {
//        return foodList.filter { it.isSelected }
//    }
//}