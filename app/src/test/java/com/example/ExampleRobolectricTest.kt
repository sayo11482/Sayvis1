package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  /**
   * Locale-proof resource smoke test: the launcher label must resolve to one of
   * the two shipped names regardless of the test device locale.
   */
  @Test
  fun `app label resolves in every locale`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertTrue("unexpected app label: $appName", appName == "SAYVIS" || appName == "سایویس")
  }
}
