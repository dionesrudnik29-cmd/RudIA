package com.rud.ia

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelConfigTest {

    private val gb = 1024L * 1024L * 1024L

    @Test
    fun contextNeverPasses2048EvenWithLotsOfFreeRam() {
        assertEquals(2048, ModelConfig.contextSizeFor(6 * gb))
    }

    @Test
    fun contextShrinksWhenRamIsLow() {
        assertEquals(1536, ModelConfig.contextSizeFor(gb + gb / 2))
        assertEquals(1024, ModelConfig.contextSizeFor(gb / 2))
    }

    @Test
    fun eightCorePhoneUsesOnlyTheFourFastCores() {
        assertEquals(4, ModelConfig.threadsFor(8))
    }

    @Test
    fun smallPhonesStillUseAtLeastTwoThreads() {
        assertEquals(2, ModelConfig.threadsFor(2))
    }
}
