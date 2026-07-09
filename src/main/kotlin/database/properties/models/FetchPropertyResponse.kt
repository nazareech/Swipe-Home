package com.swipehome.database.properties.models

import kotlinx.serialization.Serializable

@Serializable
data class FetchPropertyResponse(
    val properties: List<PropertyResponse>
)
@Serializable
data class FetchPropertiesRequest(
    val searchQuery: String
)

@Serializable
data class PropertyResponse(
    val id_property: Int,
    val id_owner: Int,
    val title: String,
    val description: String,
    //--- категорії
    val localization: String,
    //--
    val price: Double,
    val area: Double,
    val rooms: Int,
    //--
    val category: String,
    val subcategory: String,
    val parking: String,
    val pets_allowed: Boolean,
    val elevator: Boolean,
    val furniture: Boolean,
    val building_type: String,
    val status: String,
    //---
    val creates_at: String
)