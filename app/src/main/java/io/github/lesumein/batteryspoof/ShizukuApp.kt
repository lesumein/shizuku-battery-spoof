package io.github.lesumein.batteryspoof

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/** Shizuku 매니저 앱 실행 / 설치 페이지 열기 */
object ShizukuApp {
    const val PACKAGE = "moe.shizuku.privileged.api"
    private const val RELEASES_URL = "https://github.com/RikkaApps/Shizuku/releases/latest"

    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Shizuku 앱을 연다. 실행할 수 없으면 false. */
    fun open(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return false
        return start(context, intent)
    }

    /** Play 스토어 → 웹 Play 스토어 → GitHub 릴리스 순으로 설치 페이지를 연다. */
    fun openDownload(context: Context) {
        val candidates = listOf(
            "market://details?id=$PACKAGE",
            "https://play.google.com/store/apps/details?id=$PACKAGE",
            RELEASES_URL,
        )
        candidates.any { start(context, Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
    }

    fun openReleases(context: Context) {
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)))
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
