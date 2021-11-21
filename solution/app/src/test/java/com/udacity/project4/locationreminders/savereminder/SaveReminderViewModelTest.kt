package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var viewModel: SaveReminderViewModel

    @get:Rule
    val instantTaskExecRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        stopKoin()

        fakeDataSource = FakeDataSource()
        viewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )
    }

    @Test
    fun viewModel_showsLoadingAtStartAndHidesOnceDone() {
        mainCoroutineRule.pauseDispatcher()

        viewModel.validateAndSaveReminder(validDataItem)
        assertThat(viewModel.showLoading.getOrAwaitValue()).isTrue()

        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue()).isFalse()
    }

    @Test
    fun viewModel_savingValidItemSucceeds() {
        val returnValue = viewModel.validateAndSaveReminder(validDataItem)
        assertThat(returnValue).isTrue()
        assertThat(viewModel.showToast.getOrAwaitValue()).isEqualTo(getString(R.string.reminder_saved))
    }

    @Test
    fun viewModel_savingValidItemNavigatesBack() {
        viewModel.validateAndSaveReminder(validDataItem)
        assertThat(viewModel.navigationCommand.getOrAwaitValue()).isEqualTo(NavigationCommand.Back)
    }

    @Test
    fun viewModel_savingDataItemWithAllNullsWillFail() {
        val returnValue = viewModel.validateAndSaveReminder(dataItemWithAllNulls)
        assertThat(returnValue).isFalse()
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue()).isEqualTo(R.string.err_enter_title)
    }

    @Test
    fun viewModel_savingDataItemWithoutLocationWillFail() {
        val returnValue = viewModel.validateAndSaveReminder(dataItemWithoutLocation)
        assertThat(returnValue).isFalse()
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue()).isEqualTo(R.string.err_select_location)
    }

    @Test
    fun viewModel_savingDataItemWithoutLocationCoordinatesWillFail() {
        val returnValue = viewModel.validateAndSaveReminder(dataItemWithoutCoordinates)
        assertThat(returnValue).isFalse()
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue()).isEqualTo(R.string.err_select_location)
    }

    @Test
    fun viewModel_savingDataItemWithoutRadiousWillFail() {
        val returnValue = viewModel.validateAndSaveReminder(dataItemWithoutRadius)
        assertThat(returnValue).isFalse()
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue()).isEqualTo(R.string.err_select_radius)
    }
}