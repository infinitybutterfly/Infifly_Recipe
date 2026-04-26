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
import com.infinitybutterfly.infiflyrecipe.models.AllergyView
import com.infinitybutterfly.infiflyrecipe.models.FavFoodView

class AdapterAllergy(private val allergyList: ArrayList<AllergyView>) :
    RecyclerView.Adapter<AdapterAllergy.ViewHolder>() {

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

        val currentItem = allergyList[position]

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
        return allergyList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.imgFood)
        val nameText: TextView = itemView.findViewById(R.id.tvFoodName)
        val selectedOverlay: View = itemView.findViewById(R.id.viewSelectedOverlay)
        val checkmark: ImageView = itemView.findViewById(R.id.imgCheckmark)
    }
    fun getSelectedAllergyFoods(): List<AllergyView> {
        return allergyList.filter { it.isSelected }
    }
}
