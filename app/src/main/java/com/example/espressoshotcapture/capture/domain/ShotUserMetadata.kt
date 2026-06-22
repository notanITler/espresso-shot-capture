package com.example.espressoshotcapture.capture.domain

enum class TasteDirection {
    SOUR,
    BALANCED,
    BITTER
}

data class ShotUserMetadata(
    val rating: Int? = null,
    val tasteDirection: TasteDirection? = null,
    val grindSetting: String? = null,
    val beanName: String? = null,
    val notes: String? = null
) {
    fun isValid(): Boolean = ShotUserMetadataValidator.isValid(this)
}

object ShotUserMetadataValidator {
    private val decimalLikeGrindSetting = Regex("""\d+(\.\d+)?""")

    fun isValid(metadata: ShotUserMetadata): Boolean =
        isValidRating(metadata.rating) && isValidGrindSetting(metadata.grindSetting)

    fun isValidRating(rating: Int?): Boolean =
        rating == null || rating in 1..5

    fun isValidGrindSetting(grindSetting: String?): Boolean =
        grindSetting.isNullOrBlank() || decimalLikeGrindSetting.matches(grindSetting.trim())
}
