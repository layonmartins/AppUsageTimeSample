package com.layon.usageapptimesample

import android.util.Log
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun convertTimeDurationString(timeStamp: Long) : String {
    val duration : Duration = timeStamp.milliseconds
    return duration.toString()
}

fun getShortDate(ts:Long?):String{
    if(ts == null) return ""
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.timeInMillis = ts
    return android.text.format.DateFormat.format("hh:mm:ss:mmm", calendar).toString()
}

fun getFullDate(ts:Long?):String{
    if(ts == null) return ""
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.timeInMillis = ts
    return android.text.format.DateFormat.format("E, dd MMM yyyy hh:mm:ss:mmm", calendar).toString()
}
