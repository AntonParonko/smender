package com.smender.app.feature.home.domain.repository

import com.smender.app.feature.home.domain.model.HomeUiState

interface HomeRepository {
    fun getHomeUiState(isInternetConnectedOverride: Boolean? = null): HomeUiState
}
