package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow

class ObserveAssetsUseCase(
    private val assetRepository: AssetRepository,
) {
    operator fun invoke(): Flow<List<AssetSummary>> = assetRepository.observeAssets()
}
