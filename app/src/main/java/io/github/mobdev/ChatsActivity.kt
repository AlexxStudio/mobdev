package io.github.mobdev

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

class ChatsActivity : AppCompatActivity() {

    private var selectedChat: String? = null
    private val gson = Gson()

    data class MessageData(
        @SerializedName("Text")
        val textData: TextData?,

        @SerializedName("Image")
        val imageData: ImageData?
    )

    data class TextData(val text: String?)

    data class ImageData(val link: String?)

    data class SendMessage(
        val from: String,
        val to: String,
        val data: MessageData
    )

    interface ChatApi {
        @GET("channels")
        suspend fun getChannels(): List<String>

        @POST("messages")
        suspend fun sendMessage(
            @Header("X-Auth-Token") token: String,
            @Body message: SendMessage
        ): Response<ResponseBody>
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun saveChannelsToCache(channels: List<String>) {
        val sortedChannels = channels
            .distinct()
            .sortedBy { it.replace("@channel", "").lowercase() }

        getSharedPreferences("cache", MODE_PRIVATE)
            .edit()
            .putString("channels_cache", gson.toJson(sortedChannels))
            .apply()
    }

    private fun loadChannelsFromCache(): List<String> {
        val json = getSharedPreferences("cache", MODE_PRIVATE)
            .getString("channels_cache", null)
            ?: return emptyList()

        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun showChannels(
        listView: ListView,
        channels: List<String>
    ) {
        val adapter = object : ArrayAdapter<String>(
            this@ChatsActivity,
            android.R.layout.simple_list_item_activated_1,
            channels
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getView(position, convertView, parent)
                listView.setItemChecked(position, channels[position] == selectedChat)
                return view
            }
        }

        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val chatName = channels[position]
            selectedChat = chatName
            adapter.notifyDataSetChanged()

            val intent = Intent(this@ChatsActivity, MessagesActivity::class.java)
            intent.putExtra("chat", chatName)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val listView = findViewById<ListView>(R.id.chatsList)
        val newChatInput = findViewById<EditText>(R.id.newChatInput)
        val createChatButton = findViewById<Button>(R.id.createChatButton)

        listView.choiceMode = ListView.CHOICE_MODE_SINGLE

        val token = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("token", "") ?: ""

        val username = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("login", "") ?: ""

        btnLogout.setOnClickListener {
            getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

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

        fun logoutToLogin() {
            getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            val intent = Intent(this@ChatsActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        fun loadChannels() {
            val cachedChannels = loadChannelsFromCache()

            if (cachedChannels.isNotEmpty()) {
                showChannels(listView, cachedChannels)
            }

            if (!isOnline()) {
                Toast.makeText(
                    this@ChatsActivity,
                    "Нет сети, показан сохранённый список чатов",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            lifecycleScope.launch {
                try {
                    val channels = api.getChannels()
                    saveChannelsToCache(channels)

                    val updatedChannels = loadChannelsFromCache()
                    showChannels(listView, updatedChannels)

                } catch (e: Exception) {
                    Toast.makeText(
                        this@ChatsActivity,
                        "Ошибка загрузки, показан кэш",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        createChatButton.setOnClickListener {
            val input = newChatInput.text.toString().trim()

            if (input.isBlank()) {
                Toast.makeText(this, "Введите название канала", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isOnline()) {
                Toast.makeText(
                    this,
                    "Нет сети. Создание канала недоступно",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val channelName = if (input.endsWith("@channel")) {
                input
            } else {
                "$input@channel"
            }

            lifecycleScope.launch {
                try {
                    val message = SendMessage(
                        from = username,
                        to = channelName,
                        data = MessageData(
                            textData = TextData("Канал создан"),
                            imageData = null
                        )
                    )

                    val response = api.sendMessage(token, message)

                    if (response.isSuccessful) {
                        newChatInput.text.clear()
                        loadChannels()
                        Toast.makeText(
                            this@ChatsActivity,
                            "Канал создан",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (response.code() == 401) {
                        logoutToLogin()
                    } else {
                        Toast.makeText(
                            this@ChatsActivity,
                            "Ошибка создания: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@ChatsActivity,
                        "Ошибка: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        loadChannels()
    }
}