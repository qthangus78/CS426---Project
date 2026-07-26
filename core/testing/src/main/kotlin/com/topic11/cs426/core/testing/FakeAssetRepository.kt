package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.model.Location
import com.topic11.cs426.domain.model.LocationId
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
            locationId = LocationId("location-lab"),
        ),
    ),
    initialLocations: List<Location> = listOf(Location(LocationId("location-lab"), "Laboratory")),
) : AssetRepository {
    private val assets = MutableStateFlow(initialAssets)
    private val locations = MutableStateFlow(initialLocations)

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

    override fun observeLocations(): Flow<List<Location>> = locations

    override suspend fun getAsset(id: AssetId): Asset? =
        assets.value.firstOrNull { it.id == id }

    override suspend fun getAssetByCode(code: String): Asset? =
        assets.value.firstOrNull { it.code == code }

    override suspend fun getLocation(id: LocationId): Location? =
        locations.value.firstOrNull { it.id == id }

    override suspend fun saveLocation(location: Location) {
        addLocation(location)
    }

    override suspend fun saveAsset(asset: Asset) {
        assets.update { values ->
            if (values.any { it.id == asset.id }) {
                values.map { current -> if (current.id == asset.id) asset else current }
            } else {
                values + asset
            }
        }
    }

    fun addAsset(asset: Asset) {
        assets.update { values ->
            if (values.any { it.id == asset.id }) {
                values.map { current -> if (current.id == asset.id) asset else current }
            } else {
                values + asset
            }
        }
    }

    fun addLocation(location: Location) {
        locations.update { values ->
            if (values.any { it.id == location.id }) {
                values.map { current -> if (current.id == location.id) location else current }
            } else {
                values + location
            }
        }
    }
}
