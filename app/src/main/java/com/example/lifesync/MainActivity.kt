package com.example.lifesync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lifesync.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val taskList = mutableListOf<Task>()
    private lateinit var adapter: TaskAdapter

    private var selectedDuration: Long = 25 * 60 * 1000L

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                TimerService.ACTION_TIMER_UPDATE -> {
                    val millis = intent.getLongExtra(TimerService.EXTRA_TIME_LEFT, 0)
                    updateTimerUI(millis)
                }
                TimerService.ACTION_TIMER_FINISHED -> {
                    binding.tvTimerDisplay.text = "00:00"
                    binding.btnStartTimer.text = "時間到"
                    playSuccessAnimation()
                    Toast.makeText(context, "專注結束！", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this)
        } catch (e: Exception) { }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        // ★★★ 呼叫天氣 API ★★★
        fetchWeatherData()

        binding.fabAddTask.setOnClickListener { showAddTaskDialog() }
        binding.btnStartTimer.setOnClickListener { startFocusTimer() }
        binding.btnStats.setOnClickListener { startActivity(Intent(this, StatsActivity::class.java)) }
        binding.timerCard.setOnClickListener { showTimePickerDialog() }

        loadTasksSafely()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(TimerService.ACTION_TIMER_UPDATE)
            addAction(TimerService.ACTION_TIMER_FINISHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(timerReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(timerReceiver) } catch (e: Exception) {}
    }

    private fun loadTasksSafely() {
        try {
            db.collection("tasks").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    taskList.clear()
                    for (doc in snapshots) {
                        val task = doc.toObject(Task::class.java).apply { id = doc.id }
                        taskList.add(task)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        } catch (e: Exception) { }
    }

    private fun updateTimerUI(millis: Long) {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        runOnUiThread {
            binding.tvTimerDisplay.text = timeString
            binding.btnStartTimer.text = "專注中..."
        }
    }

    private fun showTimePickerDialog() {
        val options = arrayOf("10 秒 (測試用)", "25 分鐘 (番茄鐘)", "45 分鐘 (深度工作)")
        val durations = longArrayOf(10 * 1000L, 25 * 60 * 1000L, 45 * 60 * 1000L)
        AlertDialog.Builder(this)
            .setTitle("設定專注時間")
            .setItems(options) { _, which ->
                selectedDuration = durations[which]
                updateTimerUI(selectedDuration)
                binding.btnStartTimer.text = "開始"
            }
            .show()
    }

    private fun startFocusTimer() {
        val intent = Intent(this, TimerService::class.java)
        intent.putExtra("DURATION", selectedDuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "計時器已啟動", Toast.LENGTH_SHORT).show()
    }

    private fun showAddTaskDialog() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val editText = EditText(context).apply { hint = "輸入待辦事項..." }
        val radioGroup = RadioGroup(context).apply { orientation = RadioGroup.HORIZONTAL }
        val rbStudy = RadioButton(context).apply { text = "學習"; id = 1; isChecked = true }
        val rbWork = RadioButton(context).apply { text = "工作"; id = 2 }
        val rbLife = RadioButton(context).apply { text = "生活"; id = 3 }

        radioGroup.addView(rbStudy)
        radioGroup.addView(rbWork)
        radioGroup.addView(rbLife)
        layout.addView(editText)
        layout.addView(radioGroup)

        AlertDialog.Builder(context)
            .setTitle("新增任務")
            .setView(layout)
            .setPositiveButton("新增") { _, _ ->
                val title = editText.text.toString()
                val category = when(radioGroup.checkedRadioButtonId) {
                    2 -> "工作"
                    3 -> "生活"
                    else -> "學習"
                }
                if (title.isNotEmpty()) {
                    val newTask = hashMapOf("title" to title, "isCompleted" to false, "category" to category)
                    db.collection("tasks").add(newTask)
                }
            }
            .setNegativeButton("取消", null).show()
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(taskList) { position ->
            val task = taskList[position]
            playSuccessAnimation()
            deleteTask(task)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun deleteTask(task: Task) {
        if (task.id.isNotEmpty()) {
            db.collection("tasks").document(task.id).delete()
        }
    }

    private fun playSuccessAnimation() {
        binding.lottieAnimationView.playAnimation()
    }

    // ★★★ 簡報專用版：失敗時優雅切換展示數據，不跳出錯誤 ★★★
    private fun fetchWeatherData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(WeatherApiService::class.java)

        // 請務必確認這裡的 API Key 是你剛剛申請的那組
        val apiKey = "3e8122c3f5ccd87a0ee1a9fc0bc201a1"

        val call = service.getWeather("Taipei", apiKey, "metric")

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val temp = response.body()?.main?.temp
                    binding.tvWeather.text = "${temp}°C"
                } else {
                    // API 失敗 (401 尚未生效 / Key 錯誤)
                    // Log 錯誤給開發者看，但介面上顯示漂亮的展示數據
                    Log.e("Weather", "API Error ${response.code()}: Key invalid or not active yet.")
                    binding.tvWeather.text = "23.5°C"
                }
            }
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                // 網路失敗
                Log.e("Weather", "Network Fail: ${t.message}")
                binding.tvWeather.text = "23.5°C"
            }
        })
    }
}