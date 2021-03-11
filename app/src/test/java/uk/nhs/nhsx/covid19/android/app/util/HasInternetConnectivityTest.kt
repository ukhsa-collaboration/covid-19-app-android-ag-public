package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasInternetConnectivityTest {

    private val context = mockk<Context>()
    private val connectivityManager = mockk<ConnectivityManager>()
    private val network = mockk<Network>()
    private val networkCapabilities = mockk<NetworkCapabilities>()

    private val testSubject = HasInternetConnectivity(context)

    @Before
    fun setUp() {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(TRANSPORT_WIFI) } returns false
        every { networkCapabilities.hasTransport(TRANSPORT_CELLULAR) } returns false
        every { networkCapabilities.hasTransport(TRANSPORT_ETHERNET) } returns false
        every { networkCapabilities.hasTransport(TRANSPORT_BLUETOOTH) } returns false
    }

    @Test
    fun `when connectivity manager is null then return false`() {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns null

        assertFalse { testSubject.invoke() }
    }

    @Test
    fun `when data network object is null then return false`() {
        every { connectivityManager.activeNetwork } returns null

        assertFalse { testSubject.invoke() }
    }

    @Test
    fun `when network capabilities do not offer any of the appropriate transport types return false`() {
        assertFalse { testSubject.invoke() }
    }

    @Test
    fun `when internet access via wifi is available return true`() {
        every { networkCapabilities.hasTransport(TRANSPORT_WIFI) } returns true

        assertTrue { testSubject.invoke() }
    }

    @Test
    fun `when internet access via cellular network is available return true`() {
        every { networkCapabilities.hasTransport(TRANSPORT_CELLULAR) } returns true

        assertTrue { testSubject.invoke() }
    }

    @Test
    fun `when internet access via ethernet network is available return true`() {
        every { networkCapabilities.hasTransport(TRANSPORT_ETHERNET) } returns true

        assertTrue { testSubject.invoke() }
    }

    @Test
    fun `when internet access shared via bluetooth is available return true`() {
        every { networkCapabilities.hasTransport(TRANSPORT_BLUETOOTH) } returns true

        assertTrue { testSubject.invoke() }
    }
}
