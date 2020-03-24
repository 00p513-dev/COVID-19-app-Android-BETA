/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactEventDao {
    @Insert
    fun insert(contactEvent: ContactEvent)

    @Query("SELECT * FROM $TABLE_NAME")
    suspend fun getAll(): List<ContactEvent>
}
