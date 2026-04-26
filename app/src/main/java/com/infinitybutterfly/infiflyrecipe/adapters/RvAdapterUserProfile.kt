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
import com.infinitybutterfly.infiflyrecipe.models.UserProfile
import com.infinitybutterfly.infiflyrecipe.models.UserProfileItems
import com.infinitybutterfly.infiflyrecipe.models.UserProfiles

class RvAdapterUserProfile(
    private var profiles: List<UserProfiles>,
    private val onItemClick: (UserProfiles) -> Unit
) : RecyclerView.Adapter<RvAdapterUserProfile.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<UserProfiles>) {
        this.profiles = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_profile_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles[position]
        holder.username.text = profile.username
        holder.name.text = profile.name
        holder.countryname.text = profile.country

        Glide.with(holder.itemView.context)
            .load(profile.profilePicUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_background)
            )
//            .placeholder(R.drawable.ic_launcher_foreground)
            .into(holder.profileImg)

        holder.itemView.setOnClickListener { onItemClick(profile) }
    }

    override fun getItemCount() = profiles.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImg: ImageView = view.findViewById(R.id.search_profile_user_img) // Use your profile layout IDs
        val username: TextView = view.findViewById(R.id.profile_user_name)
        val name: TextView = view.findViewById(R.id.profile__name)
        val countryname: TextView = view.findViewById(R.id.profile_country_name)
    }
}