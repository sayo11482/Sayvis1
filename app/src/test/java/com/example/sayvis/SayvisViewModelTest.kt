package com.example.sayvis

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.sayvis.ui.SayvisViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Regression test for the launch crash reported on device:
 *
 * java.lang.NullPointerException: Attempt to invoke 'StateFlow.collect(...)' on a null
 * object reference at SayvisViewModel.<init> (SayvisViewModel.kt:75)
 *
 * Root cause: an `init` block declared BEFORE the `contextSnapshot` property tried to
 * collect it. Kotlin runs property initializers and init blocks in textual order, so the
 * StateFlow was still null during construction. Constructing the ViewModel in this test
 * reproduces the exact crash path because viewModelScope uses Dispatchers.Main.immediate,
 * which executes the launched coroutine synchronously on the main thread.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SayvisViewModelTest {

    @Test
    fun `view model constructs without crashing and exposes initialized state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        // Before the fix this single line crashed with the NPE above.
        val viewModel = SayvisViewModel(app)

        // Drain the main looper so any coroutines dispatched to Main run to completion.
        shadowOf(Looper.getMainLooper()).idle()

        assertNotNull(viewModel.repository)
        assertNotNull(viewModel.awareEngine)
        assertNotNull(viewModel.contextSnapshot.value)
        assertNotNull(viewModel.avatarState.value)

        // The StateFlow must serve its initialValue even before any UI subscribes.
        assertEquals(2, viewModel.contextSnapshot.value.activeMissionsCount)
        assertEquals("Online", viewModel.contextSnapshot.value.networkStatus)
    }
}
