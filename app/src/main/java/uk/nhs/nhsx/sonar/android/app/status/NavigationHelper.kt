/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.app.Activity

fun Activity.navigateTo(state: UserState) {
    if (state is DefaultState && this !is OkActivity) {
        OkActivity.start(this)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    if (state is EmberState && this !is AtRiskActivity) {
        AtRiskActivity.start(this)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    if ((state is RedState || state is CheckinState) && this !is IsolateActivity) {
        IsolateActivity.start(this)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    if (state is RecoveryState && this !is OkActivity) {
        OkActivity.start(this)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
