package com.astrove.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.astrove.TroveApp
import com.astrove.di.AppContainer

/** Builds a ViewModel from the app's [AppContainer] without per-VM factory boilerplate. */
@Composable
inline fun <reified VM : ViewModel> troveViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as TroveApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
