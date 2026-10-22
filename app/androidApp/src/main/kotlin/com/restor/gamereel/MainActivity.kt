package com.restor.gamereel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.restor.gamereel.data.storage.createRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {
    private var link by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        if (savedInstanceState == null) link = intent.sharedLink()

        setContent {
            App({ createRepository(applicationContext) }, link)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.sharedLink()?.let { link = it }
    }

    private fun Intent.sharedLink(): String? =
        (dataString ?: getStringExtra(Intent.EXTRA_TEXT))?.takeIf { "GR1." in it }
}
