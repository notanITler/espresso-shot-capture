package com.example.espressoshotcapture.capture.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotUserMetadataTest {
    @Test
    fun emptyMetadataIsValid() {
        assertTrue(ShotUserMetadata().isValid())
    }

    @Test
    fun populatedMetadataIsValid() {
        val metadata = ShotUserMetadata(
            rating = 5,
            tasteDirection = TasteDirection.BALANCED,
            grindSetting = "8.10",
            beanName = "Ethiopia Guji",
            notes = "Sweet and clear"
        )

        assertTrue(metadata.isValid())
        assertEquals("8.10", metadata.grindSetting)
    }

    @Test
    fun ratingMustBeNullOrBetweenOneAndFive() {
        assertTrue(ShotUserMetadataValidator.isValidRating(null))
        assertTrue(ShotUserMetadataValidator.isValidRating(1))
        assertTrue(ShotUserMetadataValidator.isValidRating(5))
        assertFalse(ShotUserMetadataValidator.isValidRating(0))
        assertFalse(ShotUserMetadataValidator.isValidRating(6))
    }

    @Test
    fun grindSettingAllowsBlankOrDecimalLikeText() {
        assertTrue(ShotUserMetadataValidator.isValidGrindSetting(null))
        assertTrue(ShotUserMetadataValidator.isValidGrindSetting(""))
        assertTrue(ShotUserMetadataValidator.isValidGrindSetting("  "))
        assertTrue(ShotUserMetadataValidator.isValidGrindSetting("8"))
        assertTrue(ShotUserMetadataValidator.isValidGrindSetting("8.1"))
        assertTrue(ShotUserMetadataValidator.isValidGrindSetting("9.0"))
        assertTrue(ShotUserMetadataValidator.isValidGrindSetting("8.10"))
    }

    @Test
    fun grindSettingRejectsNonDecimalText() {
        assertFalse(ShotUserMetadataValidator.isValidGrindSetting("fine"))
        assertFalse(ShotUserMetadataValidator.isValidGrindSetting("8."))
        assertFalse(ShotUserMetadataValidator.isValidGrindSetting("8.1.2"))
        assertFalse(ShotUserMetadataValidator.isValidGrindSetting("-1.0"))
    }

    @Test
    fun beanNameAndNotesMayBeBlank() {
        val metadata = ShotUserMetadata(beanName = "", notes = "   ")

        assertTrue(metadata.isValid())
    }

    @Test
    fun invalidFieldMakesMetadataInvalid() {
        assertFalse(ShotUserMetadata(rating = 0).isValid())
        assertFalse(ShotUserMetadata(grindSetting = "not-a-number").isValid())
    }
}
