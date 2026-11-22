@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.vibe.tvlauncher.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.vibe.tvlauncher.data.AppEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val SideGutter = 56.dp

private data class QuickAction(val label: String, val action: String)

private val quickActions = listOf(
    QuickAction("Settings", Settings.ACTION_SETTINGS),
    QuickAction("Wi-Fi", Settings.ACTION_WIFI_SETTINGS),
    QuickAction("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS),
    QuickAction("Display", Settings.ACTION_DISPLAY_SETTINGS),
)

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    favorites: Set<String>,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    onQuickAction: (String) -> Unit,
) {
    // A launcher is the bottom of the stack: Back must never leave it.
    BackHandler(enabled = true) { }

    var focused by remember { mutableStateOf<AppEntry?>(null) }
    val accent by animateColorAsState(
        targetValue = focused?.accent ?: Color(0xFF2C2C2E),
        animationSpec = tween(durationMillis = 700),
        label = "accent",
    )

    val favApps = remember(apps, favorites) { apps.filter { it.packageName in favorites } }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) runCatching { firstFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Colour wash that follows the focused app
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.70f),
                            accent.copy(alpha = 0.20f),
                            Color.Black,
                        ),
                    ),
                ),
        )

        Column(Modifier.fillMaxSize().padding(top = 32.dp, bottom = 24.dp)) {
            TopBar()
            Hero(focused, Modifier.weight(1f))

            if (favApps.isNotEmpty()) {
                SectionTitle("Favourites")
                AppRow(
                    apps = favApps,
                    favorites = favorites,
                    firstFocus = firstFocus,
                    onFocus = { focused = it },
                    onLaunch = onLaunch,
                    onTogglePin = onTogglePin,
                )
            }

            SectionTitle("Apps")
            AppRow(
                apps = apps,
                favorites = favorites,
                firstFocus = if (favApps.isEmpty()) firstFocus else null,
                onFocus = { focused = it },
                onLaunch = onLaunch,
                onTogglePin = onTogglePin,
            )

            Spacer(Modifier.height(20.dp))
            QuickActions(onQuickAction)
        }
    }
}

@Composable
private fun TopBar() {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(15_000)
        }
    }
    val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)

    Row(
        Modifier.fillMaxWidth().padding(horizontal = SideGutter),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(greeting, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(date, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
        }
        Text(time, fontSize = 30.sp, fontWeight = FontWeight.Light, color = Color.White)
    }
}

@Composable
private fun Hero(focused: AppEntry?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = SideGutter)) {
        Crossfade(
            targetState = focused,
            animationSpec = tween(400),
            label = "hero",
            modifier = Modifier.align(Alignment.BottomStart),
        ) { app ->
            if (app != null) {
                Column(Modifier.padding(bottom = 20.dp)) {
                    Text(
                        app.label,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Press OK to open  ·  Hold OK to pin to Favourites",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            } else {
                Spacer(Modifier.height(1.dp))
            }
        }

        // Soft blurred backdrop of the focused icon (blur is a no-op below Android 12)
        Crossfade(
            targetState = focused,
            animationSpec = tween(500),
            label = "heroIcon",
            modifier = Modifier.align(Alignment.CenterEnd),
        ) { app ->
            if (app != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(200.dp).alpha(0.55f).blur(36.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = SideGutter, top = 8.dp, bottom = 10.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.85f),
    )
}

@Composable
private fun AppRow(
    apps: List<AppEntry>,
    favorites: Set<String>,
    firstFocus: FocusRequester?,
    onFocus: (AppEntry) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SideGutter, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            AppCard(
                app = app,
                pinned = app.packageName in favorites,
                modifier = if (index == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier,
                onFocus = { onFocus(app) },
                onClick = { onLaunch(app) },
                onLongClick = { onTogglePin(app) },
            )
        }
    }
}

@Composable
private fun AppCard(
    app: AppEntry,
    pinned: Boolean,
    modifier: Modifier,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .size(width = 208.dp, height = 117.dp)
            .onFocusChanged { if (it.isFocused || it.hasFocus) onFocus() },
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF2C2C2E),
            focusedContainerColor = Color(0xFF2C2C2E),
            pressedContainerColor = Color(0xFF2C2C2E),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape),
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevationColor = app.accent.copy(alpha = 0.6f), elevation = 18.dp),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (app.banner != null) {
                Image(
                    bitmap = app.banner,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(app.accent.copy(alpha = 0.9f), app.accent.copy(alpha = 0.35f))),
                        ),
                )
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.align(Alignment.Center).size(68.dp),
                )
            }
            if (pinned) {
                Text(
                    "★",
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    fontSize = 16.sp,
                    color = Color(0xFFFFD60A),
                )
            }
        }
    }
}

@Composable
private fun QuickActions(onQuickAction: (String) -> Unit) {
    Row(
        Modifier.padding(horizontal = SideGutter),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        quickActions.forEach { qa ->
            Surface(
                onClick = { onQuickAction(qa.action) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
            ) {
                Text(
                    qa.label,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
