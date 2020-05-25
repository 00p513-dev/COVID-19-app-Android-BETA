/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.AT_RISK
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.OK
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.after
import uk.nhs.nhsx.sonar.android.app.util.atSevenAm
import uk.nhs.nhsx.sonar.android.app.util.latest
import uk.nhs.nhsx.sonar.android.app.util.toUtc

sealed class UserState {

    companion object {
        const val NO_DAYS_IN_SYMPTOMATIC = 7
        const val NO_DAYS_IN_EXPOSED = 14

        fun default(): DefaultState =
            DefaultState

        fun exposed(today: LocalDate = LocalDate.now()): ExposedState =
            ExposedState(today.atSevenAm().toUtc(), today.after(NO_DAYS_IN_EXPOSED - 1).days().toUtc())

        fun checkin(
            since: DateTime,
            symptoms: NonEmptySet<Symptom>,
            today: LocalDate = LocalDate.now()
        ): CheckinState =
            CheckinState(
                since,
                today.after(1).day().toUtc(),
                symptoms
            )

        fun symptomatic(
            symptomsDate: LocalDate,
            symptoms: NonEmptySet<Symptom>,
            today: LocalDate = LocalDate.now()
        ): SymptomaticState {
            val suggested = symptomsDate.after(NO_DAYS_IN_SYMPTOMATIC).days()
            val tomorrow = today.after(1).day()

            // if symptomsDate > 7 days ago then symptomatic state is until tomorrow
            // if symptomsDate <= 7 days ago then symptomatic state is until suggested
            val until = latest(suggested, tomorrow)

            return SymptomaticState(
                symptomsDate.atSevenAm().toUtc(),
                until.toUtc(),
                symptoms
            )
        }

        fun positive(
            testDate: DateTime,
            symptoms: NonEmptySet<Symptom>,
            today: LocalDate = LocalDate.now()
        ): PositiveState {
            val suggested = testDate.toLocalDate().after(NO_DAYS_IN_SYMPTOMATIC).days()
            val tomorrow = today.after(1).day()

            // if symptomsDate > 7 days ago then positive state is until tomorrow
            // if symptomsDate <= 7 days ago then positive state is until suggested
            val until = latest(suggested, tomorrow)

            return PositiveState(
                testDate.atSevenAm().toUtc(),
                until.toUtc(),
                symptoms
            )
        }
    }

    fun since(): DateTime? =
        when (this) {
            is SymptomaticState -> since
            is CheckinState -> since
            is ExposedState -> since
            is PositiveState -> since
            else -> null
        }

    fun until(): DateTime? =
        when (this) {
            is DefaultState -> null
            is ExposedState -> until
            is SymptomaticState -> until
            is CheckinState -> until
            is PositiveState -> until
        }

    fun hasExpired(): Boolean =
        until()?.isBeforeNow == true

    fun displayState(): DisplayState =
        when (this) {
            is DefaultState -> OK
            is ExposedState -> AT_RISK
            is SymptomaticState -> ISOLATE
            is CheckinState -> ISOLATE
            is PositiveState -> ISOLATE
        }

    fun scheduleCheckInReminder(reminders: Reminders) =
        when {
            (this is SymptomaticState && !hasExpired()) -> reminders.scheduleCheckInReminder(until)
            (this is PositiveState && !hasExpired()) -> reminders.scheduleCheckInReminder(until)
            else -> Unit
        }

    fun symptoms(): Set<Symptom> =
        when (this) {
            is SymptomaticState -> symptoms
            is PositiveState -> symptoms
            is CheckinState -> symptoms
            else -> emptySet()
        }
}

// Initial state
object DefaultState : UserState() {
    override fun toString(): String = "DefaultState"
}

// State when you have been in contact with someone in SymptomaticState
data class ExposedState(val since: DateTime, val until: DateTime) : UserState()

// State when you initially have symptoms. Prompted after 1 to 7 days to checkin.
data class SymptomaticState(
    val since: DateTime,
    val until: DateTime,
    val symptoms: NonEmptySet<Symptom>
) : UserState()

// State after first checkin from SymptomaticState, does not get prompted again.
data class CheckinState(
    val since: DateTime,
    val until: DateTime,
    val symptoms: NonEmptySet<Symptom>
) : UserState()

// State when user has tested and the test result was positive
data class PositiveState(
    val since: DateTime,
    val until: DateTime,
    val symptoms: NonEmptySet<Symptom>
) : UserState()

enum class DisplayState {
    OK, // Default
    AT_RISK, // Exposed
    ISOLATE // Symptomatic
}

enum class Symptom(val value: String) {
    COUGH("COUGH"),
    TEMPERATURE("TEMPERATURE"),
    ANOSMIA("ANOSMIA"),
    SNEEZE("SNEEZE"),
    NAUSEA("NAUSEA");

    companion object {
        fun fromValue(value: String) = values().firstOrNull { it.value == value }
    }
}
