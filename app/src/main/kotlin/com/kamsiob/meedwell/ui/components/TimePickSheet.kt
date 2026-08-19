package com.kamsiob.meedwell.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow
import java.util.Calendar
import java.util.Date

/**
 * Picking an hour, in this app's own language.
 *
 * A sheet of half hours rather than a platform time picker. The system dialog is
 * a Material surface with its own elevation, corner radius and colour scheme,
 * and dropping one into this design would be the single loudest thing in the
 * app. It is also more machinery than the question needs: dawn is not a moment
 * anybody wants to set to the minute.
 *
 * The list opens already scrolled to whatever is set, so the current answer is
 * the first thing seen rather than something to hunt for.
 */
@Composable
fun TimePickSheet(
    title: String,
    note: String,
    minutes: List<Int>,
    selectedMinute: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(selectedMinute) {
        val index = minutes.indexOf(selectedMinute)
        if (index > 1) listState.scrollToItem(index - 1)
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
            Box(Modifier.height(18.dp))
            Text(title, style = type.h2, color = colors.primaryText)
            Text(
                note,
                style = type.meta,
                color = colors.tertiaryText,
                modifier = Modifier.padding(top = 6.dp),
            )

            LazyColumn(
                state = listState,
                // Tall enough to show several at once, short enough that the
                // sheet never becomes the whole screen.
                modifier = Modifier.heightIn(max = 320.dp).padding(top = 10.dp),
            ) {
                items(minutes, key = { it }) { minute ->
                    val selected = minute == selectedMinute
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable(role = Role.RadioButton) { onPick(minute) }
                            .padding(vertical = 12.dp)
                            .semantics {
                                contentDescription = clockLabelFor(context, minute)
                                this.selected = selected
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            clockLabelFor(context, minute),
                            style = type.rowTitle,
                            color = if (selected) colors.mossInk else colors.primaryText,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            MeedwellIcon(MeedwellIcons.Check, size = 15.dp, tint = colors.mossInk)
                        }
                    }
                }
            }
            Box(Modifier.height(22.dp))
        }
    }
}

/** The phone's own clock format, so the sheet never argues with the panel. */
internal fun clockLabelFor(context: android.content.Context, minuteOfDay: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
    }
    return DateFormat.getTimeFormat(context).format(Date(calendar.timeInMillis))
}

/** Half hours between two whole hours, inclusive. */
fun halfHoursBetween(fromHour: Int, toHour: Int): List<Int> =
    (fromHour * 60..toHour * 60 step 30).toList()
