package com.restor.gamereel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.restor.gamereel.data.storage.createRepository
import com.restor.gamereel.platform.initHttpCache
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var link by mutableStateOf<String?>(null)
    private var file by mutableStateOf<IncomingFile?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        initHttpCache(cacheDir)
        if (savedInstanceState == null) {
            link = intent.sharedLink()
            file = intent.sharedFile()
        }

        setContent {
            App({ createRepository(applicationContext) }, link, file)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.sharedLink()?.let { link = it }
        intent.sharedFile()?.let { file = it }
    }

    private fun Intent.sharedLink(): String? =
        (dataString ?: getStringExtra(Intent.EXTRA_TEXT))?.takeIf { "GR1." in it }

    @Suppress("DEPRECATION")
    private fun Intent.sharedFile(): IncomingFile? {
        val uri = (if (action == Intent.ACTION_SEND) getParcelableExtra<Uri>(Intent.EXTRA_STREAM) else data)
            ?.takeIf { it.scheme == "content" || it.scheme == "file" } ?: return null
        return IncomingFile(uri.toString()) {
            withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
        }
    }
}
