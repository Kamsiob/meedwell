package com.kamsiob.meedwell.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import com.kamsiob.meedwell.ui.theme.Motion
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.playback.PlaybackState
import com.kamsiob.meedwell.ui.components.ContourScrubber
import com.kamsiob.meedwell.ui.components.Cover
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.SurroundingsCardItem
import com.kamsiob.meedwell.playback.RepeatMode
import com.kamsiob.meedwell.ui.components.FieldPlate
import com.kamsiob.meedwell.ui.components.LevelLine
import com.kamsiob.meedwell.ui.components.SunMark
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.PageEdge
import com.kamsiob.meedwell.ui.components.PageMarks
import com.kamsiob.meedwell.ui.components.PlayerPage
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.Spacing
import kotlin.math.abs

/**
 * The player, as a **two-page horizontal spread**.
 *
 * Music on the left, Surroundings on the right, one swipe apart. This is the
 * single biggest structural correction from the grid: the player was a
 * now-playing screen with Surroundings buried three taps away under More, and
 * the design has them as two pages of one open book.
 *
 * What makes it a spread rather than a pager:
 *
 *  - **Page marks in the app's own iconography**, the copper coin for music and
 *    the sun for Surroundings, inked with a moss underline on the page you are
 *    on and outlined at 30% ink on the other. Not dots.
 *  - **A seven pixel sliver of the facing page** at the screen edge with a
 *    hairline. Without it, the swipe is undiscoverable.
 *  - The Surroundings mark **lights up whenever a sound is running**, even from
 *    the music page, so you can tell rain is playing without leaving the page
 *    you are on.
 *
 * The two pages are recomposed on each turn by the AnimatedContent that
 * choreographs the arrival, which is a deliberate trade: the parts arriving in
 * order are the experience the owner asked for, and they are re-set each turn.
 */
@Composable
fun PlayerSpread(
    page: PlayerPage,
    onPageChange: (PlayerPage) -> Unit,
    state: PlaybackState,
    surroundings: SurroundingsPlayingState,
    onCollapse: () -> Unit,
    onMenu: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenArtwork: () -> Unit,
    onOpenQueue: () -> Unit,
    onLove: () -> Unit,
    onSleepTimer: () -> Unit,
    onTone: () -> Unit,
    onSurroundingsPlayPause: () -> Unit,
    onSurroundingsVolume: (Float) -> Unit,
    onSurroundingsCredit: () -> Unit,
    onBrowseSurroundings: () -> Unit,
    onPickSurroundings: (String) -> Unit,
    onSurroundingsStop: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onOutput: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val haptics = LocalHapticFeedback.current

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(page) {
                var travelled = 0f
                detectHorizontalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragEnd = {
                        // A deliberate threshold, so a slightly imprecise tap
                        // on the artwork never turns the page.
                        if (abs(travelled) >= SWIPE_THRESHOLD_PX) {
                            onPageChange(
                                if (travelled < 0) PlayerPage.Surroundings else PlayerPage.Music
                            )
                        }
                    },
                ) { _, delta ->
                    val was = abs(travelled) >= SWIPE_THRESHOLD_PX
                    travelled += delta
                    // **The gesture confirms itself before the finger lifts.**
                    // A swipe whose result only appears on release feels like it
                    // failed even when it worked, which is exactly what was
                    // reported. One tick at the threshold, mid drag.
                    if (!was && abs(travelled) >= SWIPE_THRESHOLD_PX) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
            .semantics {
                // The swipe has a named equivalent, because a gesture with no
                // alternative is a feature a TalkBack user does not have.
                customActions = listOf(
                    CustomAccessibilityAction("Surroundings page") {
                        onPageChange(PlayerPage.Surroundings); true
                    },
                    CustomAccessibilityAction("Music page") {
                        onPageChange(PlayerPage.Music); true
                    },
                )
            }
    ) {
        // **It scrolls.**
        //
        // It did not, and on a 360 by 640 phone the sum of header, cover,
        // titles, scrubber and transport ran past the viewport: the play button
        // measured 56 by zero and simply was not there. The one test device
        // barely fits, which is why it went unseen. In landscape the cover alone
        // was taller than the screen.
        // **Pull it down to put it away.**
        //
        // The only way out was a chevron in the top left, which on a tall phone
        // means letting go of the thing you are holding and reaching. Every
        // full screen player on this platform closes by being pulled down, and
        // this one has to as well.
        //
        // Done through nested scroll rather than a raw drag, so it never fights
        // the page's own scrolling: the pull only counts once the content is
        // already at the top and the finger is still going down. Anything
        // upward resets it, so a scroll back up never half-arms the gesture.
        val scrollState = rememberScrollState()
        var pulled by remember { mutableFloatStateOf(0f) }
        val pullToClose = remember(scrollState) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < 0f) pulled = 0f
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y > 0f && scrollState.value == 0) {
                        pulled += available.y
                        if (pulled > PULL_TO_CLOSE_PX) {
                            pulled = 0f
                            onCollapse()
                        }
                    }
                    return Offset.Zero
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .nestedScroll(pullToClose)
                .verticalScroll(scrollState)
                .padding(horizontal = Spacing.gutter)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainIcon(MeedwellIcons.ChevronDown, "Close the player", onCollapse)
                Box(Modifier.weight(1f))
                PlainIcon(MeedwellIcons.Dots, "More actions", onMenu)
            }

            PageMarks(
                page = page,
                surroundingsPlaying = surroundings.isPlaying,
                onSelect = onPageChange,
                modifier = Modifier.padding(top = 12.dp),
            )

            // **The turn of a page, not a cut.**
            //
            // The two pages used to swap instantly, which made a spread read as
            // two unrelated screens sharing a chevron and left the swipe feeling
            // like it had failed even when it worked. The new page comes in from
            // the side it lives on and the old one leaves the other way, so the
            // gesture and the movement agree.
            //
            // Short on purpose: 220ms, eased. Anything longer sits between you
            // and a volume slider you were reaching for.
            val reducedTurn = MeedwellTheme.reducedMotion
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (reducedTurn) {
                        // Reduced motion: one 90ms crossfade, no travel. A cut
                        // would flicker, which is worse than a short fade.
                        return@AnimatedContent (
                            androidx.compose.animation.fadeIn(tween(90, easing = LinearEasing)) togetherWith
                                androidx.compose.animation.fadeOut(tween(90, easing = LinearEasing))
                        ) using SizeTransform(clip = false) { _, _ -> snap() }
                    }
                    val toSurroundings = targetState == PlayerPage.Surroundings
                    // **The page settles in rather than snapping in.**
                    //
                    // The slide alone read as mechanical. The arriving page now
                    // comes from a shorter distance, fades up over a slightly
                    // longer beat than it moves, and lifts the last two percent
                    // of its scale as it lands, so it feels like paper coming to
                    // rest. The one leaving goes quickly and quietly, because
                    // nobody is watching it.
                    //
                    // 300ms total, and deliberately no longer. This is something
                    // to feel on the way past, not a thing to sit through: the
                    // whole point of the spread is that the far page is close.
                    // **The body no longer travels as a block, and no longer
                    // fades as one.**
                    //
                    // It slid a sixth of the width while its own opacity was
                    // still under a half, so most of the travel happened while
                    // the content was too faint to see it move, and the 2 percent
                    // scale was 3.6dp per edge on blank paper. Two fades of the
                    // same shape over the same ground read as nothing happening.
                    //
                    // Now the container only hints the direction, 14dp, enough
                    // for peripheral vision and too little to read as a slide,
                    // and carries no opacity at all. The arrival is carried
                    // entirely by the parts arriving in order, each with its own
                    // alpha, so nothing is multiplied by an envelope and the
                    // ordering stays sharp.
                    val enter = slideInHorizontally(
                        animationSpec = tween(300, easing = Motion.Settle),
                        initialOffsetX = { if (toSurroundings) 42 else -42 },
                    )
                    // Gone before the third beat of the arrival. Nobody watches
                    // an exit, so it leaves quickly and takes no scale with it.
                    val exit = slideOutHorizontally(
                        animationSpec = tween(Motion.leave, easing = Motion.Leave),
                        targetOffsetX = { if (toSurroundings) -48 else 48 },
                    ) + fadeOut(tween(90, easing = LinearEasing))
                    // **The container must not resize on a spring.**
                    //
                    // `enter togetherWith exit` defaults its size transform to
                    // clip = true on a spring. The two pages differ by hundreds
                    // of dp, so the arriving page was revealed through a window
                    // that was itself still growing after the transition had
                    // finished, and parts staggering in were staggering partly
                    // outside the clip. The last thing the eye saw was a height
                    // settling, which reads as "the layout is adjusting" rather
                    // than "a page turned". That is most of why the turn could
                    // not be seen at all.
                    //
                    // Snapped and unclipped. This sits in a vertical scroll with
                    // nothing below it, so taking the target height at once is
                    // literally invisible.
                    enter togetherWith exit using SizeTransform(clip = false) { _, _ -> snap() }
                },
                label = "player spread",
            ) { shown ->
            Column {
            when (shown) {
                PlayerPage.Music -> MusicPage(
                    state = state,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onOpenArtwork = onOpenArtwork,
                    onOpenQueue = onOpenQueue,
                    onLove = onLove,
                    onSleepTimer = onSleepTimer,
                    onTone = onTone,
                    onShuffle = onShuffle,
                    onRepeat = onRepeat,
                    onOutput = onOutput,
                    surroundings = surroundings,
                    onSurroundingsVolume = onSurroundingsVolume,
                    onSurroundingsPlayPause = onSurroundingsPlayPause,
                    onOpenSurroundings = { onPageChange(PlayerPage.Surroundings) },
                )
                PlayerPage.Surroundings -> SurroundingsPage(
                    state = surroundings,
                    onPlayPause = onSurroundingsPlayPause,
                    onVolume = onSurroundingsVolume,
                    onCredit = onSurroundingsCredit,
                    onBrowse = onBrowseSurroundings,
                    onPick = onPickSurroundings,
                    onSleepTimer = onSleepTimer,
                    onStop = onSurroundingsStop,
                    onLeave = { onPageChange(PlayerPage.Music) },
                )
            }
            }
            }
        }

        // The facing page's edge, on whichever side it is.
        PageEdge(
            onRight = page == PlayerPage.Music,
            modifier = Modifier.align(
                if (page == PlayerPage.Music) Alignment.CenterEnd else Alignment.CenterStart
            ),
        )
    }
}

@Composable
private fun MusicPage(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenArtwork: () -> Unit,
    onOpenQueue: () -> Unit,
    onLove: () -> Unit,
    onSleepTimer: () -> Unit,
    onTone: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onOutput: () -> Unit,
    surroundings: SurroundingsPlayingState,
    onSurroundingsVolume: (Float) -> Unit,
    onSurroundingsPlayPause: () -> Unit,
    onOpenSurroundings: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // **Drag the record to change the record.** MASTER_SPEC has required this
    // since version one: swiping the cover skips, tapping still opens the
    // artwork, and the two never conflict. The cover consumes its own
    // horizontal drags, which is what makes "never conflict" mechanically true:
    // a child that consumes stops the page-turn above from ever seeing them.
    // The affordance is the gesture itself: the cover rides with the finger at
    // a third of the drag, capped, and springs home if the throw was short.
    var coverDrag by remember { mutableFloatStateOf(0f) }
    val coverReduced = MeedwellTheme.reducedMotion
    val coverRide by animateFloatAsState(
        targetValue = coverDrag,
        animationSpec = if (coverDrag == 0f) Motion.standard else snap(),
        label = "cover ride",
    )

    // Artwork complete, never scrimmed and never written on.
    Reveal(order = 0) {
    Cover(
        url = state.artworkUri,
        title = state.title,
        cornerRadius = Radius.cover,
        contentDescription = "Cover of ${state.title}. Open the artwork viewer.",
        modifier = Modifier
            .padding(top = 15.dp)
            // Square, because Bandcamp art is square and the law is that
            // artwork is shown complete and never cropped. The grid's 286x210
            // band suits its landscape mock gradients; letterboxing real square
            // art into it would put empty ground either side of the one thing
            // on the screen that is the record.
            //
            // **Full width.** It was inset to 82 percent for a while, on the
            // argument that a plate with paper either side is how an
            // illustration sits on a printed page. On the phone it just looked
            // centered and small, and the owner said so. The record is the one
            // thing on this screen worth the whole measure; the page is made
            // less bare by putting something under it, not by shrinking it.
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                translationX = if (coverReduced) 0f else coverRide
            }
            .pointerInput(state.trackId) {
                var travelled = 0f
                detectHorizontalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragEnd = {
                        val threshold = 72.dp.toPx()
                        when {
                            travelled <= -threshold -> onNext()
                            travelled >= threshold -> onPrevious()
                        }
                        travelled = 0f
                        coverDrag = 0f
                    },
                    onDragCancel = { travelled = 0f; coverDrag = 0f },
                ) { change, delta ->
                    change.consume()
                    travelled += delta
                    coverDrag = (travelled * 0.35f).coerceIn(-40.dp.toPx(), 40.dp.toPx())
                }
            }
            .clickable(role = Role.Button, onClick = onOpenArtwork),
    )
    }

    Reveal(order = 1) {
    Column(
        Modifier.fillMaxWidth().padding(top = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            state.title,
            style = type.playerTitle,
            color = colors.primaryText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            // Artist and record, as the grid sets it. The album was being
            // dropped, so this line carried half its information on the screen
            // with the least to read.
            listOf(state.artist, state.album).filter { it.isNotBlank() }.joinToString(" · "),
            // Body weight rather than `rowTitle`. At Medium it competed with the
            // title directly above it and flattened the very hierarchy the page
            // was short of.
            style = type.body,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
        // "andante · IV of IX", in the serif. Dynamics replace numbers.
        // Absent entirely when neither part is known, rather than an empty row.
        val programme = state.programmeLine
        if (programme.isNotBlank()) {
            Text(
                programme,
                style = type.dynamics,
                color = colors.tertiaryText,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
    }

    }

    Reveal(order = 2) {
    Column {
    ContourScrubber(
        progress = state.progress,
        seed = state.trackId.orEmpty().ifBlank { state.title },
        onSeek = onSeek,
        positionLabel = formatClock(state.positionMs),
        durationLabel = formatClock(state.durationMs),
        modifier = Modifier.padding(top = 14.dp),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatClock(state.positionMs), style = type.meta, color = colors.tertiaryText)
        Text(formatClock(state.durationMs), style = type.meta, color = colors.tertiaryText)
    }
    }
    }

    // `.tr { gap: 34px }`, with a 56px filled circle for play.
    //
    // **Shuffle and repeat flank the cluster.** Both were built, both were
    // reachable from the player's state, and neither had a control anywhere in
    // the interface, so a record could only ever be played straight through
    // once. Grid 09 already puts a repeat and a shuffle mark either side of its
    // transport on the facing page, so this is the spread's own arrangement
    // rather than a new one.
    //
    // It also fixes what looked like a broken skip. At the last track "next"
    // did nothing and said nothing, because with no repeat there is nowhere to
    // go. Now it dims when it cannot move, and repeat is sitting right beside
    // it offering the answer.
    Reveal(order = 3) {
    Row(
        Modifier.fillMaxWidth().padding(top = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIcon(
            MeedwellIcons.Shuffle,
            if (state.shuffle) "Shuffle, on" else "Shuffle, off",
            onShuffle,
            size = 24.dp,
            inked = state.shuffle,
        )
        Box(Modifier.width(10.dp))
        PlainIcon(
            MeedwellIcons.Previous,
            "Previous",
            onPrevious,
            size = 26.dp,
            enabled = state.hasPrevious || state.positionMs > 3_000,
        )
        Box(Modifier.width(34.dp))
        // The one filled shape in the app, and now the one that answers hardest.
        val playInteraction = remember { MutableInteractionSource() }
        val playPressed by playInteraction.collectIsPressedAsState()
        // The one filled shape in the app, and the one thing allowed to
        // spring back with a little life. DESIGN.md reserves the expressive
        // spring for signature moments; this is the signature.
        val playScale by animateFloatAsState(
            targetValue = if (playPressed && !MeedwellTheme.reducedMotion) 0.94f else 1f,
            animationSpec = if (playPressed) tween(60, easing = LinearEasing) else Motion.expressive,
            label = "play press",
        )
        Box(
            Modifier
                .graphicsLayer { scaleX = playScale; scaleY = playScale }
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.primaryText)
                .clickable(
                    role = Role.Button,
                    interactionSource = playInteraction,
                    indication = null,
                    onClick = onPlayPause,
                )
                .semantics { contentDescription = if (state.isPlaying) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) {
            MeedwellIcon(
                icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                size = 24.dp,
                tint = colors.background,
            )
        }
        Box(Modifier.width(34.dp))
        PlainIcon(MeedwellIcons.Next, "Next", onNext, size = 26.dp, enabled = state.hasNext)
        Box(Modifier.width(10.dp))
        PlainIcon(
            icon = when (state.repeat) {
                RepeatMode.One -> MeedwellIcons.RepeatOne
                else -> MeedwellIcons.Repeat
            },
            description = when (state.repeat) {
                RepeatMode.Off -> "Repeat, off"
                RepeatMode.All -> "Repeat the record"
                RepeatMode.One -> "Repeat this piece"
            },
            onClick = onRepeat,
            size = 24.dp,
            inked = state.repeat != RepeatMode.Off,
        )
    }
    }

    Reveal(order = 4) {
    Column {
    // **Under the transport, not above it.**
    //
    // Above, this card pushed the play button clean off a 914dp screen, which
    // is a straight trade of the most important control for a secondary one.
    // Below the transport it is still on the page, still the first thing under
    // the controls, and the part of it that is cut off is what invites the
    // scroll.
    // **A card, so it is unmistakably not the music's volume.**
    //
    // It was a bare slider with a small sun beside it, sitting directly under
    // the track's own clocks, and it read as the volume for the thing playing.
    // A control that can be mistaken for a different control is worse than one
    // that is missing, because the mistake is silent.
    //
    // So it is set apart the way the floating card is: its own ground, its own
    // hairline, a soft shadow. It names what it belongs to in small caps, it
    // names the recording, it pauses and resumes without leaving the page, and
    // tapping it turns to the Surroundings page where a different recording is
    // one more tap away.
    if (surroundings.hasSound) {
        val edge = if (colors.isDark) Color(0x29EFEEE6) else Color(0x291C2420)
        val shadowInk = if (colors.isDark) Color(0x99000000) else Color(0x451C2420)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    clip = false,
                    ambientColor = shadowInk,
                    spotColor = shadowInk,
                )
                .clip(RoundedCornerShape(20.dp))
                // Opaque. The floating card's translucency exists because it floats
                // over a scrolling shelf; this card sits inline on solid ground,
                // so 93 percent of the ground over the ground was a wasted
                // compositing layer wearing a borrowed idiom.
                .background(colors.background)
                .border(1.dp, edge, RoundedCornerShape(20.dp))
                .clickable(role = Role.Button, onClick = onOpenSurroundings)
                .semantics {
                    contentDescription =
                        "Underneath: ${surroundings.title}. Tap to choose another."
                }
                .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SunMark(
                    here = true,
                    playing = surroundings.isPlaying,
                    width = 16.dp,
                    height = 12.dp,
                )
                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                    // **The eyebrow says SURROUNDINGS, not UNDERNEATH.**
                    //
                    // The old word was solving a real problem, that this level
                    // must never be taken for the music's, but it solved it by
                    // shouting a spatial adverb nobody had been taught. This is
                    // the same word as the tab, the page mark and the section
                    // head, so it is learned once, and the sun on the level line
                    // below ties the two together without saying anything.
                    Text("SURROUNDINGS", style = type.plate, color = colors.tertiaryText)
                    Text(
                        surroundings.title,
                        style = type.rowSub,
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                PlainIcon(
                    if (surroundings.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                    if (surroundings.isPlaying) "Pause the surroundings" else "Play the surroundings",
                    onSurroundingsPlayPause,
                    size = 15.dp,
                )
                MeedwellIcon(
                    MeedwellIcons.ChevronRight,
                    size = 14.dp,
                    tint = colors.tertiaryText,
                )
            }
            LevelLine(
                value = surroundings.volume,
                onChange = onSurroundingsVolume,
                playing = surroundings.isPlaying,
            )
        }
    }

    // `.subrow`: heart, sleep, tone, output, queue.
    //
    // **Ruled off and enlarged.** These sat at 16dp in 48dp boxes directly under
    // the transport with nothing between them, so the primary controls and the
    // secondary ones read as one undifferentiated row of small marks. A hairline
    // separates the two jobs, and the glyphs are drawn at 20dp in 56dp boxes
    // because somebody reaching for these in a car should not have to aim.
    }
    }

    Reveal(order = 5) {
    Column {
    Hairline(Modifier.padding(top = 14.dp))
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIcon(MeedwellIcons.Heart, "Love this piece", onLove, size = 22.dp)
        // The fermata, which is the sleep timer's mark everywhere. There is no
        // moon in this app.
        PlainIcon(
            MeedwellIcons.Fermata,
            if (state.sleepSecondsRemaining != null) "Sleep timer, ${state.sleepLabel} left" else "Sleep timer",
            onSleepTimer,
            size = 22.dp,
            inked = state.sleepSecondsRemaining != null || state.sleepAtEndOfPiece,
        )
        PlainIcon(
            MeedwellIcons.Tone,
            "Tone, currently ${state.toneName}",
            onTone,
            size = 22.dp,
            inked = state.voicing != com.kamsiob.meedwell.core.library.Voicing.AsRecorded,
        )
        PlainIcon(MeedwellIcons.Output, "Where the sound goes", onOutput, size = 22.dp)
        PlainIcon(MeedwellIcons.QueueOpen, "The queue", onOpenQueue, size = 22.dp)
    }
    }
    }

    // **The one hint the spread needs.**
    //
    // The facing page is discoverable by the sliver at the edge and by the two
    // page marks, and neither says what is over there. This does, once, quietly,
    // and only while there is nothing playing underneath: the moment a bed is
    // running the card above says it far better, and a standing instruction
    // would become furniture.
    //
    // Set in the serif italic the app uses for tempo marks, at tertiary ink, so
    // it reads as an engraver's note in a margin rather than as a tooltip.
    if (!surroundings.hasSound) {
        Text(
            "Swipe for Surroundings →",
            style = type.tempo,
            color = colors.tertiaryText,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .semantics {
                    contentDescription = "Swipe left for Surroundings, or use the sun mark above"
                },
        )
    }

    // **Nothing closes the page.**
    //
    // A seed head plate sat here, at a third opacity, as a printed score closes
    // a page. The owner's reaction was "I don't even know what that is", which
    // is the whole answer: a mark that has to be explained is decoration, and
    // this design does not decorate. The sleep line went with it. Neither the
    // timer nor anything else should say a word here unless there is something
    // to say, and when a timer is running the fermata in the row above already
    // carries it.
    Box(Modifier.height(26.dp))
}

@Composable
private fun SurroundingsPage(
    state: SurroundingsPlayingState,
    onPlayPause: () -> Unit,
    onVolume: (Float) -> Unit,
    onCredit: () -> Unit,
    onBrowse: () -> Unit,
    onPick: (String) -> Unit,
    onSleepTimer: () -> Unit,
    onStop: () -> Unit,
    onLeave: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // **The page says what it is, in both states.**
    //
    // It used to open "Nothing underneath" over a sentence about field
    // recordings and then two thirds of blank page, and the owner's reaction was
    // that he did not know why it said any of that. The title is the same word
    // as the tab and the page mark, so it is learned once, and the line under it
    // states both uses in nine words: a bed under music, or the only thing on.
    // Nothing here needs a second explanation.
    Reveal(order = 0) {
    Column {
    Text(
        "Surroundings",
        style = type.playerTitle,
        color = colors.primaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
    )
    Text(
        "Recordings of real places. Put one under your music, or enjoy one on its own.",
        style = type.body,
        color = colors.secondaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp, end = 8.dp),
    )
    }
    }

    if (state.hasSound) {
        Reveal(order = 1) {
        FieldPlate(
            seed = state.soundId,
            group = state.group,
            playing = state.isPlaying,
            modifier = Modifier.padding(top = 16.dp),
        )
        }
        Reveal(order = 2) {
        Column {
        Text(
            state.title,
            style = type.h2,
            color = colors.primaryText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        )
        if (state.description.isNotBlank()) {
            Text(
                state.description,
                style = type.tempo,
                color = colors.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onCredit)
                .semantics { contentDescription = "Who recorded this, and under what license" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                state.credit,
                style = type.meta,
                color = colors.tertiaryText,
                textAlign = TextAlign.Center,
            )
        }

        LevelLine(
            value = state.volume,
            onChange = onVolume,
            playing = state.isPlaying,
            modifier = Modifier.padding(top = 8.dp),
        )

        // Stop and the sleep timer flank the transport, the way shuffle and
        // repeat flank the music page's. Two pages of one book, same furniture
        // in the same places. A bed loops forever and has no queue, so the
        // grid's own repeat and shuffle marks here would be decoration.
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A word, not an ✕. The cross means "put this away" everywhere else,
            // and here it ended the sound: the same mismatch the tab's playing
            // block already fixed. One name for the action everywhere: Stop.
            Box(
                Modifier
                    .defaultMinSize(minWidth = 52.dp, minHeight = 52.dp)
                    .clickable(role = Role.Button, onClick = onStop)
                    .semantics { contentDescription = "Stop the surroundings" },
                contentAlignment = Alignment.Center,
            ) {
                Text("Stop", style = type.chip, color = colors.secondaryText)
            }
            Box(Modifier.width(22.dp))
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.moss)
                    .clickable(role = Role.Button, onClick = onPlayPause)
                    .semantics {
                        contentDescription =
                            if (state.isPlaying) "Pause the surroundings" else "Play the surroundings"
                    },
                contentAlignment = Alignment.Center,
            ) {
                MeedwellIcon(
                    icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                    size = 21.dp,
                    tint = colors.background,
                )
            }
            Box(Modifier.width(22.dp))
            PlainIcon(
                MeedwellIcons.Fermata,
                if (state.sleepLabel.isNotBlank()) "Sleep timer, ${state.sleepLabel} left" else "Sleep timer",
                onSleepTimer,
                size = 21.dp,
                inked = state.sleepLabel.isNotBlank(),
            )
        }
        }
        }
    }

    HereList(state = state, onPick = onPick, onBrowse = onBrowse, onLeave = onLeave)
    SurroundingsFoot(state = state, onBrowse = onBrowse)
}

/**
 * What is on this phone, browsable by the kind of place it is.
 *
 * **A flat four was a rounding error.** On a phone holding the whole library the
 * page offered four recordings out of a hundred and eleven, with no way to reach
 * the rest without leaving the player. That is not a short list, it is a
 * shortcoming dressed as restraint.
 *
 * So: a rail of categories, then the recordings in the one you are looking at.
 * The rail uses the same idiom as the shelf's view switcher and the page marks,
 * an inked label standing on a short moss rule, because this app already has a
 * way of saying "this one" and it should not invent a second. Nothing is filled,
 * nothing is enclosed, and the list beneath changes without the page jumping.
 *
 * **It is never empty.** With nothing on the phone it explains what this is and
 * offers one clear way in.
 */
@Composable
private fun HereList(
    state: SurroundingsPlayingState,
    onPick: (String) -> Unit,
    onBrowse: () -> Unit,
    onLeave: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    if (state.hereByGroup.isEmpty()) {
        Reveal(order = 3) {
            Column(Modifier.fillMaxWidth().padding(top = 26.dp)) {
                SectionHead("How this works")
                Text(
                    "Pick a recording and it plays underneath whatever you are listening to, " +
                        "at its own level. Leave the music off and it is simply the room you " +
                        "are sitting in.",
                    style = type.body,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "Three came with the app and work with no connection. The rest are a tap away.",
                    style = type.meta,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable(role = Role.Button, onClick = onBrowse)
                        .padding(top = 18.dp)
                        .semantics { contentDescription = "All recordings" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("All recordings ›", style = type.button, color = colors.primaryText)
                }
            }
        }
        return
    }

    // Open on the category holding whatever is playing, so the page arrives
    // already looking at the thing you are listening to.
    val playingGroup = state.hereByGroup.indexOfFirst { slice ->
        slice.items.any { it.playing }
    }
    var selected by remember(state.hereByGroup.size) {
        mutableStateOf(if (playingGroup >= 0) playingGroup else 0)
    }
    val slice = state.hereByGroup.getOrNull(selected.coerceIn(0, state.hereByGroup.lastIndex))

    Reveal(order = 3) {
        Column(Modifier.fillMaxWidth()) {
            SectionHead("On this phone", Modifier.padding(top = 20.dp, bottom = 6.dp))

            // The rail. Horizontally scrollable, because nine categories will
            // not fit and shrinking them to fit would make them unreadable.
            //
            // **It says that it scrolls.** A cut off word at the edge is the
            // honest signal and it was not enough on its own, so a chevron sits
            // at the right while there is more to the right, and disappears at
            // the end. It is drawn in the margin rather than over the words.
            val railScroll = rememberScrollState()
            Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(railScroll),
                verticalAlignment = Alignment.Top,
            ) {
                state.hereByGroup.forEachIndexed { index, item ->
                    val on = index == selected
                    Column(
                        Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable(role = Role.Tab) { selected = index }
                            .padding(end = 18.dp)
                            .semantics {
                                contentDescription =
                                    if (on) "${item.title}, showing" else item.title
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            item.title,
                            style = type.chip,
                            color = if (on) colors.primaryText else colors.tertiaryText,
                            maxLines = 1,
                        )
                        // The same 16dp mark the page marks wear, and it
                        // draws rather than teleporting: the exact fix the page
                        // marks got, applied here instead of reinvented without.
                        val markWidth by animateDpAsState(
                            targetValue = if (on) 16.dp else 0.dp,
                            animationSpec = if (MeedwellTheme.reducedMotion) snap() else tween(Motion.turn, easing = Motion.Settle),
                            label = "rail mark",
                        )
                        Box(
                            Modifier
                                .padding(top = 5.dp)
                                .width(markWidth)
                                .height(2.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (on) colors.moss else Color.Transparent)
                        )
                    }
                }
            }
            if (railScroll.value < railScroll.maxValue) {
                MeedwellIcon(
                    MeedwellIcons.ChevronRight,
                    size = 14.dp,
                    tint = colors.tertiaryText,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            }

            // The recordings in the category being looked at. Crossfaded, so
            // switching reads as the same shelf turning rather than the page
            // being rebuilt under your thumb.
            // **Swipe the list to change category.**
            //
            // The gesture lives on the list rather than the whole page, so it
            // never fights the spread's own page turn: a swipe that starts on
            // these rows is consumed here, and one that starts anywhere else
            // still turns back to the music.
            Crossfade(
                targetState = slice?.id.orEmpty(),
                animationSpec = if (MeedwellTheme.reducedMotion) snap() else tween(220, easing = Motion.Settle),
                label = "category",
                modifier = Modifier.pointerInput(state.hereByGroup.size) {
                    var travelled = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { travelled = 0f },
                        onDragEnd = {
                            if (travelled <= -70f) {
                                selected = (selected + 1).coerceAtMost(state.hereByGroup.lastIndex)
                            } else if (travelled >= 70f) {
                                // **No silent dead zone.** The list consumes its
                                // drags, so on the first category a right-swipe
                                // used to do nothing at all while the identical
                                // gesture an inch higher turned the page. At the
                                // boundary the gesture keeps its meaning and
                                // turns back to the music.
                                if (selected == 0) onLeave()
                                else selected -= 1
                            }
                        },
                    ) { change, delta ->
                        change.consume()
                        travelled += delta
                    }
                },
            ) { _ ->
                Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    slice?.items?.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp)
                                .clickable(role = Role.Button) { onPick(item.id) }
                                .padding(vertical = 9.dp)
                                .semantics {
                                    contentDescription =
                                        if (item.playing) "${item.title}, playing"
                                        else "Play ${item.title}"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SunMark(
                                here = true,
                                playing = item.playing,
                                width = 16.dp,
                                height = 12.dp,
                            )
                            Text(
                                item.title,
                                style = type.rowTitle,
                                color = if (item.playing) colors.primaryText else colors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                            )
                            Text(
                                if (item.playing) "playing" else item.duration,
                                style = type.plate,
                                color = if (item.playing) colors.mossInk else colors.tertiaryText,
                            )
                        }
                        Hairline()
                    }
                }
            }
        }
    }
}

/** The closing subrow: the one way to the full library. */
@Composable
private fun SurroundingsFoot(
    state: SurroundingsPlayingState,
    onBrowse: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Reveal(order = 4) {
        // One way to the library, and only when there is something the list
        // above did not already offer. The fermata lives beside the transport;
        // a second one down here was the same control twice on one page.
        if (state.hereByGroup.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onBrowse),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    "All recordings ›",
                    style = type.chip,
                    color = colors.secondaryText,
                    modifier = Modifier.semantics { contentDescription = "All recordings" },
                )
            }
        }
    }
    Box(Modifier.height(24.dp))
}

/**
 * One part of a page, arriving a beat after the one above it.
 *
 * **The turn had a movement but no experience.** A slide and a fade that both
 * ran on the whole page at once read as the screen being replaced, and the owner
 * said he had to go looking for it rather than feeling it. Parts that arrive in
 * order read as a page being set: the eye follows the build instead of catching
 * the end of it.
 *
 * One beat apart, four or five parts, so the whole thing is over
 * inside a third of a second. Long enough to be felt, far too short to wait for.
 * Reduced motion turns it off entirely, because a stagger is exactly the kind of
 * movement somebody switches that setting on to avoid.
 */
@Composable
private fun Reveal(order: Int, content: @Composable () -> Unit) {
    if (MeedwellTheme.reducedMotion) {
        content()
        return
    }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            durationMillis = 190,
            delayMillis = Motion.stagger(order),
            easing = Motion.Settle,
        ),
        label = "reveal",
    )
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 14.dp.toPx()
        }
    ) {
        content()
    }
}


/** The dynamic a volume sits at, for a screen reader that cannot see the line. */
fun dynamicName(value: Float): String = when {
    value <= 0.01f -> "silent"
    value < 0.2f -> "pianissimo"
    value < 0.4f -> "piano"
    value < 0.6f -> "mezzo piano"
    value < 0.8f -> "mezzo forte"
    value < 0.95f -> "forte"
    else -> "fortissimo"
}

/**
 * Every icon control on the player, with three things the old one lacked.
 *
 * **It answers a finger.** There was no feedback of any kind on press, so a tap
 * that did nothing and a tap that worked looked identical, which is most of why
 * a skip at the end of a record read as a broken button. It now dips to 88
 * percent under the finger and springs back, which is felt more than seen.
 *
 * **On is unmistakable.** Shuffle and repeat carried their state as a change of
 * ink alone, one step of gray to moss, which is far too quiet for the two
 * controls whose whole job is telling you what mode you are in. An inked
 * control now also stands on a short moss rule, the same mark the page marks
 * and the shelf switcher use for "this one". Still no fill.
 *
 * **Bigger.** The glyphs were 15 to 18dp inside a 48dp box. They are drawn
 * larger now and the box is 52dp, because this is the one screen somebody uses
 * without looking closely.
 */
@Composable
private fun PlainIcon(
    icon: MeedwellIcons,
    description: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 17.dp,
    /** On, for a mode like shuffle or repeat. Carried by ink and a rule. */
    inked: Boolean = false,
    /** False when the control genuinely cannot act, such as next at the end. */
    enabled: Boolean = true,
) {
    val colors = MeedwellTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // **A stroke answers pressure by taking more ink, not by shrinking.**
    //
    // The 0.88 dip on a 20dp glyph was a 2.4dp wobble, and a uniform scale dip
    // is the most generic press on any platform. The dip is shallow now, and
    // the real answer is the ink darkening under the finger, the way a nib
    // presses into paper. It releases slower than it lands, like ink drying.
    val scale by animateFloatAsState(
        targetValue = if (pressed && !MeedwellTheme.reducedMotion) 0.94f else 1f,
        animationSpec = tween(if (pressed) 60 else 160, easing = Motion.Settle),
        label = "press",
    )
    val restingInk = when {
        inked -> colors.moss
        !enabled -> colors.tertiaryText.copy(alpha = 0.3f)
        else -> colors.secondaryText
    }
    val ink by animateColorAsState(
        targetValue = if (pressed && enabled) colors.primaryText else restingInk,
        animationSpec = tween(if (pressed) 70 else 160, easing = Motion.Settle),
        label = "press ink",
    )

    Box(
        Modifier
            .defaultMinSize(minWidth = 52.dp, minHeight = 52.dp)
            .clickable(
                role = Role.Button,
                enabled = enabled,
                interactionSource = interaction,
                // No ripple. This design has no filled shapes for one to sit in,
                // so the press is carried by the glyph itself.
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MeedwellIcon(
                icon = icon,
                size = size,
                tint = ink,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            )
            // The rule that says "on", 3dp under the glyph.
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .width(if (inked) 14.dp else 0.dp)
                    .height(2.dp)
                    .background(if (inked) colors.moss else Color.Transparent)
            )
        }
    }
}

private fun formatClock(ms: Long): String {
    if (ms < 0) return "--:--"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

private const val SWIPE_THRESHOLD_PX = 90f

/** What the Surroundings page of the spread needs to draw itself. */
/** One category, and what of it is on this phone. */
data class SurroundingsSlice(
    val id: String,
    val title: String,
    val items: List<SurroundingsCardItem>,
)

data class SurroundingsPlayingState(
    /** The recording's own id, which seeds its plate so it always draws the same. */
    val soundId: String = "",
    /** Its group, which decides which field the plate draws. */
    val group: String = "",
    val title: String = "",
    val description: String = "",
    val credit: String = "",
    val isPlaying: Boolean = false,
    val volume: Float = 0.6f,
    val hasSound: Boolean = false,
    /**
     * What is on the phone, playing one first, at most four.
     *
     * Grid 09 draws three rows under an "On this phone" head, and they are the
     * whole reason this page is worth swiping to: changing the bed without
     * leaving the music. Without them the page was a blurb and a link out.
     */
    val here: List<SurroundingsCardItem> = emptyList(),
    /** "45:00" while a sleep timer runs, blank otherwise. */
    val sleepLabel: String = "",
    /**
     * Everything on this phone, split by the kind of place it is.
     *
     * The page used to show a flat four. On a phone holding the whole library
     * that is not a short list, it is a rounding error, and there was no way to
     * reach the other hundred without leaving the player.
     */
    val hereByGroup: List<SurroundingsSlice> = emptyList(),
)


/**
 * How far the page has to be pulled past its top before it closes.
 *
 * Generous, because this competes with an ordinary scroll and a page that
 * vanished on a slightly long flick would be worse than one that never closed.
 */
private const val PULL_TO_CLOSE_PX = 260f
