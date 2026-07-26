package com.topic11.cs426.domain

import com.topic11.cs426.core.testing.FakeAssetRepository
import com.topic11.cs426.core.testing.FakeTemplateRepository
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.usecase.AssetInput
import com.topic11.cs426.domain.usecase.AssetSaveResult
import com.topic11.cs426.domain.usecase.AssetValidationError
import com.topic11.cs426.domain.usecase.CreateAssetUseCase
import com.topic11.cs426.domain.usecase.CreateTemplateUseCase
import com.topic11.cs426.domain.usecase.TemplateCreateInput
import com.topic11.cs426.domain.usecase.TemplateMetadataInput
import com.topic11.cs426.domain.usecase.TemplateSaveResult
import com.topic11.cs426.domain.usecase.TemplateValidationError
import com.topic11.cs426.domain.usecase.UpdateAssetUseCase
import com.topic11.cs426.domain.usecase.UpdateTemplateMetadataUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetTemplateWorkflowUseCaseTest {
    @Test
    fun `create asset validates location and duplicate code`() = runTest {
        val repository = FakeAssetRepository()
        repository.addAsset(
            InspectionTestFixtures.sampleAsset.copy(
                id = AssetId("asset-existing"),
                code = "LAB-044",
                locationId = LocationId("location-lab"),
            ),
        )
        val useCase = CreateAssetUseCase(
            assetRepository = repository,
            idFactory = { AssetId("asset-new") },
        )

        val result = useCase(
            AssetInput(
                name = "  New Lab  ",
                code = "LAB-044",
                locationId = LocationId("location-lab"),
            ),
        )

        assertTrue(result is AssetSaveResult.ValidationFailed)
        result as AssetSaveResult.ValidationFailed
        assertEquals(listOf(AssetValidationError.DuplicateCode), result.errors)
    }

    @Test
    fun `create asset saves normalized asset`() = runTest {
        val repository = FakeAssetRepository(initialAssets = emptyList())
        val useCase = CreateAssetUseCase(
            assetRepository = repository,
            idFactory = { AssetId("asset-created") },
        )

        val result = useCase(
            AssetInput(
                name = "  Lab 202  ",
                code = " LAB-202 ",
                locationId = LocationId("location-lab"),
            ),
        )

        assertTrue(result is AssetSaveResult.Success)
        val asset = (result as AssetSaveResult.Success).asset
        assertEquals(AssetId("asset-created"), asset.id)
        assertEquals("Lab 202", asset.name)
        assertEquals("LAB-202", asset.code)
        assertEquals(asset, repository.getAsset(asset.id))
    }

    @Test
    fun `create asset validates required fields and stale location`() = runTest {
        val repository = FakeAssetRepository(initialAssets = emptyList())
        val useCase = CreateAssetUseCase(
            assetRepository = repository,
            idFactory = { AssetId("asset-created") },
        )

        val missingFields = useCase(
            AssetInput(
                name = " ",
                code = null,
                locationId = null,
            ),
        )
        val staleLocation = useCase(
            AssetInput(
                name = "Room 202",
                code = null,
                locationId = LocationId("missing-location"),
            ),
        )

        assertTrue(missingFields is AssetSaveResult.ValidationFailed)
        assertEquals(
            listOf(
                AssetValidationError.NameRequired,
                AssetValidationError.LocationRequired,
            ),
            (missingFields as AssetSaveResult.ValidationFailed).errors,
        )
        assertTrue(staleLocation is AssetSaveResult.ValidationFailed)
        assertEquals(
            listOf(AssetValidationError.LocationNotFound),
            (staleLocation as AssetSaveResult.ValidationFailed).errors,
        )
    }

    @Test
    fun `update asset validates duplicate code from another asset`() = runTest {
        val repository = FakeAssetRepository()
        repository.addAsset(
            InspectionTestFixtures.sampleAsset.copy(
                id = AssetId("asset-second"),
                code = "LAB-202",
                locationId = LocationId("location-lab"),
            ),
        )

        val result = UpdateAssetUseCase(repository)(
            InspectionTestFixtures.asset1Id,
            AssetInput(
                name = "Computer Lab I.44",
                code = "LAB-202",
                locationId = LocationId("location-lab"),
            ),
        )

        assertTrue(result is AssetSaveResult.ValidationFailed)
        assertEquals(
            listOf(AssetValidationError.DuplicateCode),
            (result as AssetSaveResult.ValidationFailed).errors,
        )
    }

    @Test
    fun `update asset returns not found for stale id`() = runTest {
        val repository = FakeAssetRepository(initialAssets = emptyList())
        val result = UpdateAssetUseCase(repository)(
            AssetId("missing"),
            AssetInput(
                name = "Lab",
                code = null,
                locationId = LocationId("location-lab"),
            ),
        )

        assertEquals(AssetSaveResult.NotFound, result)
    }

    @Test
    fun `create template saves initial section and checklist item`() = runTest {
        val repository = FakeTemplateRepository()
        val useCase = CreateTemplateUseCase(
            templateRepository = repository,
            idFactory = { TemplateId("template-created") },
        )

        val result = useCase(
            TemplateCreateInput(
                name = "  Lab readiness  ",
                recurrencePolicyDays = 90,
                sectionTitle = "  Equipment  ",
                itemTitle = "  Projector starts  ",
                itemDescription = "Check with the room remote",
                required = true,
                critical = false,
                weight = 2,
                answerType = ChecklistAnswerType.PASS_FAIL_NA,
            ),
        )

        assertTrue(result is TemplateSaveResult.Success)
        val template = (result as TemplateSaveResult.Success).template
        assertEquals(TemplateId("template-created"), template.id)
        assertEquals("Lab readiness", template.name)
        assertEquals(90, template.recurrencePolicyDays)
        assertEquals("Equipment", template.sections.single().title)
        assertEquals("Projector starts", template.sections.single().items.single().title)
        assertEquals(template, repository.getTemplate(template.id))
    }

    @Test
    fun `create template validates required metadata and checklist fields`() = runTest {
        val result = CreateTemplateUseCase(
            templateRepository = FakeTemplateRepository(),
            idFactory = { TemplateId("template-created") },
        )(
            TemplateCreateInput(
                name = " ",
                recurrencePolicyDays = -1,
                sectionTitle = "",
                itemTitle = "",
                itemDescription = null,
                required = true,
                critical = false,
                weight = -1,
                answerType = ChecklistAnswerType.PASS_FAIL_NA,
            ),
        )

        assertTrue(result is TemplateSaveResult.ValidationFailed)
        result as TemplateSaveResult.ValidationFailed
        assertEquals(
            listOf(
                TemplateValidationError.NameRequired,
                TemplateValidationError.RecurrenceInvalid,
                TemplateValidationError.SectionTitleRequired,
                TemplateValidationError.ItemTitleRequired,
                TemplateValidationError.WeightInvalid,
            ),
            result.errors,
        )
    }

    @Test
    fun `update template metadata preserves checklist structure`() = runTest {
        val original = InspectionTestFixtures.sampleTemplate
        val repository = FakeTemplateRepository(mapOf(original.id to original))
        val result = UpdateTemplateMetadataUseCase(repository)(
            original.id,
            TemplateMetadataInput(
                name = "Updated template",
                recurrencePolicyDays = 120,
            ),
        )

        assertTrue(result is TemplateSaveResult.Success)
        val template = (result as TemplateSaveResult.Success).template
        assertEquals("Updated template", template.name)
        assertEquals(120, template.recurrencePolicyDays)
        assertEquals(original.sections, template.sections)
    }

    @Test
    fun `update template metadata validates name and recurrence`() = runTest {
        val original = InspectionTestFixtures.sampleTemplate
        val repository = FakeTemplateRepository(mapOf(original.id to original))

        val result = UpdateTemplateMetadataUseCase(repository)(
            original.id,
            TemplateMetadataInput(
                name = " ",
                recurrencePolicyDays = 0,
            ),
        )

        assertTrue(result is TemplateSaveResult.ValidationFailed)
        assertEquals(
            listOf(
                TemplateValidationError.NameRequired,
                TemplateValidationError.RecurrenceInvalid,
            ),
            (result as TemplateSaveResult.ValidationFailed).errors,
        )
        assertEquals(original, repository.getTemplate(original.id))
    }
}
