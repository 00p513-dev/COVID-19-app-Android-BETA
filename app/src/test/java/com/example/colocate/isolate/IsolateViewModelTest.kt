/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.isolate

import com.example.colocate.persistence.CoLocationDataProvider
import com.example.colocate.persistence.ResidentIdProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationData

class IsolateViewModelTest {

    private val coLocationApi = mockk<CoLocationApi>(relaxed = true)
    private val coLocationDataProvider = mockk<CoLocationDataProvider>()
    private val residentIdProvider = mockk<ResidentIdProvider>()
    private val testSubject = IsolateViewModel(
        coLocationApi,
        Dispatchers.Unconfined,
        coLocationDataProvider
    )

    companion object {
        private const val RESIDENT_ID = "80baf81b-8afd-47e9-9915-50691525c910"
    }

    @Test
    fun onNotifyCallsCoLocationApi() {
        runBlocking {
            val events = JSONArray().apply {
                put(0, "foo")
                put(1, "bar")
            }
            val coLocationData = CoLocationData(RESIDENT_ID, events)
            coEvery { coLocationDataProvider.getData() } returns coLocationData
            every { residentIdProvider.getResidentId() } returns RESIDENT_ID

            testSubject.onNotifyClick()

            verify {
                coLocationApi.save(eq(coLocationData), any(), any())
            }
        }
    }
}
