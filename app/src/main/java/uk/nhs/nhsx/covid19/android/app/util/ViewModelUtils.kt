package uk.nhs.nhsx.covid19.android.app.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

fun ViewModel.getViewModelScopeOrDefault(coroutineScope: CoroutineScope?) =
    coroutineScope ?: this.viewModelScope
