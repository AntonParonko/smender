package com.smender.app.feature.home.data.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.provider.Settings

object AutostartSettingsNavigator {
    fun hasAutostartSettingsScreen(context: Context): Boolean {
        return manufacturerOrderedAutostartIntents(context.packageName)
            .any { intent -> intent.resolveActivity(context.packageManager) != null }
    }

    fun firstResolvableAutostartIntent(context: Context): Intent? {
        val packageName = context.packageName
        val oemIntent = manufacturerOrderedAutostartIntents(packageName)
            .firstOrNull { intent -> intent.resolveActivity(context.packageManager) != null }
        return oemIntent ?: appDetailsIntent(packageName)
    }

    fun allAutostartIntentsWithFallback(context: Context): List<Intent> {
        val packageName = context.packageName
        return manufacturerOrderedAutostartIntents(packageName) + appDetailsIntent(packageName)
    }

    private fun manufacturerOrderedAutostartIntents(packageName: String): List<Intent> {
        val miui = listOf(
            componentIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity", packageName),
            componentIntent("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity", packageName)
        )
        val huaweiHonor = listOf(
            componentIntent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity", packageName),
            componentIntent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity", packageName)
        )
        val other = listOf(
            componentIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity", packageName),
            componentIntent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity", packageName),
            componentIntent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity", packageName),
            componentIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity", packageName),
            componentIntent("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity", packageName),
            componentIntent("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity", packageName),
            componentIntent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity", packageName),
            componentIntent("com.realme.securitycenter", "com.realme.securitycenter.permission.startup.StartupAppListActivity", packageName)
        )

        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> huaweiHonor + miui + other
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> miui + huaweiHonor + other
            else -> miui + huaweiHonor + other
        }
    }

    private fun componentIntent(pkg: String, cls: String, packageName: String): Intent {
        return Intent().apply {
            component = ComponentName(pkg, cls)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("package_name", packageName)
            putExtra("packageName", packageName)
            putExtra("pkg_name", packageName)
        }
    }

    private fun appDetailsIntent(packageName: String): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
