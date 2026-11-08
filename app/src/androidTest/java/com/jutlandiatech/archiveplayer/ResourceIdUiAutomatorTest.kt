package com.jutlandiatech.archiveplayer

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** R69: test tags are exposed as resource IDs, so UI Automator finds elements without localized text. */
@RunWith(AndroidJUnit4::class)
class ResourceIdUiAutomatorTest {
    @Test fun R69_setupElementsFoundByResourceId() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val context = instrumentation.targetContext
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
        assertNotNull(device.wait(Until.findObject(By.res("setup_root")), 10_000))
        assertNotNull(device.findObject(By.res("setup_language_en")))
        assertNotNull(device.findObject(By.res("setup_continue_button")))
    }
}
