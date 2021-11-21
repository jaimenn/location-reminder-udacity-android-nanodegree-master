package com.udacity.project4.locationreminders.reminderslist

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import java.io.Serializable
import java.util.*

/**
 * data class acts as a data mapper between the DB and the UI
 */
data class ReminderDataItem(
    val title: String?,
    val description: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radius: Float?,
    val id: String = UUID.randomUUID().toString()
) : Serializable {

    val printableLocation: String
        get() {
            if (location != null) {
                return location
            }

            return "Lat: $latitude Lon: $longitude"
        }
}

fun ReminderDataItem.toDTO() = ReminderDTO(
    title = title,
    description = description,
    location = location,
    latitude = latitude,
    longitude = longitude,
    radius = radius
)