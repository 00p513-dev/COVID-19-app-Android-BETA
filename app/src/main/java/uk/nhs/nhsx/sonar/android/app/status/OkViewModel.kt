/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.analytics.onboardingCompleted
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.RegistrationState.Complete
import uk.nhs.nhsx.sonar.android.app.status.RegistrationState.InProgress
import uk.nhs.nhsx.sonar.android.app.util.map
import javax.inject.Inject

class OkViewModel @Inject constructor(
    private val onboardingStatusProvider: OnboardingStatusProvider,
    private val sonarIdProvider: SonarIdProvider,
    private val registrationManager: RegistrationManager,
    private val analytics: SonarAnalytics
) : ViewModel() {

    fun viewState(): LiveData<RegistrationState> =
        sonarIdProvider
            .hasProperSonarIdLiveData()
            .map { hasProperSonarId ->
                if (hasProperSonarId) Complete else InProgress
            }

    fun onStart() {
        if (!onboardingStatusProvider.get()) {
            analytics.trackEvent(onboardingCompleted())
            onboardingStatusProvider.set(true)
        }

        if (!sonarIdProvider.hasProperSonarId()) {
            registrationManager.register()
        }
    }
}

enum class RegistrationState {
    Complete,
    InProgress
}
