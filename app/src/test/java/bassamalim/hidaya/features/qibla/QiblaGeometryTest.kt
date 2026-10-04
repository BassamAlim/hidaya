package bassamalim.hidaya.features.qibla

import bassamalim.hidaya.core.models.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Test

/** Qibla direction (degrees from true north) and distance to the Kaaba for a few cities. */
class QiblaGeometryTest {

    @Test
    fun `Cairo faces south east`() {
        assertEquals(136.1f, qiblaBearing(Coordinates(30.0444, 31.2357)), 0.5f)
    }

    @Test
    fun `London faces east south east`() {
        assertEquals(119.0f, qiblaBearing(Coordinates(51.5074, -0.1278)), 0.5f)
    }

    @Test
    fun `Jakarta faces west north west`() {
        assertEquals(295.1f, qiblaBearing(Coordinates(-6.2088, 106.8456)), 0.5f)
    }

    @Test
    fun `distance from Cairo`() {
        assertEquals(1287.0, kaabaDistanceKm(Coordinates(30.0444, 31.2357)), 5.0)
    }

    // Far east-west of Mecca, where swapping latitude and longitude used to cost ~375 km
    @Test
    fun `distance from London`() {
        assertEquals(4794.0, kaabaDistanceKm(Coordinates(51.5074, -0.1278)), 5.0)
    }

    @Test
    fun `distance from Jakarta`() {
        assertEquals(7920.0, kaabaDistanceKm(Coordinates(-6.2088, 106.8456)), 5.0)
    }

}
