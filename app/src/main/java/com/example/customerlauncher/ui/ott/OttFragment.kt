package com.example.customerlauncher.ui.ott

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
import com.example.customerlauncher.R
import com.example.customerlauncher.ui.common.GridSpacingItemDecoration


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
        val spanCount = 5
        val spacing = 12 // dp → px 변환 필요시 아래 참고
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)
        recyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing, includeEdge = false))

        val pm = requireActivity().packageManager
        val apps = getInstalledOttApps(pm)

        recyclerView.adapter = OttAdapter(apps, pm,
            onClick = { app ->
                val intent = pm.getLaunchIntentForPackage(app.activityInfo.packageName)
                intent?.let { startActivity(it) }
            },
            onLongClick = { app ->
                val pkgName = app.activityInfo.packageName
                val isFavorite = sharedPreferences.getBoolean(pkgName, false)
                sharedPreferences.edit().putBoolean(pkgName, !isFavorite).apply()
                true
            }
        )
    }

    private fun getInstalledOttApps(pm: PackageManager): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val allApps = pm.queryIntentActivities(intent, 0)

        val ottKeywords =listOf(
            "netflix", "youtube", "pooq", "wavve", "웨이브",
            "tving", "watcha", "coupang", "disney", "prime", "espn"
        )

        return allApps.filter {
            val label = it.loadLabel(pm).toString().lowercase()
            ottKeywords.any { keyword -> label.contains(keyword) }
        }
    }
}