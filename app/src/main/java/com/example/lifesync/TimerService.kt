package com.example.lifesync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    private var timer: CountDownTimer? = null
    private val CHANNEL_ID = "TimerServiceChannel"
    private val NOTIFICATION_ID = 1

    companion object {
        const val ACTION_TIMER_UPDATE = "com.example.lifesync.TIMER_UPDATE"
        const val ACTION_TIMER_FINISHED = "com.example.lifesync.TIMER_FINISHED"
        const val EXTRA_TIME_LEFT = "TIME_LEFT"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val duration = intent?.getLongExtra("DURATION", 25 * 60 * 1000L) ?: (25 * 60 * 1000L)

        Log.d("TimerService", "Service 啟動，準備倒數: $duration")

        // 1. 建立前景通知
        val notification = createNotification("專注計時中...")

        // ★★★ Android 14 修正：啟動前景服務時必須指定類型 ★★★
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                // 嘗試使用 Special Use 類型，如果失敗則退回普通模式
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 2. 先取消舊的計時器
        timer?.cancel()

        // 3. 啟動新的計時器
        timer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // 發送廣播
                val broadcastIntent = Intent(ACTION_TIMER_UPDATE)
                broadcastIntent.putExtra(EXTRA_TIME_LEFT, millisUntilFinished)

                // ★★★ 關鍵：指定接收 Package，確保 MainActivity 收得到 ★★★
                broadcastIntent.setPackage(packageName)

                sendBroadcast(broadcastIntent)

                Log.d("TimerService", "Tick: $millisUntilFinished")
            }

            override fun onFinish() {
                val broadcastIntent = Intent(ACTION_TIMER_FINISHED)
                broadcastIntent.setPackage(packageName)
                sendBroadcast(broadcastIntent)

                Log.d("TimerService", "倒數結束")
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "專注計時服務",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LifeSync")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        timer?.cancel()
        Log.d("TimerService", "Service 已銷毀")
        super.onDestroy()
    }
}