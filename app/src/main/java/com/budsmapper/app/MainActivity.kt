package com.budsmapper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.ui.BudsMapperApp

class MainActivity : ComponentActivity() {

    private lateinit var eventStore: LastEventStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventStore = LastEventStore.getInstance(applicationContext)

        setContent {
            BudsMapperApp(eventStore = eventStore)
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure UI state aligns with latest background service recordings
        eventStore.reload()
    }
}
