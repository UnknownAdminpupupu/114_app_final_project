package com.example.lifesync

// 修正：增加 category 欄位以解決 MainActivity 的 "Too many arguments" 錯誤
data class Task(
    var id: String = "",
    var title: String = "",
    var isCompleted: Boolean = false,
    var category: String = "其他" // 新增這個欄位
)

// 天氣相關資料模型
data class WeatherResponse(val main: MainData)
data class MainData(val temp: Float)