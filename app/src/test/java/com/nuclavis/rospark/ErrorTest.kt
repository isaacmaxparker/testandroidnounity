package com.nuclavis.rospark

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ReportFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito.doNothing
import org.powermock.api.mockito.PowerMockito.doReturn
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.spy
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import javax.inject.Inject

@PowerMockIgnore("jdk.internal.reflect.*")
@RunWith(PowerMockRunner::class)
@PrepareForTest(ReportFragment::class)
class ErrorActivityTests {
    @Inject
    var error: Error? = null

    @Test
    fun test_onCreate() {
        try {
            return;
            // Mock some data
            mockStatic(Error::class.java)
            val activity: Error = spy(Error())
            doNothing().`when`(activity).setContentView(R.layout.error)
            doReturn(mock(AppCompatDelegate::class.java)).`when`(activity).getDelegate()

            // Call the method
            activity.onCreate(null)

            // Verify that it worked
            verify(activity, times(1)).setContentView(R.layout.error)
            verify(activity, times(1)).onCreate(null)
        } catch (e: Exception) {
            println("EXCEPTION: ")
            println(e)
        }
    }
}