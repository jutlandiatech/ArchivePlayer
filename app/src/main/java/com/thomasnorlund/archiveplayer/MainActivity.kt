package com.thomasnorlund.archiveplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.thomasnorlund.archiveplayer.cast.FlavourCast
import com.thomasnorlund.archiveplayer.ui.PlayerScreen
import com.thomasnorlund.archiveplayer.ui.theme.ArchivePlayerTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArchivePlayerTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            actions = { FlavourCast.RouteButton(Modifier) },
                        )
                    },
                ) { padding ->
                    PlayerScreen(Modifier.padding(padding).fillMaxSize())
                }
            }
        }
    }
}
