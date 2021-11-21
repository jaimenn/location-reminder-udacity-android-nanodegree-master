package com.udacity.project4.locationreminders.util

import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

val validDataItem = ReminderDataItem(
    title = "Take AlienX on a Date",
    description = "Testing",
    location = "Area 51",
    latitude = 2.344234234,
    longitude = 1.34234234,
    radius = 1000f
)

val dataItemWithAllNulls = ReminderDataItem(
    title = null,
    description = null,
    location = null,
    latitude = null,
    longitude = null,
    radius = null
)

val dataItemWithoutLocation = ReminderDataItem(
    title = "Test",
    description = null,
    location = null,
    latitude = null,
    longitude = null,
    radius = null
)

val dataItemWithoutCoordinates = ReminderDataItem(
    title = "test",
    description = "test",
    location = "test",
    latitude = null,
    longitude = null,
    radius = null
)

val dataItemWithoutRadius = ReminderDataItem(
    title = "test",
    description = "test",
    location = "test",
    latitude = 2.213123123,
    longitude = 1.023432423,
    radius = null
)