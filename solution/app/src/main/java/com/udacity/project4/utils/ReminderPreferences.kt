package com.udacity.project4.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.google.gson.Gson
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

object ReminderPreferences {

    private val KEY = ReminderDataItem::class.simpleName

    fun get(context: Context): ReminderDataItem? {
        val mPrefs: SharedPreferences = context.getSharedPreferences(KEY, MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(ReminderDataItem::class.java.simpleName, "")
        return gson.fromJson(json, ReminderDataItem::class.java)
    }

    fun set(context: Context, reminder: ReminderDataItem) {
        var mPrefs: SharedPreferences = context.getSharedPreferences(KEY, MODE_PRIVATE)
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        val gson = Gson()
        val json = gson.toJson(reminder)
        prefsEditor.putString(ReminderDataItem::class.simpleName, json)
        prefsEditor.commit()
    }

    fun clear(context: Context) {
        var mPrefs: SharedPreferences = context.getSharedPreferences(KEY, MODE_PRIVATE)
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.putString(ReminderDataItem::class.simpleName, null)
        prefsEditor.commit()
    }
}
