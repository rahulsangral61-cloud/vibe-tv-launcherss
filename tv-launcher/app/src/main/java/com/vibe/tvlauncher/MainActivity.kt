package com.vibe.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vibe.tvlauncher.data.AppEntry
import com.vibe.tvlauncher.data.AppRepository
import com.vibe.tvlauncher.ui.HomeScreen
import com.vibe.tvlauncher.ui.LauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository by lazy { AppRepository(applicationContext) }
    private val prefs by lazy { getSharedPreferences("launcher", Context.MODE_PRIVATE) }

    private var apps by mutableStateOf<List<AppEntry>>(emptyList())
    private var favorites by mutableStateOf<Set<String>>(emptySet())

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favorites = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
        refreshApps()

        setContent {
            LauncherTheme {
                HomeScreen(
                    apps = apps,
                    favorites = favorites,
                    onLaunch = ::launchApp,
                    onTogglePin = ::togglePin,
                    onQuickAction = ::openSystemScreen,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        runCatching { unregisterReceiver(packageReceiver) }
        super.onStop()
    }

    private fun refreshApps() {
        lifecycleScope.launch { apps = repository.load() }
    }

    private fun launchApp(app: AppEntry) {
        try {
            startActivity(app.launchIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePin(app: AppEntry) {
        val updated = favorites.toMutableSet().apply {
            if (!add(app.packageName)) remove(app.packageName)
        }
        favorites = updated
        prefs.edit().putStringSet(KEY_FAVORITES, updated).apply()
    }

    private fun openSystemScreen(action: String) {
        try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(this, "Not available on this TV", Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val KEY_FAVORITES = "favorites"
    }
}
