package com.example.lifesync

data class Task(
    var id: String = "",

    // 任務基本資料
    var title: String = "",
    var category: String = "其他",
    var isCompleted: Boolean = false,

    // 時間相關（重點）
    var deadlineAt: Long = 0L,   // DDL（0 = 未設定）
    var createdAt: Long = 0L     // 建立時間
)

// ===== 天氣相關資料模型 =====
data class WeatherResponse(val main: MainData)
data class MainData(val temp: Float)
