/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.os.ParcelUuid
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LongLiveConnectionScan @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val saveContactWorker: SaveContactWorker,
    private val dateProvider: () -> Date = { Date() },
    private val period: Long = 10_000
) : Scanner {
    private val coLocateServiceUuidFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
        .build()

    private var compositeDisposable = CompositeDisposable()

    /*
     When the iPhone app goes into the background iOS changes how services are advertised:
  
         1) The service uuid is now null
         2) The information to identify the service is encoded into the manufacturing data in a
         unspecified/undocumented way.
  
        The below filter is based on observation of the advertising packets produced by an iPhone running
        the app in the background.
       */
    private val coLocateBackgroundedIPhoneFilter = ScanFilter.Builder()
        .setServiceUuid(null)
        .setManufacturerData(
            76,
            byteArrayOf(
                0x01, // 0
                0x00, // 1
                0x00, // 2
                0x00, // 3
                0x00, // 4
                0x00, // 5
                0x00, // 6
                0x00, // 7
                0x00, // 8
                0x00, // 9
                0x40, // 10
                0x00, // 11
                0x00, // 12
                0x00, // 13
                0x00, // 14
                0x00, // 15
                0x00 // 16
            )
        )
        .build()

    private val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()

    private val macAddressToRecord = mutableMapOf<String, SaveContactWorker.Record>()

    override fun start(coroutineScope: CoroutineScope) {
        val scanDisposable = rxBleClient.scanBleDevices(
            settings,
            coLocateBackgroundedIPhoneFilter,
            coLocateServiceUuidFilter
        )
            .filter { it.bleDevice.connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED }
            .subscribe(
                { result -> connectAndCaptureEvents(result, coroutineScope) },
                ::onScanError
            )
        compositeDisposable.add(scanDisposable)
    }

    private fun connectAndCaptureEvents(
        result: ScanResult,
        coroutineScope: CoroutineScope
    ) {
        val macAddress = result.bleDevice.macAddress
        val connectionDisposable = result.bleDevice.establishConnection(false)
            .flatMap { connection -> captureContactEvents(connection, macAddress) }
            .subscribe(::onReadSuccess) { e -> onDisconnect(e, macAddress, coroutineScope) }
        compositeDisposable.add(connectionDisposable)
    }

    private fun captureContactEvents(
        connection: RxBleConnection,
        macAddress: String
    ): Observable<Event> {
        return Observable.combineLatest(
            readIdentifierAndCreateRecord(connection, macAddress),
            readRssiPeriodically(connection),
            createEvent(macAddress)
        )
    }

    private fun onDisconnect(
        e: Throwable?,
        macAddress: String,
        coroutineScope: CoroutineScope
    ) {
        Timber.e(e, "Failed to read from remote device with mac-address: $macAddress")
        saveRecord(macAddress, coroutineScope)
    }

    private fun saveRecord(
        macAddress: String,
        coroutineScope: CoroutineScope
    ) {
        val record = macAddressToRecord.remove(macAddress)
        if (record != null) {
            val duration = (dateProvider().time - record.timestamp.time) / 1000
            val finalRecord = record.copy(duration = duration)
            Timber.d("Save record: $finalRecord")
            saveContactWorker.saveContactEventV2(
                coroutineScope,
                finalRecord
            )
        }
    }

    private fun createEvent(
        macAddress: String
    ): BiFunction<Identifier, Int, Event> {
        return BiFunction<Identifier, Int, Event> { identifier, rssi ->
            Event(
                macAddress,
                identifier,
                rssi
            )
        }
    }

    private fun readRssiPeriodically(connection: RxBleConnection) =
        Observable.interval(0, period, TimeUnit.MILLISECONDS)
            .flatMapSingle { connection.readRssi() }

    private fun readIdentifierAndCreateRecord(connection: RxBleConnection, macAddress: String) =
        connection.readCharacteristic(DEVICE_CHARACTERISTIC_UUID)
            .map { bytes -> Identifier.fromBytes(bytes) }.toObservable()
            .doOnNext { identifier ->
                macAddressToRecord[macAddress] =
                    SaveContactWorker.Record(
                        timestamp = dateProvider(),
                        sonarId = identifier
                    )
            }

    override fun stop() {
        compositeDisposable.clear()
    }

    private fun onScanError(e: Throwable) = Timber.e("Scan failed with: $e")

    private fun onReadSuccess(event: Event) {
        Timber.d("Scanning Saving: $event")
        val record = macAddressToRecord[event.macAddress]
        record?.rssiValues?.add(event.rssi)
    }

    private data class Event(
        val macAddress: String,
        val identifier: Identifier,
        val rssi: Int
    ) {
        override fun toString() = "Event[identifier: ${identifier.asString}, rssi: $rssi]"
    }
}