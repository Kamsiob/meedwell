package com.kamsiob.meedwell.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow

/**
 * Where the sound goes: this phone, headphones, a speaker, a television.
 *
 * **Not a settings intent.** The first attempt opened
 * `android.settings.panel.action.MEDIA_OUTPUT`, which is documented but does not
 * resolve on every phone, and on the one being tested it resolved to nothing at
 * all. So the button appeared to do nothing while Bluetooth was connected, which
 * is the worst possible result: the control existed and lied.
 *
 * This asks the platform's own router for the routes it actually has and lets
 * one be chosen. Bluetooth devices, wired headphones and the phone's own speaker
 * all appear here because they are live audio routes. A Chromecast or a
 * television appears when its provider is installed and broadcasting, which is
 * the system's business rather than this app's to guess at.
 *
 * Styled as one of ours rather than borrowed: no filled rows, selection carried
 * by ink and a moss mark, the same way every other choice in this app is shown.
 */
@Composable
fun OutputSheet(onDismiss: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val context = LocalContext.current
    val router = remember { MediaRouter.getInstance(context.applicationContext) }
    val selector = remember {
        MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
            .build()
    }

    var routes by remember { mutableStateOf(router.routes.filter { it.matchesSelector(selector) }) }
    var selectedId by remember { mutableStateOf(router.selectedRoute.id) }

    // An active scan, and only while this sheet is open. Discovery costs battery
    // and there is no reason to be looking for televisions the rest of the time.
    DisposableEffect(Unit) {
        val callback = object : MediaRouter.Callback() {
            private fun refresh() {
                routes = router.routes.filter { it.matchesSelector(selector) }
                selectedId = router.selectedRoute.id
            }

            override fun onRouteAdded(r: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteRemoved(r: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteChanged(r: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteSelected(
                r: MediaRouter,
                route: MediaRouter.RouteInfo,
                reason: Int,
            ) = refresh()
        }
        router.addCallback(
            selector,
            callback,
            MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN,
        )
        onDispose { router.removeCallback(callback) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(role = Role.Button, onClick = onDismiss)
            .semantics { contentDescription = "Close" },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .sheetShadow()
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(horizontal = 26.dp),
        ) {
            SheetHandle(onDismiss = onDismiss, modifier = Modifier.padding(top = 12.dp))

            Text(
                "Where the sound goes",
                style = type.h2,
                color = colors.primaryText,
                modifier = Modifier.padding(top = 10.dp),
            )

            routes.forEach { route ->
                val on = route.id == selectedId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .clickable(role = Role.Button) {
                            router.selectRoute(route)
                            selectedId = route.id
                            onDismiss()
                        }
                        .padding(vertical = 13.dp)
                        .semantics {
                            contentDescription =
                                if (on) "${route.name}, in use" else "Send the sound to ${route.name}"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            route.name,
                            style = type.rowTitle,
                            color = if (on) colors.primaryText else colors.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        route.description?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = type.meta,
                                color = colors.tertiaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    if (on) {
                        Text("in use", style = type.plate, color = colors.mossInk)
                    }
                }
                Hairline()
            }

            if (routes.size <= 1) {
                Text(
                    "Nothing else is connected. Pair a speaker or headphones and they turn up here.",
                    style = type.meta,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            // The honest escape: everything the system knows, in the system's
            // own screen, for the cases a route provider does not advertise.
            Box(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .clickable(role = Role.Button) {
                        runCatching {
                            context.startActivity(
                                Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                            )
                        }
                        onDismiss()
                    }
                    .padding(top = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("Bluetooth settings ›", style = type.chip, color = colors.secondaryText)
            }

            Box(Modifier.height(22.dp))
        }
    }
}
