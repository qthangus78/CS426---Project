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

data object TemplatesScreen : Screen, Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    @JvmField
    val CREATOR: Parcelable.Creator<TemplatesScreen> = object : Parcelable.Creator<TemplatesScreen> {
        override fun createFromParcel(parcel: Parcel): TemplatesScreen = TemplatesScreen

        override fun newArray(size: Int): Array<TemplatesScreen?> = arrayOfNulls(size)
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

data object ReportsScreen : Screen, Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    @JvmField
    val CREATOR: Parcelable.Creator<ReportsScreen> = object : Parcelable.Creator<ReportsScreen> {
        override fun createFromParcel(parcel: Parcel): ReportsScreen = ReportsScreen

        override fun newArray(size: Int): Array<ReportsScreen?> = arrayOfNulls(size)
    }
}
