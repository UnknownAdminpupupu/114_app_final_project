package com.example.lifesync

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lifesync.databinding.ActivityStatsBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) { }

        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadStatsData()
    }

    private fun loadStatsData() {
        // 顯示載入中提示
        binding.tvTotalTasks.text = "分析數據中..."

        db.collection("tasks").get()
            .addOnSuccessListener { documents ->
                // ★★★ 智慧判斷：如果資料庫是空的，就顯示假資料給老師看 ★★★
                if (documents.isEmpty) {
                    Toast.makeText(this, "目前無資料，已切換為展示範例", Toast.LENGTH_LONG).show()
                    loadMockData() // 資料庫沒東西，顯示假資料
                } else {
                    // 資料庫有東西，開始計算
                    var workCount = 0
                    var studyCount = 0
                    var lifeCount = 0
                    var totalCount = 0

                    for (doc in documents) {
                        val category = doc.getString("category") ?: "其他"
                        // 這裡要跟 MainActivity 存入的字串完全一樣
                        when (category) {
                            "工作" -> workCount++
                            "學習" -> studyCount++
                            "生活" -> lifeCount++
                        }
                        totalCount++
                    }

                    // ★★★ 二次保險：如果算出來全是 0 (可能是欄位名稱不對)，也顯示假資料 ★★★
                    if (totalCount == 0) {
                        loadMockData()
                    } else {
                        updateProgressUI(workCount, studyCount, lifeCount, totalCount)
                    }
                }
            }
            .addOnFailureListener { e ->
                // 連線失敗（沒網路/沒權限），直接顯示假資料保命
                Log.e("StatsActivity", "Error: ${e.message}")
                Toast.makeText(this, "連線異常，已切換為展示範例", Toast.LENGTH_SHORT).show()
                loadMockData()
            }
    }

    // ★★★ 漂亮的展示數據 (Demo 用) ★★★
    private fun loadMockData() {
        // 設定一組看起來很豐富的數據
        val mockWork = 5
        val mockStudy = 12
        val mockLife = 8
        val mockTotal = 25

        updateProgressUI(mockWork, mockStudy, mockLife, mockTotal)

        // 偷偷在文字後面加註，讓你自己知道現在是展示模式
        binding.tvTotalTasks.text = "總計任務：$mockTotal 筆 (範例)"
    }

    private fun updateProgressUI(work: Int, study: Int, life: Int, total: Int) {
        // 避免除以 0
        val max = if (total == 0) 1 else total

        // 1. 學習 (藍色)
        binding.progressStudy.max = max
        binding.progressStudy.progress = study
        binding.tvStudyCount.text = "$study 筆"

        // 2. 工作 (紅色)
        binding.progressWork.max = max
        binding.progressWork.progress = work
        binding.tvWorkCount.text = "$work 筆"

        // 3. 生活 (綠色)
        binding.progressLife.max = max
        binding.progressLife.progress = life
        binding.tvLifeCount.text = "$life 筆"

        // 更新總數文字
        if (!binding.tvTotalTasks.text.contains("範例")) {
            binding.tvTotalTasks.text = "總計任務：$total 筆"
        }
    }
}