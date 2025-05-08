package com.example.customerlauncher.ui

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
    private val packageManager: PackageManager,
    private val onClick: (ResolveInfo) -> Unit
) : RecyclerView.Adapter<OttAdapter.ViewHolder>() {

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ott_app, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.loadIcon(packageManager))
        holder.name.text = app.loadLabel(packageManager)
        holder.view.setOnClickListener { onClick(app) }
    }

    override fun getItemCount() = apps.size
}
