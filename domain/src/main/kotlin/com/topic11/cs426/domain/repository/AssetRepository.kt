package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.model.Location
import com.topic11.cs426.domain.model.LocationId
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    fun observeAssets(): Flow<List<AssetSummary>>

    fun observeLocations(): Flow<List<Location>>

    suspend fun getAsset(id: AssetId): Asset?

    suspend fun getAssetByCode(code: String): Asset?

    suspend fun getLocation(id: LocationId): Location?

    suspend fun saveLocation(location: Location)

    suspend fun saveAsset(asset: Asset)
}
