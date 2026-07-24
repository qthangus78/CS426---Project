package com.topic11.cs426.data

import com.topic11.cs426.core.database.dao.CatalogDao
import com.topic11.cs426.data.mapping.toDomain
import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
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

    override suspend fun getAsset(id: AssetId): Asset? =
        catalogDao.getAsset(id.value)?.toDomain()
}
