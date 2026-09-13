package com.example.sayvis

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.sayvis.ui.SayvisViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        val looper = shadowOf(Looper.getMainLooper())
        looper.idle()

        assertNotNull(viewModel.repository)
        assertNotNull(viewModel.awareEngine)
        assertNotNull(viewModel.contextSnapshot.value)
        assertNotNull(viewModel.avatarState.value)

        // Since v1.2 the context snapshot is a LIVE combine over the real database and
        // telemetry (the AWARE engine subscribes to it from init, so WhileSubscribed
        // activates immediately). Wait, bounded, for the first real emission to replace
        // the placeholder initial value, then assert the real fresh-install state.
        val deadline = System.currentTimeMillis() + 10_000
        while (viewModel.contextSnapshot.value.activeMissionsCount != 0 &&
            System.currentTimeMillis() < deadline
        ) {
            looper.idle()
            Thread.sleep(25)
        }
        // A fresh install has zero active missions - the placeholder demo value (2) is gone.
        assertEquals(0, viewModel.contextSnapshot.value.activeMissionsCount)
        // networkStatus always comes from the repository's fixed vocabulary.
        assertTrue(
            viewModel.contextSnapshot.value.networkStatus == "Online - SAYVIS Gateway Secure Enclave" ||
                viewModel.contextSnapshot.value.networkStatus == "Offline - Sovereign Local Safe Mode"
        )
    }
}
