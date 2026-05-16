package io.github.mobdev

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import android.widget.Button

class ChatsActivity : AppCompatActivity() {

    interface ChatApi {
        @GET("channels")
        suspend fun getChannels(): List<String>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnLogout.setOnClickListener {

            getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)

            finish()
        }

        val listView = findViewById<ListView>(R.id.chatsList)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val api = Retrofit.Builder()
            .baseUrl("https://faerytea.name/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApi::class.java)

        lifecycleScope.launch {
            try {
                val channels = api.getChannels()

                val adapter = ArrayAdapter(
                    this@ChatsActivity,
                    android.R.layout.simple_list_item_1,
                    channels
                )

                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val chatName = channels[position]

                    val intent = android.content.Intent(
                        this@ChatsActivity,
                        MessagesActivity::class.java
                    )
                    intent.putExtra("chat", chatName)
                    startActivity(intent)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@ChatsActivity,
                    "Не удалось загрузить чаты",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}