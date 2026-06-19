package com.astrove.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class AccessState { FULL, PARTIAL, DENIED }

object MediaAccess {

    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else ->
            @Suppress("DEPRECATION")
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun current(context: Context): AccessState {
        fun granted(p: String) =
            ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> when {
                granted(Manifest.permission.READ_MEDIA_IMAGES) -> AccessState.FULL
                granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> AccessState.PARTIAL
                else -> AccessState.DENIED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                if (granted(Manifest.permission.READ_MEDIA_IMAGES)) AccessState.FULL else AccessState.DENIED
            else ->
                @Suppress("DEPRECATION")
                if (granted(Manifest.permission.READ_EXTERNAL_STORAGE)) AccessState.FULL else AccessState.DENIED
        }
    }
}
