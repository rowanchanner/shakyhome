package com.sharky.dropbox

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DropBoxSmokeTest {
    @Test fun opensReceiver() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Sharky DropBox")).check(matches(isDisplayed()))
            onView(withText("New pairing code")).check(matches(isDisplayed()))
        }
    }
}
