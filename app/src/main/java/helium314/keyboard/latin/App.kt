// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
package helium314.keyboard.latin

import android.app.Application
import helium314.keyboard.keyboard.emoji.SupportedEmojis
import helium314.keyboard.latin.define.DebugFlags
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.LayoutUtilsCustom
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.SubtypeSettings

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugFlags.init(this)
        migrateDashboardPreferences()
        Settings.init(this)
        SubtypeSettings.init(this)
        RichInputMethodManager.init(this)

        AppUpgrade.checkVersionUpgrade(this)
        AppUpgrade.transferOldPinnedClips(this) // todo: remove in a few months, maybe mid 2026
        app = this
        Defaults.initDynamicDefaults(this)
        LayoutUtilsCustom.removeMissingLayouts(this) // only after version upgrade
        SupportedEmojis.load(this)

        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        @Suppress("DEPRECATION")
        Log.i(
            "startup", "Starting ${applicationInfo.processName} version ${packageInfo.versionName} (${
                packageInfo.versionCode
            }) on Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})"
        )
    }

    private fun migrateDashboardPreferences() {
        if (android.os.Build.VERSION.SDK_INT >= 24 &&
            !getSystemService(android.os.UserManager::class.java).isUserUnlocked) return
        val settings = dev.headunit.keyboard.DashboardStyle.prefs(this)
        if (settings.contains("dashboard_v2")) return
        val old = getSharedPreferences("keyboard", MODE_PRIVATE)
        val update = settings.edit()
        if (old.contains("accent")) update.putInt("dashboard_accent", old.getInt("accent", dev.headunit.keyboard.DashboardStyle.AMBER))
        if (old.contains("sounds")) update.putBoolean(Settings.PREF_SOUND_ON, old.getBoolean("sounds", true))
        update.putBoolean("dashboard_v2", true).apply()
    }

    companion object {
        // used so JniUtils can access application once
        private var app: App? = null
        fun getApp(): App? {
            val application = app
            app = null
            return application
        }
    }
}
