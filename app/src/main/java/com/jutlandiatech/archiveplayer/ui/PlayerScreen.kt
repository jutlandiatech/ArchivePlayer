package com.jutlandiatech.archiveplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.ui.compose.PlayerSurface
import com.jutlandiatech.archiveplayer.AppViewModel
import com.jutlandiatech.archiveplayer.Screen
import kotlinx.coroutines.delay
import com.jutlandiatech.archiveplayer.cast.FlavourCast
import com.jutlandiatech.archiveplayer.core.language.LanguageNormalizer
import kotlin.math.abs
import kotlin.math.roundToInt

private fun clock(ms: Long): String {
    val s = (ms.coerceAtLeast(0) / 1000)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun positionState(vm: AppViewModel, periodMs: Long): State<Long> = produceState(0L, vm.player) {
    while (true) { value = vm.player.positionMs; delay(periodMs) }
}

/** Mini-player above the bottom bar on every tab (UI_SHELL_SPEC.md, UI_PLAYER_SPEC.md P01/P18). */
@Composable
fun MiniPlayer(vm: AppViewModel) {
    val p = vm.player
    if (!p.hasQueue) {
        if (p.canUndoStop) {
            LaunchedEffect(Unit) { delay(5_000); p.dismissUndo() }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("player.stopped"), Modifier.weight(1f))
                TextButton(onClick = { p.undoStop() }, modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.PLAYER_UNDO_STOP)) { Text(t("player.stopped.undo")) }
            }
        }
        return
    }
    val position by positionState(vm, 500)
    var drag by remember { mutableFloatStateOf(0f) }
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().testTag(ElementIds.MINI_PLAYER)
            .pointerInput(Unit) {
                detectVerticalDragGestures(onDragStart = { drag = 0f }, onDragEnd = { if (drag > 40.dp.toPx()) p.stop() }) { _, d -> drag += d }
            }
            .clickable { vm.playerOpen = true },
    ) {
        Column {
            val duration = p.durationMs
            LinearProgressIndicator(progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().testTag(ElementIds.MINI_PLAYER_PROGRESS))
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val title = p.title.orEmpty()
                Column(Modifier.weight(1f)) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.tagged(ElementIds.MINI_PLAYER_TITLE, title))
                    p.itemTitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                GlyphButton(if (p.isPlaying) "❚❚" else "▶", t(if (p.isPlaying) "player.pause" else "player.play"), ElementIds.MINI_PLAYER_PLAY_PAUSE_BUTTON) { p.togglePlay() }
            }
        }
    }
}

@Composable
private fun OptionRow(tag: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, onClick = onClick).testTag(tag)
            .semantics { text = androidx.compose.ui.text.AnnotatedString(label) }.padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

val SPEEDS = listOf("0_5" to 0.5f, "0_75" to 0.75f, "1_0" to 1f, "1_25" to 1.25f, "1_5" to 1.5f, "1_75" to 1.75f, "2_0" to 2f)
val SLEEP_OPTIONS = listOf("off", "end", "15", "30", "45", "60")

/** Full-screen player layer (UI_PLAYER_SPEC.md). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(vm: AppViewModel) {
    val p = vm.player
    BackHandler { vm.playerOpen = false }
    var sheet by remember { mutableStateOf<String?>(null) }
    val position by positionState(vm, 250)
    val duration = p.durationMs
    Surface(Modifier.fillMaxSize().testTag(ElementIds.PLAYER_SCREEN)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphButton("⌄", t("player.close"), ElementIds.PLAYER_COLLAPSE_BUTTON) { vm.playerOpen = false }
                Spacer(Modifier.weight(1f))
                CastButton(Modifier.testTag(ElementIds.PLAYER_CAST_BUTTON))
            }
            if (p.isVideo) {
                PlayerSurface(player = p.player, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(androidx.compose.ui.graphics.Color.Black).testTag(ElementIds.PLAYER_VIDEO_SURFACE))
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant).testTag(ElementIds.PLAYER_ARTWORK), contentAlignment = Alignment.Center) {
                    Text("♪", style = MaterialTheme.typography.displayLarge)
                }
            }
            val title = p.title.orEmpty()
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.tagged(ElementIds.PLAYER_TITLE, title))
            val sub = listOfNotNull(p.itemTitle, p.creator).joinToString(" · ")
            Text(sub, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clickable {
                p.currentId?.substringBefore('/')?.let { vm.playerOpen = false; vm.push(Screen.Item(it)) }
            }.tagged(ElementIds.PLAYER_SUBTITLE, sub))
            val stateKey = if (p.isPlaying) "playing" else "paused"
            Text(t("player.$stateKey"), style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.testTag(ElementIds.PLAYER_STATE).semantics { stateDescription = stateKey })
            Slider(
                value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                onValueChange = { if (duration > 0) p.seekTo((it * duration).toLong()) },
                modifier = Modifier.fillMaxWidth().testTag(ElementIds.PLAYER_SEEK_BAR),
            )
            Row {
                Text(clock(position), modifier = Modifier.tagged(ElementIds.PLAYER_ELAPSED, clock(position)))
                Spacer(Modifier.weight(1f))
                val left = "-" + clock(duration - position)
                Text(left, modifier = Modifier.tagged(ElementIds.PLAYER_REMAINING, left))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                val shuffle = if (p.shuffle) "on" else "off"
                GlyphButton("⤮", t("player.shuffle.$shuffle"), ElementIds.PLAYER_SHUFFLE_BUTTON, extra = Modifier.semantics { stateDescription = shuffle }) { p.toggleShuffle() }
                GlyphButton("↺", t("player.skipBack"), ElementIds.PLAYER_SKIP_BACK_BUTTON) { p.skip(-10_000) }
                GlyphButton("⏮", t("player.previous"), ElementIds.PLAYER_PREVIOUS_BUTTON) { p.previous() }
                GlyphButton(if (p.isPlaying) "❚❚" else "▶", t(if (p.isPlaying) "player.pause" else "player.play"), ElementIds.PLAYER_PLAY_PAUSE_BUTTON) { p.togglePlay() }
                GlyphButton("⏭", t("player.next"), ElementIds.PLAYER_NEXT_BUTTON) { p.next() }
                GlyphButton("↻", t("player.skipForward"), ElementIds.PLAYER_SKIP_FORWARD_BUTTON) { p.skip(30_000) }
                val repeat = when (p.repeatMode) { Player.REPEAT_MODE_ALL -> "all"; Player.REPEAT_MODE_ONE -> "one"; else -> "off" }
                GlyphButton(if (repeat == "one") "🔂" else "🔁", t("player.repeat.$repeat"), ElementIds.PLAYER_REPEAT_BUTTON, extra = Modifier.semantics { stateDescription = repeat }) { p.cycleRepeat() }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                val liked = vm.isCurrentLiked()
                GlyphButton(if (liked) "♥" else "♡", t(if (liked) "entry.unlike" else "entry.like"), ElementIds.PLAYER_LIKE_BUTTON,
                    extra = Modifier.semantics { stateDescription = if (liked) "on" else "off" }) { vm.toggleCurrentLike() }
                GlyphButton("☰", t("player.queue"), ElementIds.PLAYER_QUEUE_BUTTON) { sheet = "queue" }
                GlyphButton("HQ", t("player.quality"), ElementIds.PLAYER_QUALITY_BUTTON) { sheet = "quality" }
                if (vm.currentSubtitles().isNotEmpty()) GlyphButton("CC", t("player.subtitles"), ElementIds.PLAYER_SUBTITLES_BUTTON) { sheet = "subtitles" }
                GlyphButton("×", t("player.speed"), ElementIds.PLAYER_SPEED_BUTTON) { sheet = "speed" }
                GlyphButton("☾", t("player.sleep"), ElementIds.PLAYER_SLEEP_TIMER_BUTTON) { sheet = "sleep" }
            }
            vm.sleepEndsAt?.let { end ->
                val now by produceState(System.currentTimeMillis(), end) { while (true) { value = System.currentTimeMillis(); delay(1_000) } }
                val text = t("player.sleep.remaining", "time" to clock(end - now))
                Text(text, modifier = Modifier.tagged(ElementIds.PLAYER_SLEEP_REMAINING, text))
            }
            if (p.error != null) {
                Text(t("player.error"), color = MaterialTheme.colorScheme.error, modifier = Modifier.tagged(ElementIds.PLAYER_ERROR, t("player.error")))
                Button(onClick = { p.retry() }, modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.PLAYER_RETRY_BUTTON)) { Text(t("action.retry")) }
            }
        }
    }
    val close = { sheet = null }
    when (sheet) {
        "speed" -> ModalBottomSheet(onDismissRequest = close, modifier = Modifier.testTag(ElementIds.SHEET_SPEED)) {
            SPEEDS.forEach { (key, v) -> OptionRow(ElementIds.speedOption(key), "${v}×", abs(p.speed - v) < 0.01f) { p.changeSpeed(v); close() } }
            Spacer(Modifier.height(24.dp))
        }
        "sleep" -> ModalBottomSheet(onDismissRequest = close, modifier = Modifier.testTag(ElementIds.SHEET_SLEEP)) {
            SLEEP_OPTIONS.forEach { o ->
                val label = when (o) { "off" -> t("player.sleep.off"); "end" -> t("player.sleep.endOfEntry"); else -> t("player.sleep.minutes", "minutes" to o) }
                OptionRow(ElementIds.sleepOption(o), label, vm.sleepOption == o) { vm.setSleep(o); close() }
            }
            Spacer(Modifier.height(24.dp))
        }
        "quality" -> ModalBottomSheet(onDismissRequest = close, modifier = Modifier.testTag(ElementIds.SHEET_QUALITY)) {
            val options = vm.qualityOptions()
            val chosen = vm.chosenQuality()
            OptionRow(ElementIds.QUALITY_OPTION_AUTO, t("player.quality.auto") + (options.firstOrNull { it.auto }?.let { " · " + it.label } ?: ""), chosen == null) { vm.chooseQuality(null); close() }
            options.forEach { o -> OptionRow(ElementIds.qualityOption(o.key), o.label, chosen == o.key) { vm.chooseQuality(o.key); close() } }
            Spacer(Modifier.height(24.dp))
        }
        "subtitles" -> ModalBottomSheet(onDismissRequest = close, modifier = Modifier.testTag(ElementIds.SHEET_SUBTITLES)) {
            val chosen = vm.chosenSubtitle
            OptionRow(ElementIds.SUBTITLE_OPTION_OFF, t("player.subtitles.off"), chosen == null) { vm.chooseSubtitle(null); close() }
            vm.currentSubtitles().map { s ->
                val lang = s.language?.let { LanguageNormalizer.nativeName(it) } ?: t("player.subtitles")
                if (s.autoGenerated) "$lang · ${t("player.subtitles.auto")}" else lang
            }.forEachIndexed { i, label -> OptionRow(ElementIds.subtitleOption(i), label, chosen == i) { vm.chooseSubtitle(i); close() } }
            Spacer(Modifier.height(24.dp))
        }
        "queue" -> ModalBottomSheet(onDismissRequest = close, modifier = Modifier.testTag(ElementIds.SHEET_QUEUE)) {
            TextButton(onClick = { p.stop(); close() }, modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp).testTag(ElementIds.QUEUE_CLEAR_BUTTON)) { Text(t("player.queue.clear")) }
            LazyColumn {
                itemsIndexed(p.queue) { i, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var dy by remember { mutableFloatStateOf(0f) }
                        Box(Modifier.size(48.dp).pointerInput(i, p.queue.size) {
                            val row = 56.dp.toPx()
                            detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = {
                                val target = (i + (dy / row).roundToInt()).coerceIn(0, p.queue.lastIndex)
                                if (target != i) p.move(i, target)
                            }) { _, d -> dy += d }
                        }.tagged(ElementIds.queueDrag(i), "≡"), contentAlignment = Alignment.Center) { Text("≡") }
                        val label = item.mediaMetadata.title?.toString().orEmpty()
                        Box(Modifier.weight(1f).heightIn(min = 56.dp).clickable { p.jump(i) }.tagged(ElementIds.queueRow(i), label), contentAlignment = Alignment.CenterStart) {
                            Text(label, fontWeight = if (i == p.index) androidx.compose.ui.text.font.FontWeight.Bold else null)
                        }
                        GlyphButton("✕", t("playlist.remove"), ElementIds.queueRemove(i)) { p.remove(i) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun CastButton(modifier: Modifier = Modifier) {
    if (FlavourCast.isAvailable) Box(modifier) { FlavourCast.RouteButton(Modifier) }
}
