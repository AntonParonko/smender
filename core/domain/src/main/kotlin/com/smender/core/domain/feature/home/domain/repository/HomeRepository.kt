package com.smender.core.domain.feature.home.domain.repository

import com.smender.core.domain.feature.home.domain.model.HomeUiState

interface HomeRepository {
    fun getHomeUiState(isInternetConnectedOverride: Boolean? = null): HomeUiState
}
