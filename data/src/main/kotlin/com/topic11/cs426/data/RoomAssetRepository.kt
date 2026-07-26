package com.topic11.cs426.data

import com.topic11.cs426.core.database.dao.CatalogDao
import com.topic11.cs426.data.mapping.toDomain
import com.topic11.cs426.data.mapping.toEntity
import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.model.Location
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomAssetRepository(
    private val catalogDao: CatalogDao,
) : AssetRepository {
    override fun observeAssets(): Flow<List<AssetSummary>> =
        catalogDao.observeAssetSummaries()
            .map { assets -> assets.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeLocations(): Flow<List<Location>> =
        catalogDao.observeLocations()
            .map { locations -> locations.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun getAsset(id: AssetId): Asset? =
        catalogDao.getAsset(id.value)?.toDomain()

    override suspend fun getAssetByCode(code: String): Asset? =
        catalogDao.getAssetByCode(code)?.toDomain()

    override suspend fun getLocation(id: LocationId): Location? =
        catalogDao.getLocation(id.value)?.toDomain()

    override suspend fun saveAsset(asset: Asset) {
        val code = asset.code
        if (code != null) {
            val duplicate = catalogDao.getAssetByCode(code)
            require(duplicate == null || duplicate.id == asset.id.value) {
                "Asset code already exists: $code"
            }
        }
        catalogDao.upsertAssets(listOf(asset.toEntity()))
    }
}
