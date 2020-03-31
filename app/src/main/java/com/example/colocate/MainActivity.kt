/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.colocate.ble.BluetoothService
import com.example.colocate.ble.util.isBluetoothEnabled
import com.example.colocate.isolate.IsolateActivity
import com.example.colocate.registration.RegistrationActivity
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.StatusStorage
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ColocateApplication).applicationComponent.inject(this)

        setContentView(R.layout.activity_main)

        findViewById<AppCompatButton>(R.id.confirm_onboarding).setOnClickListener {
            requestPermissions(
                arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        }

        if (hasLocationPermission(this) && isBluetoothEnabled()) {
            ContextCompat.startForegroundService(this, Intent(this, BluetoothService::class.java))

            navigateTo(statusStorage.get())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION &&
            grantResults.size == 2 &&
            grantResults.first() == PERMISSION_GRANTED &&
            grantResults.last() == PERMISSION_GRANTED
        ) {
            RegistrationActivity.start(this)
            finish()
        } else {
            // TODO see with Design team what we can do current requirement is to stay in this screen
        }
    }
}
