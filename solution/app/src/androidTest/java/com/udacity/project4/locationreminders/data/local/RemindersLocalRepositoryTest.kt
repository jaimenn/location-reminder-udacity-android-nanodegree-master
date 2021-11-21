package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

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
        ).allowMainThreadQueries().build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertReminderSucceeds() = runBlocking {
        repository.saveReminder(reminderData)

        val result = repository.getReminders()
        assertThat(result).isInstanceOf(Result.Success::class.java)

        result as Result.Success

        assertThat(result.data).isNotEmpty()
        assertThat(result.data).hasSize(1)
    }

    @Test
    fun retrieveExistingReminderSucceeds() = runBlocking {
        repository.saveReminder(reminderData)

        val result = repository.getReminder(reminderData.id)
        assertThat(result).isInstanceOf(Result.Success::class.java)

        result as Result.Success

        assertThat(result.data).isNotNull()
        assertThat(result.data.title).isEqualTo(reminderData.title)
        assertThat(result.data.description).isEqualTo(reminderData.description)
        assertThat(result.data.location).isEqualTo(reminderData.location)
        assertThat(result.data.latitude).isEqualTo(reminderData.latitude)
        assertThat(result.data.longitude).isEqualTo(reminderData.longitude)
        assertThat(result.data.radius).isEqualTo(reminderData.radius)
    }

    @Test
    fun retrieveNonExistingReminderFails() = runBlocking {
        val result = repository.getReminder(reminderData.id)
        assertThat(result).isInstanceOf(Result.Error::class.java)

        result as Result.Error

        assertThat(result.message).isEqualTo("Reminder not found!")
        assertThat(result.statusCode).isNull()
    }

    @Test
    fun deleteRemindersSucceeds() = runBlocking {
        repository.saveReminder(reminderData)
        repository.deleteAllReminders()

        val result = repository.getReminders()
        assertThat(result).isInstanceOf(Result.Success::class.java)

        result as Result.Success
        assertThat(result.data).isEmpty()
    }
}