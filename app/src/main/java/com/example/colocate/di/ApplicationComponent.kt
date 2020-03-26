/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di

import com.example.colocate.DiagnoseActivity
import com.example.colocate.MainActivity
import com.example.colocate.RegistrationNotificationService
import com.example.colocate.ble.BluetoothService
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.di.module.StatusModule
import com.example.colocate.isolate.IsolateActivity
import dagger.Component
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule

@Component(
    modules = [
        PersistenceModule::class,
        AppModule::class,
        BluetoothModule::class,
        NetworkModule::class,
        EncryptionKeyStorageModule::class,
        StatusModule::class
    ]
)
interface ApplicationComponent {
    fun inject(bluetoothService: BluetoothService)
    fun inject(isolateActivity: IsolateActivity)
    fun inject(diagnoseActivity: DiagnoseActivity)
    fun inject(mainActivity: MainActivity)
    fun inject(registrationNotificationService: RegistrationNotificationService)
}
