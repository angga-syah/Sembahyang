package id.linuwih.sholat.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {

    @Test
    fun `versi dibandingkan per angka, bukan per huruf`() {
        assertTrue(AppUpdater.compareVersions("0.2.10", "0.2.9") > 0)
        assertTrue(AppUpdater.compareVersions("1.0", "0.9.9") > 0)
        assertTrue(AppUpdater.compareVersions("0.1.0", "0.2.0") < 0)
    }

    @Test
    fun `angka nol di belakang tidak dianggap versi berbeda`() {
        assertEquals(0, AppUpdater.compareVersions("0.2", "0.2.0"))
        assertEquals(0, AppUpdater.compareVersions("0.2.0", "0.2.0"))
    }
}
