package com.budsmapper.app.ui

import androidx.compose.runtime.Composable
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.ui.screens.HomeScreen
import com.budsmapper.app.ui.theme.BudsMapperTheme

@Composable
fun BudsMapperApp(eventStore: LastEventStore) {
    BudsMapperTheme {
        HomeScreen(eventStore = eventStore)
    }
}
