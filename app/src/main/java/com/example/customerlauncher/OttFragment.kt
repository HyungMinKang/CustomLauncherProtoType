package com.example.customerlauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customerlauncher.ui.OttAdapter


class OttFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ott, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.ottRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 4)

        val pm = requireActivity().packageManager
        val apps = getInstalledOttApps(pm)

        recyclerView.adapter = OttAdapter(apps, pm) { app ->
            val intent = pm.getLaunchIntentForPackage(app.activityInfo.packageName)
            intent?.let { startActivity(it) }
        }
    }

    private fun getInstalledOttApps(pm: PackageManager): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val allApps = pm.queryIntentActivities(intent, 0)

        val ottPackages = listOf(
            "com.netflix.ninja",
            "com.google.android.youtube.tv",
            "com.google.android.youtube.tvmusic"
        )

        return allApps.filter { it.activityInfo.packageName in ottPackages }
    }
}