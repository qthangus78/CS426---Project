package com.topic11.cs426.domain.model

data class Location(
    val id: LocationId,
    val name: String,
    val parentId: LocationId? = null,
) {
    init {
        require(name.isNotBlank()) { "Location name cannot be blank." }
    }
}
