package com.thomasnorlund.archiveplayer.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.material3.Player
import com.thomasnorlund.archiveplayer.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(modifier: Modifier = Modifier, viewModel: PlayerViewModel = viewModel()) {
    Column(modifier) {
        Player(
            player = viewModel.player,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Text(
            text = PlayerViewModel.SAMPLE_ITEM.mediaMetadata.title.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}
