package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAssetRepository(
    initialAssets: List<Asset> = listOf(
        Asset(
            id = InspectionTestFixtures.asset1Id,
            name = "Computer Lab I.44",
        ),
    ),
) : AssetRepository {
    private val assets = MutableStateFlow(initialAssets)

    val savedAssets: List<Asset>
        get() = assets.value

    override fun observeAssets(): Flow<List<AssetSummary>> =
        assets.map { values ->
            values.map { asset ->
                AssetSummary(
                    id = asset.id,
                    name = asset.name,
                    code = asset.code,
                    locationName = "",
                    nextInspectionDueAtMillis = asset.nextInspectionDueAtMillis,
                )
            }
        }

    override suspend fun getAsset(id: AssetId): Asset? =
        assets.value.firstOrNull { it.id == id }

    override suspend fun saveAsset(asset: Asset) {
        check(assets.value.any { it.id == asset.id }) {
            "Asset does not exist: ${asset.id.value}"
        }
        assets.update { values ->
            values.map { current -> if (current.id == asset.id) asset else current }
        }
    }
}
