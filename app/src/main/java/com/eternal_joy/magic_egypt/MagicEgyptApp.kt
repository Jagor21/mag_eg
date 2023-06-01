package com.eternal_joy.magic_egypt

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.AppsFlyerProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val LOG_TAG = "MagicEgyptApp"

class MagicEgyptApp : Application() {

    private lateinit var sharedPreferences: SharedPreferences

    val conversionData = MutableStateFlow<Any?>("")

    override fun onCreate() {
        super.onCreate()
        createNotificationsChannels()
        createSilentNotificationChannel()

        sharedPreferences = getSharedPreferences("your_prefs", MODE_PRIVATE)
        val prefEditor = sharedPreferences.edit()
        val conversionDataListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                Log.d(LOG_TAG, data.toString())
                var campaign = data?.get("campaign").toString()
                if (campaign.isNotEmpty()) {
                    prefEditor.putString("conversion_data", campaign).commit()
                    conversionData.update { campaign }
                } else {
                    campaign = data?.get("adset").toString()
                    if(campaign.isNotEmpty()) {
                        prefEditor.putString("conversion_data", campaign).commit()
                        conversionData.update { campaign }
                    }
                }
            }

            override fun onConversionDataFail(error: String?) {
            }

            override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
            }

            override fun onAttributionFailure(error: String?) {
            }
        }
        AppsFlyerLib.getInstance().setDebugLog(true)
        AppsFlyerLib.getInstance()
            .init(getString(R.string.appsflyer_dev_key), conversionDataListener, this)
        AppsFlyerLib.getInstance().start(this)
        val id = AppsFlyerLib.getInstance().getAppsFlyerUID(this)
        id?.let { Log.d(LOG_TAG, "AppsFlyerUID: $it") }
        val cuid = AppsFlyerProperties.getInstance().getString(AppsFlyerProperties.APP_USER_ID)
        Log.d(LOG_TAG, "CUID: $cuid")
    }

    private fun createNotificationsChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.reminders_notification_channel_id),
                getString(R.string.reminders_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            ContextCompat.getSystemService(this, NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    private fun createSilentNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(MagicEgyptFCMService.SILENT_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    MagicEgyptFCMService.SILENT_CHANNEL_ID,
                    "WheelOfFortune silent events",
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.enableLights(false)
                channel.enableVibration(false)
                channel.setSound(null, null)
                manager.createNotificationChannel(channel)
            }
        }
    }
}