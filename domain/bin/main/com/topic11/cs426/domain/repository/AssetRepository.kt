package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    fun observeAssets(): Flow<List<AssetSummary>>

    suspend fun getAsset(id: AssetId): Asset?
}
