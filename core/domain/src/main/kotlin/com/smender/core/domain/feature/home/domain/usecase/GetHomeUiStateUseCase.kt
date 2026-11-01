package com.smender.core.domain.feature.home.domain.usecase

import com.smender.core.domain.feature.home.domain.model.HomeUiState
import com.smender.core.domain.feature.home.domain.repository.HomeRepository

class GetHomeUiStateUseCase(
    private val homeRepository: HomeRepository
) {
    operator fun invoke(isInternetConnectedOverride: Boolean? = null): HomeUiState {
        return homeRepository.getHomeUiState(isInternetConnectedOverride)
    }
}
