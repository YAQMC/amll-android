package dev.yaqmc.amll.ui

/**
 * Test-only render counters for the JVM performance harness. Disabled by default; when disabled
 * every hook is a single volatile read. Never enabled by production code.
 */
internal object AmllRenderStats {
    @Volatile
    @JvmField
    var enabled: Boolean = false

    @JvmField var playerCompositions = 0L
    @JvmField var groupCompositions = 0L
    @JvmField var karaokeCompositions = 0L
    @JvmField var karaokeDraws = 0L
    @JvmField var seekEpochs = 0L

    fun reset() {
        playerCompositions = 0
        groupCompositions = 0
        karaokeCompositions = 0
        karaokeDraws = 0
        seekEpochs = 0
    }
}
