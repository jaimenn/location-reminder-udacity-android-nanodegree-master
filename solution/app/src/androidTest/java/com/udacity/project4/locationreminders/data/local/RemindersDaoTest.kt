package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase
    private lateinit var dao: RemindersDao

    private val reminderData = ReminderDTO(
        title = "Test",
        description = "testing",
        location = "test",
        latitude = 2.343434343,
        longitude = 1.231424,
        radius = 300f
    )

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        dao = database.reminderDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertIntoDBSucceeds() = runBlockingTest {
        dao.saveReminder(reminderData)

        assertThat(dao.getReminders()).hasSize(1)
        assertThat(dao.getReminders()).contains(reminderData)
    }

    @Test
    fun retrieveFromDBSucceeds() = runBlockingTest {
        dao.saveReminder(reminderData)

        val reminder = dao.getReminderById(reminderData.id)

        assertThat(reminder).isNotNull()
        assertThat(reminder?.title).isEqualTo(reminderData.title)
        assertThat(reminder?.description).isEqualTo(reminderData.description)
        assertThat(reminder?.location).isEqualTo(reminderData.location)
        assertThat(reminder?.latitude).isEqualTo(reminderData.latitude)
        assertThat(reminder?.longitude).isEqualTo(reminderData.longitude)
        assertThat(reminder?.radius).isEqualTo(reminderData.radius)
    }

    @Test
    fun deleteFromDBSucceeds() = runBlockingTest {
        dao.saveReminder(reminderData)
        assertThat(dao.getReminders()).hasSize(1)

        dao.deleteAllReminders()
        assertThat(dao.getReminders()).isEmpty()
    }
}