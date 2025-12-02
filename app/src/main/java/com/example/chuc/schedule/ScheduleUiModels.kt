package com.example.chuc.schedule

/**
 * Lightweight UI models used for schedule selectors.
 */
data class GroupUiModel(
    val code: String,
    val publicId: String? = null,
    val internalId: String? = null,
    val startYear: Int? = null,
    val isActive: Boolean = true
)

data class TeacherUiModel(
    val name: String,
    val publicId: String? = null,
    val internalId: String? = null,
    val isActive: Boolean = true
)

