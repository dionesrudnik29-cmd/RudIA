package com.rud.ia

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {

    @Test
    fun bigPhotoIsReducedTo768OnTheLongestSide() {
        assertEquals(768 to 576, ImageSizing.targetSize(4000, 3000))
        assertEquals(576 to 768, ImageSizing.targetSize(3000, 4000))
    }

    @Test
    fun smallPhotoIsNeverEnlarged() {
        assertEquals(500 to 400, ImageSizing.targetSize(500, 400))
    }

    @Test
    fun hugePhotoIsOpenedAtReducedSizeToSaveMemory() {
        assertEquals(4, ImageSizing.sampleSize(4000, 3000))
        assertEquals(1, ImageSizing.sampleSize(800, 600))
    }
}
