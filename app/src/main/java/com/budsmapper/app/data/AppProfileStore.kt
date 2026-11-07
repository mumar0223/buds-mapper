package com.budsmapper.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported Bluetooth Earbud Media Commands
 */
enum class BudsMediaCommand(val key: String, val displayName: String, val description: String) {
    MEDIA_PLAY("MEDIA_PLAY", "Play", "Single tap or play press"),
    MEDIA_PAUSE("MEDIA_PAUSE", "Pause", "Single tap or pause press"),
    MEDIA_PLAY_PAUSE("MEDIA_PLAY_PAUSE", "Play / Pause", "Toggle playback press"),
    MEDIA_NEXT("MEDIA_NEXT", "Next", "Double tap or skip forward"),
    MEDIA_PREVIOUS("MEDIA_PREVIOUS", "Previous", "Triple tap or skip backward");

    companion object {
        fun fromKey(key: String): BudsMediaCommand? = entries.find { it.key == key }
    }
}

/**
 * Extensible Actions that can be assigned to earbud commands
 */
enum class MappedAction(val id: String, val displayName: String, val description: String) {
    DEFAULT("DEFAULT", "Default Media Behavior", "Passes through standard media playback command"),
    SWIPE_UP("SWIPE_UP", "Swipe Up", "Scrolls to the next Short, Reel, or TikTok"),
    SWIPE_DOWN("SWIPE_DOWN", "Swipe Down", "Scrolls to the previous Short, Reel, or TikTok"),
    SCROLL_DOWN("SCROLL_DOWN", "Scroll Down", "Performs a standard page scroll downward"),
    SCROLL_UP("SCROLL_UP", "Scroll Up", "Performs a standard page scroll upward"),
    PLAY_PAUSE("PLAY_PAUSE", "Play / Pause", "Toggles playback without skipping"),
    NONE("NONE", "Do Nothing", "Consumes the earbud gesture silently");

    companion object {
        fun fromId(id: String): MappedAction = entries.find { it.id == id } ?: DEFAULT
    }
}

/**
 * AppProfileStore
 *
 * Lightweight persistent storage for Application Profiles and Gesture Mappings.
 * Uses SharedPreferences to persist:
 * - Set of enabled package names
 * - Per-app, per-command action assignments
 *
 * Fully event-driven via StateFlows, accessible from both UI and Background Services.
 */
class AppProfileStore(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Set of package names that have custom gesture profiles enabled
    private val _enabledPackages = MutableStateFlow<Set<String>>(loadEnabledPackages())
    val enabledPackages: StateFlow<Set<String>> = _enabledPackages.asStateFlow()

    // Trigger flow to notify UI when any mapping changes
    private val _mappingVersion = MutableStateFlow(0)
    val mappingVersion: StateFlow<Int> = _mappingVersion.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_ENABLED_PACKAGES) {
            _enabledPackages.value = loadEnabledPackages()
        } else if (key?.startsWith("map_") == true) {
            _mappingVersion.value += 1
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        seedDefaultTemplatesIfEmpty()
    }

    private fun loadEnabledPackages(): Set<String> {
        return prefs.getStringSet(KEY_ENABLED_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    fun isProfileEnabled(packageName: String): Boolean {
        return _enabledPackages.value.contains(packageName)
    }

    fun setProfileEnabled(packageName: String, enabled: Boolean) {
        val current = _enabledPackages.value.toMutableSet()
        if (enabled) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_ENABLED_PACKAGES, current).apply()
        _enabledPackages.value = current
    }

    fun getMapping(packageName: String, command: BudsMediaCommand): MappedAction {
        val key = buildMappingKey(packageName, command)
        val savedId = prefs.getString(key, null)
        if (savedId != null) {
            return MappedAction.fromId(savedId)
        }
        // Return sensible defaults if not explicitly mapped
        return getDefaultActionForApp(packageName, command)
    }

    fun setMapping(packageName: String, command: BudsMediaCommand, action: MappedAction) {
        val key = buildMappingKey(packageName, command)
        prefs.edit().putString(key, action.id).apply()
        _mappingVersion.value += 1
    }

    private fun buildMappingKey(packageName: String, command: BudsMediaCommand): String {
        return "map_${packageName}_${command.key}"
    }

    /**
     * Seeds popular default profiles (e.g. YouTube Shorts next/prev on Double/Triple tap)
     * so when a user turns ON an app, it's immediately ready to use!
     */
    private fun seedDefaultTemplatesIfEmpty() {
        if (!prefs.contains("seeded_templates_v1")) {
            // Seed YouTube defaults
            setMapping("com.google.android.youtube", BudsMediaCommand.MEDIA_NEXT, MappedAction.SWIPE_UP)
            setMapping("com.google.android.youtube", BudsMediaCommand.MEDIA_PREVIOUS, MappedAction.SWIPE_DOWN)
            setMapping("com.google.android.youtube", BudsMediaCommand.MEDIA_PLAY, MappedAction.DEFAULT)
            setMapping("com.google.android.youtube", BudsMediaCommand.MEDIA_PAUSE, MappedAction.DEFAULT)
            setMapping("com.google.android.youtube", BudsMediaCommand.MEDIA_PLAY_PAUSE, MappedAction.DEFAULT)

            // Seed Instagram defaults
            setMapping("com.instagram.android", BudsMediaCommand.MEDIA_NEXT, MappedAction.SWIPE_UP)
            setMapping("com.instagram.android", BudsMediaCommand.MEDIA_PREVIOUS, MappedAction.SWIPE_DOWN)

            // Seed Chrome defaults
            setMapping("com.android.chrome", BudsMediaCommand.MEDIA_NEXT, MappedAction.SCROLL_DOWN)
            setMapping("com.android.chrome", BudsMediaCommand.MEDIA_PREVIOUS, MappedAction.SCROLL_UP)

            prefs.edit().putBoolean("seeded_templates_v1", true).apply()
        }
    }

    private fun getDefaultActionForApp(packageName: String, command: BudsMediaCommand): MappedAction {
        val isVideoShorts = packageName.contains("youtube") ||
            packageName.contains("tiktok") ||
            packageName.contains("instagram")

        return when (command) {
            BudsMediaCommand.MEDIA_NEXT -> if (isVideoShorts) MappedAction.SWIPE_UP else MappedAction.DEFAULT
            BudsMediaCommand.MEDIA_PREVIOUS -> if (isVideoShorts) MappedAction.SWIPE_DOWN else MappedAction.DEFAULT
            BudsMediaCommand.MEDIA_PLAY,
            BudsMediaCommand.MEDIA_PAUSE,
            BudsMediaCommand.MEDIA_PLAY_PAUSE -> MappedAction.DEFAULT
        }
    }

    companion object {
        private const val PREFS_NAME = "buds_mapper_profiles"
        private const val KEY_ENABLED_PACKAGES = "enabled_packages"

        @Volatile
        private var instance: AppProfileStore? = null

        fun getInstance(context: Context): AppProfileStore {
            return instance ?: synchronized(this) {
                instance ?: AppProfileStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
