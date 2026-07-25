package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAssetRepository : AssetRepository {
    private val assets = mutableMapOf<AssetId, Asset>()

    override fun observeAssets(): Flow<List<AssetSummary>> {
        return MutableStateFlow(
            assets.map { (id, a) ->
                AssetSummary(
                    id = id,
                    name = a.name,
                    code = a.code,
                    nextInspectionDueAtMillis = a.nextInspectionDueAtMillis,
                )
            },
        )
    }

    override suspend fun getAsset(id: AssetId): Asset? {
        return assets[id]
    }

    override suspend fun saveAsset(asset: Asset) {
        assets[asset.id] = asset
    }

    fun addAsset(asset: Asset) {
        assets[asset.id] = asset
    }
}
