package com.topic11.cs426.core.navigation

import android.os.Parcel
import android.os.Parcelable
import com.slack.circuit.runtime.screen.Screen

data object DashboardScreen : Screen, Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    @JvmField
    val CREATOR: Parcelable.Creator<DashboardScreen> = object : Parcelable.Creator<DashboardScreen> {
        override fun createFromParcel(parcel: Parcel): DashboardScreen = DashboardScreen

        override fun newArray(size: Int): Array<DashboardScreen?> = arrayOfNulls(size)
    }
}

data object AssetsScreen : Screen, Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    @JvmField
    val CREATOR: Parcelable.Creator<AssetsScreen> = object : Parcelable.Creator<AssetsScreen> {
        override fun createFromParcel(parcel: Parcel): AssetsScreen = AssetsScreen

        override fun newArray(size: Int): Array<AssetsScreen?> = arrayOfNulls(size)
    }
}

data class AssetDetailScreen(
    val assetId: String,
) : Screen, Parcelable {
    constructor(parcel: Parcel) : this(
        assetId = requireNotNull(parcel.readString()) {
            "AssetDetailScreen requires a persisted assetId."
        },
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(assetId)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AssetDetailScreen> = object : Parcelable.Creator<AssetDetailScreen> {
            override fun createFromParcel(parcel: Parcel): AssetDetailScreen = AssetDetailScreen(parcel)

            override fun newArray(size: Int): Array<AssetDetailScreen?> = arrayOfNulls(size)
        }
    }
}

data class AssetEditorScreen(
    val assetId: String?,
) : Screen, Parcelable {
    constructor(parcel: Parcel) : this(assetId = parcel.readString())

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(assetId)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AssetEditorScreen> = object : Parcelable.Creator<AssetEditorScreen> {
            override fun createFromParcel(parcel: Parcel): AssetEditorScreen = AssetEditorScreen(parcel)

            override fun newArray(size: Int): Array<AssetEditorScreen?> = arrayOfNulls(size)
        }
    }
}

data object TemplatesScreen : Screen, Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    @JvmField
    val CREATOR: Parcelable.Creator<TemplatesScreen> = object : Parcelable.Creator<TemplatesScreen> {
        override fun createFromParcel(parcel: Parcel): TemplatesScreen = TemplatesScreen

        override fun newArray(size: Int): Array<TemplatesScreen?> = arrayOfNulls(size)
    }
}

data class TemplateDetailScreen(
    val templateId: String,
) : Screen, Parcelable {
    constructor(parcel: Parcel) : this(
        templateId = requireNotNull(parcel.readString()) {
            "TemplateDetailScreen requires a persisted templateId."
        },
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(templateId)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TemplateDetailScreen> = object : Parcelable.Creator<TemplateDetailScreen> {
            override fun createFromParcel(parcel: Parcel): TemplateDetailScreen = TemplateDetailScreen(parcel)

            override fun newArray(size: Int): Array<TemplateDetailScreen?> = arrayOfNulls(size)
        }
    }
}

data class TemplateEditorScreen(
    val templateId: String?,
) : Screen, Parcelable {
    constructor(parcel: Parcel) : this(templateId = parcel.readString())

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(templateId)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TemplateEditorScreen> = object : Parcelable.Creator<TemplateEditorScreen> {
            override fun createFromParcel(parcel: Parcel): TemplateEditorScreen = TemplateEditorScreen(parcel)

            override fun newArray(size: Int): Array<TemplateEditorScreen?> = arrayOfNulls(size)
        }
    }
}

data class InspectionScreen(
    val inspectionId: String,
) : Screen, Parcelable {
    constructor(parcel: Parcel) : this(
        inspectionId = requireNotNull(parcel.readString()) {
            "InspectionScreen requires a persisted inspectionId."
        },
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(inspectionId)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<InspectionScreen> = object : Parcelable.Creator<InspectionScreen> {
            override fun createFromParcel(parcel: Parcel): InspectionScreen = InspectionScreen(parcel)

            override fun newArray(size: Int): Array<InspectionScreen?> = arrayOfNulls(size)
        }
    }
}

data object IssuesScreen : Screen, Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    @JvmField
    val CREATOR: Parcelable.Creator<IssuesScreen> = object : Parcelable.Creator<IssuesScreen> {
        override fun createFromParcel(parcel: Parcel): IssuesScreen = IssuesScreen

        override fun newArray(size: Int): Array<IssuesScreen?> = arrayOfNulls(size)
    }
}

data class IssueDetailScreen(
    val issueId: String,
) : Screen, Parcelable {
    constructor(parcel: Parcel) : this(
        issueId = requireNotNull(parcel.readString()) {
            "IssueDetailScreen requires a persisted issueId."
        },
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(issueId)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<IssueDetailScreen> = object : Parcelable.Creator<IssueDetailScreen> {
            override fun createFromParcel(parcel: Parcel): IssueDetailScreen = IssueDetailScreen(parcel)

            override fun newArray(size: Int): Array<IssueDetailScreen?> = arrayOfNulls(size)
        }
    }
}

data object ReportsScreen : Screen, Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    @JvmField
    val CREATOR: Parcelable.Creator<ReportsScreen> = object : Parcelable.Creator<ReportsScreen> {
        override fun createFromParcel(parcel: Parcel): ReportsScreen = ReportsScreen

        override fun newArray(size: Int): Array<ReportsScreen?> = arrayOfNulls(size)
    }
}

data class ReportDetailScreen(
    val inspectionId: String,
) : Screen, Parcelable {
    constructor(parcel: Parcel) : this(
        inspectionId = requireNotNull(parcel.readString()) {
            "ReportDetailScreen requires a persisted inspectionId."
        },
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(inspectionId)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ReportDetailScreen> = object : Parcelable.Creator<ReportDetailScreen> {
            override fun createFromParcel(parcel: Parcel): ReportDetailScreen = ReportDetailScreen(parcel)

            override fun newArray(size: Int): Array<ReportDetailScreen?> = arrayOfNulls(size)
        }
    }
}
