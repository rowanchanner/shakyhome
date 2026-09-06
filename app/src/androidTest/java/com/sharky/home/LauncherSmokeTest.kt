package com.sharky.home

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matchers.equalTo

@RunWith(AndroidJUnit4::class)
class LauncherSmokeTest {
    @Test fun launchNavigateAndRecreate() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withText("Home")).check(matches(isDisplayed()))
            onView(withTagValue(equalTo("nav:Settings"))).perform(click())
            onView(withText("Make it yours")).check(matches(isDisplayed()))
            scenario.recreate()
            onView(withText("Make it yours")).check(matches(isDisplayed()))
            onView(withTagValue(equalTo("nav:Apps"))).perform(click())
            onView(withText("Your apps")).check(matches(isDisplayed()))
            onView(withTagValue(equalTo("nav:Home"))).perform(click())
            onView(withText("Sharky apps")).check(matches(isDisplayed()))
        }
    }
}
