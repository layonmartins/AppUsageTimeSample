package com.layon.usageapptimesample

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.layon.usageapptimesample.ui.theme.UsageAppTimeSampleTheme

const val TAG = "layonlog"
const val intervalMilliseconds: Long = (1*60*60*1000) // 1 hours in milliseconds


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UsageAppTimeSampleTheme {
                MainScreen(context = this)
            }
        }
    }

}

// The `PACKAGE_USAGE_STATS` permission is a not a runtime permission and hence cannot be
// requested directly using `ActivityCompat.requestPermissions`. All special permissions
// are handled by `AppOpsManager`.
fun hasPackageUsageStatsPermission(context: Context): Boolean {
    val appOpsManager: AppOpsManager? =
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager?
    val mode = appOpsManager!!.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(), context.packageName
    )
    return if (mode == AppOpsManager.MODE_ALLOWED) true else false
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, context: Context) {
    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val hasPermission = remember { mutableStateOf(false) }
        ComposableLifecycle { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    hasPermission.value = hasPackageUsageStatsPermission(context.applicationContext)
                }
                else -> {}
            }
        }
        if (hasPermission.value) {
            //UsageEventsScreen(getEventsData(LocalContext.current, intervalMilliseconds))
            UsageEventsScreen(getEventsDataGroupedByPackages(LocalContext.current, intervalMilliseconds))
        } else {
            RequestUsageStatsPermissionScreen()
        }
    }
}

@Composable
fun RequestUsageStatsPermissionScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("This app needs Android's Usage Access Permission")
        Button(onClick = {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                context.startActivity(this)
            }
        }) { Text("Allow usage access") }
    }
}

@Preview
@Composable
fun showRequestUsageStatsPermission() {
    RequestUsageStatsPermissionScreen()
}

// Simple method to get all events data history
private fun getEventsData(context: Context, intervalMilliseconds: Long) : List<Triple<String, String, Long>> {
    val currentTime = System.currentTimeMillis()
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    // The `queryEvents` method takes in the `beginTime` and `endTime` to retrieve the usage events.
    // In our case, beginTime = currentTime - 10 minutes ( 1000 * 60 * 10 milliseconds )
    // and endTime = currentTime
    val usageEvents = usageStatsManager.queryEvents(intervalMilliseconds, currentTime)
    val usageEvent = UsageEvents.Event()
    val usageEventsList = mutableListOf<Triple<String, String, Long>>()
    val nonSystemAppMap = getAllAppsWithoutSystemApp(context)
    while (usageEvents.hasNextEvent()) {
        usageEvents.getNextEvent(usageEvent)
        if(nonSystemAppMap.contains(usageEvent.packageName)) {
            usageEventsList.add(Triple(
                usageEvent.packageName,
                nonSystemAppMap[usageEvent.packageName] ?: "",
                usageEvent.timeStamp,
            ) as Triple<String, String, Long>)
        }
    }
    return usageEventsList.toList()
}

// Method to get all events data grouped by package automatically by android UsageStatsManager API
private fun getEventsDataGroupedByPackages(context: Context, intervalMilliseconds: Long) :  List<Triple<String, String, Long>> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val currentTime = System.currentTimeMillis()
        val beginTime = currentTime - intervalMilliseconds
        val listOfAppsUsage = mutableListOf<Triple<String, String, Long>>()
        val nonSystemAppMap = getAllAppsWithoutSystemApp(context)
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStatsMap: Map<String, UsageStats> =
            usageStatsManager.queryAndAggregateUsageStats(beginTime, currentTime)
        usageStatsMap.forEach {
            if(it.value.totalTimeInForeground > 0) {
                if(nonSystemAppMap.contains(it.value.packageName)) {
                    listOfAppsUsage.add(
                        Triple(
                            it.value.packageName,
                            nonSystemAppMap[it.value.packageName] ?: "",
                            it.value.totalTimeInForeground,
                        ) as Triple<String, String, Long>
                    )
                }
            }
            listOfAppsUsage.sortByDescending { it.third }
        }

        /*Log.d("layonflog", "getEventsData2() listOfAppsUsage.size ${listOfAppsUsage.size}")
        listOfAppsUsage.forEach{
            Log.d("layonflog", "listOfAppsUsage: ${it.first} - ${it.second} - ${convertTimeDurationString(it.third)}")
        }*/
        return listOfAppsUsage
    } else {
        throw Exception("getEventsDataGroupedByPackages: Build < VERSION_CODES.Q")
    }
}



/*
* https://tomas-repcik.medium.com/listing-all-installed-apps-in-android-13-via-packagemanager-3b04771dc73
* There is a way how to filter system apps.
* A good rule of thumb is if the app does not contain the main activity,
* it is not a user-facing application and could be considered a system app.

* https://proandroiddev.com/how-to-get-users-installed-apps-in-android-11-b4a4d2754286
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
tools:ignore="QueryAllPackagesPermission" />
 */
private fun getAllAppsWithoutSystemApp(context: Context) : Map<String, String>{
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null)
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
    val mapOfApps = mutableMapOf<String, String>()
    val appInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
            mainIntent,
            PackageManager.ResolveInfoFlags.of(0L)
        )
    } else {
        pm.queryIntentActivities(mainIntent, 0)
    }
    Log.d("layonflog", "getAllAppsWithoutSystemApp appInfos.size : ${appInfos.size}")
    appInfos.forEach {
        //Log.d("layonflog", "mapOfApps: ${it.activityInfo.packageName}, ${it.activityInfo.loadLabel(context.packageManager).toString()}")
        mapOfApps.put(it.activityInfo.packageName, it.activityInfo.loadLabel(context.packageManager).toString())
    }
    return mapOfApps
}


@Composable
fun UsageEventsScreen(listOfAppsUsage: List<Triple<String, String, Long>>) {
    Column {
        Text(
            text = "Usage Events List by: ${convertTimeDurationString(intervalMilliseconds)}",
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        LazyColumn {
            itemsIndexed(listOfAppsUsage) { index, item ->
                usageItemView(item)
            }
        }
    }
}

private fun getIcon(context: Context, packageName: String) =
    context.packageManager.getApplicationIcon(packageName)


@Preview
@Composable
fun showUsageEventsScreen() {
    val fakeList = listOf(
        Triple("com.zhiliaoapp.musically", "tiktok", 1714050106842L),
        Triple("com.google.android.youtube", "youtube", 1714050106842L),
        Triple("com.kwai.video", "kwai", 1714050106842L),
    )
    UsageEventsScreen(fakeList)
}

@Composable
fun usageItemView(item: Triple<String, String, Long>) {
    OutlinedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, Color.Black),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ){
            Spacer(modifier = Modifier.width(10.dp))
            Image(
                painter = DrawablePainter(getIcon(LocalContext.current, item.first)),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(50.dp)
                    .width(50.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = item.second,
                    modifier = Modifier.padding(4.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.first,
                    modifier = Modifier.padding(4.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
                Text(
                    text = convertTimeDurationString(item.third),
                    modifier = Modifier.padding(4.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Preview
@Composable
fun usageItemViewPreview() {
    usageItemView(Triple<String, String, Long>("AppPackage", "AppName", 123123123L))
}

@Composable
fun ComposableLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: (LifecycleOwner, Lifecycle.Event) -> Unit
) {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            onEvent(source, event)
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}