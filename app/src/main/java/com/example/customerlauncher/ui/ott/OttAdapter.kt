package com.example.customerlauncher.ui.ott

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.customerlauncher.R

class OttAdapter(
    private val apps: List<ResolveInfo>,
    private val pm: PackageManager,
    private val onClick: (ResolveInfo) -> Unit,
    private val onLongClick: (ResolveInfo) -> Boolean
) : RecyclerView.Adapter<OttAdapter.OttViewHolder>() {

    inner class OttViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OttViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ott_app, parent, false)
        return OttViewHolder(view)
    }

    override fun onBindViewHolder(holder: OttViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.loadIcon(pm))

        holder.itemView.setOnClickListener { onClick(app) }
        holder.itemView.setOnLongClickListener { onLongClick(app) }

        // 포커스 애니메이션
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.isSelected = hasFocus // selector 작동
            v.animate()
                .scaleX(if (hasFocus) 1.05f else 1.0f)
                .scaleY(if (hasFocus) 1.05f else 1.0f)
                .setDuration(150)
                .start()
        }
    }

    override fun getItemCount(): Int = apps.size
}
