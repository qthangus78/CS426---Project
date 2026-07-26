package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.Location
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.repository.AssetRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class ObserveLocationsUseCase(
    private val assetRepository: AssetRepository,
) {
    operator fun invoke(): Flow<List<Location>> = assetRepository.observeLocations()
}

class GetAssetUseCase(
    private val assetRepository: AssetRepository,
) {
    suspend operator fun invoke(assetId: AssetId): Asset? = assetRepository.getAsset(assetId)
}

class CreateAssetUseCase(
    private val assetRepository: AssetRepository,
    private val idFactory: () -> AssetId = { AssetId("asset-${UUID.randomUUID()}") },
) {
    suspend operator fun invoke(input: AssetInput): AssetSaveResult {
        val normalized = input.normalized()
        val errors = validateAssetInput(
            repository = assetRepository,
            input = normalized,
            existingAssetId = null,
        )
        if (errors.isNotEmpty()) return AssetSaveResult.ValidationFailed(errors)

        val asset = Asset(
            id = idFactory(),
            name = normalized.name,
            code = normalized.code,
            locationId = requireNotNull(normalized.locationId),
            recurrencePolicyDays = null,
        )
        assetRepository.saveAsset(asset)
        return AssetSaveResult.Success(asset)
    }
}

class UpdateAssetUseCase(
    private val assetRepository: AssetRepository,
) {
    suspend operator fun invoke(assetId: AssetId, input: AssetInput): AssetSaveResult {
        val current = assetRepository.getAsset(assetId) ?: return AssetSaveResult.NotFound
        val normalized = input.normalized()
        val errors = validateAssetInput(
            repository = assetRepository,
            input = normalized,
            existingAssetId = assetId,
        )
        if (errors.isNotEmpty()) return AssetSaveResult.ValidationFailed(errors)

        val asset = current.copy(
            name = normalized.name,
            code = normalized.code,
            locationId = requireNotNull(normalized.locationId),
        )
        assetRepository.saveAsset(asset)
        return AssetSaveResult.Success(asset)
    }
}

data class AssetInput(
    val name: String,
    val code: String?,
    val locationId: LocationId?,
)

sealed interface AssetSaveResult {
    data class Success(val asset: Asset) : AssetSaveResult
    data class ValidationFailed(val errors: List<AssetValidationError>) : AssetSaveResult
    data object NotFound : AssetSaveResult
}

sealed interface AssetValidationError {
    val message: String

    data object NameRequired : AssetValidationError {
        override val message: String = "Asset name is required."
    }

    data object LocationRequired : AssetValidationError {
        override val message: String = "Choose a location for this asset."
    }

    data object LocationNotFound : AssetValidationError {
        override val message: String = "Selected location is no longer available."
    }

    data object DuplicateCode : AssetValidationError {
        override val message: String = "Asset code is already used."
    }
}

private fun AssetInput.normalized(): AssetInput {
    return copy(
        name = name.trim(),
        code = code?.trim()?.takeIf { it.isNotEmpty() },
    )
}

private suspend fun validateAssetInput(
    repository: AssetRepository,
    input: AssetInput,
    existingAssetId: AssetId?,
): List<AssetValidationError> {
    val errors = mutableListOf<AssetValidationError>()
    if (input.name.isBlank()) errors += AssetValidationError.NameRequired
    val locationId = input.locationId
    if (locationId == null) {
        errors += AssetValidationError.LocationRequired
    } else if (repository.getLocation(locationId) == null) {
        errors += AssetValidationError.LocationNotFound
    }
    val code = input.code
    if (code != null) {
        val duplicate = repository.getAssetByCode(code)
        if (duplicate != null && duplicate.id != existingAssetId) {
            errors += AssetValidationError.DuplicateCode
        }
    }
    return errors
}
