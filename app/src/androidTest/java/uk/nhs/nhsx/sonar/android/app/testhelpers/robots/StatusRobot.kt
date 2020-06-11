package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import android.view.View
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.ExposedSymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewContainsText
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText
import uk.nhs.nhsx.sonar.android.app.testhelpers.stringFromResId
import uk.nhs.nhsx.sonar.android.app.testhelpers.waitForText
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import kotlin.reflect.KClass

class StatusRobot {

    fun <T : UserState> checkActivityIsDisplayed(userState: KClass<T>) {
        val title: Int = when (userState) {
            DefaultState::class -> R.string.status_default_title
            ExposedState::class -> R.string.status_exposed_title
            SymptomaticState::class -> R.string.status_symptomatic_title
            ExposedSymptomaticState::class -> R.string.status_symptomatic_title
            PositiveState::class -> R.string.status_positive_test_title
            else -> throw IllegalArgumentException("StatusRobot: not able to match title: $userState")
        }

        waitForText(title, 6_000)
        checkStatusTitle(title)
    }

    fun clickBookTestCard() {
        onView(withId(R.id.bookTest)).perform(click())
    }

    fun clickCurrentAdviceCard() {
        onView(withId(R.id.readLatestAdvice)).perform(click())
    }

    fun clickNotFeelingWellCard() {
        onView(withId(R.id.feelUnwell)).perform(scrollTo(), click())
    }

    fun clickEnableNotifications() {
        onView(withId(R.id.notificationPanel)).perform(click())
    }

    fun checkStatusTitle(@StringRes stringRes: Int) {
        val stringValue = stringFromResId(stringRes)
        onView(withId(R.id.statusTitle))
            .check(matches(withText(stringValue)))
            .check(matches(isDisplayed()))
    }

    fun checkStatusDescription(state: UserState) {
        val expected = "Please isolate until ${state.until().toUiFormat()}"
        checkViewContainsText(R.id.statusDescription, expected)
    }

    fun checkBookVirusTestCardIsDisplayed() {
        onView(withId(R.id.bookTest)).check(matches(isDisplayed()))
    }

    fun checkBookVirusTestCardIsNotDisplayed() {
        onView(withId(R.id.bookTest)).check(matches(not(isDisplayed())))
    }

    fun checkFinalisingSetup() {
        checkViewHasText(R.id.registrationStatusText, R.string.registration_finalising_setup)
        verifyCheckMySymptomsButton(not(isEnabled()))
    }

    fun checkFeelUnwellIsNotDisplayed() {
        onView(withId(R.id.feelUnwell)).check(matches(not(isDisplayed())))
    }

    fun checkFeelUnwellIsDisplayed() {
        // job retries after at least 10 seconds
        waitForText(R.string.registration_everything_is_working_ok, timeoutInMs = 20000)
        verifyCheckMySymptomsButton(isDisplayed())
        verifyCheckMySymptomsButton(isEnabled())
    }

    fun checkEnableNotificationsIsDisplayed() {
        onView(withId(R.id.notificationPanel)).check(matches(isDisplayed()))
    }

    fun checkEnableNotificationsIsNotDisplayed() {
        onView(withId(R.id.notificationPanel)).check(matches(not(isDisplayed())))
    }

    private fun verifyCheckMySymptomsButton(matcher: Matcher<View>) {
        onView(withId(R.id.feelUnwell)).check(matches(matcher))
    }
}
