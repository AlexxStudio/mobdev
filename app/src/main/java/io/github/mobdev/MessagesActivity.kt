package io.github.mobdev

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.annotations.SerializedName
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
import retrofit2.http.Path

class MessagesActivity : AppCompatActivity() {

    data class Message(
        val id: String?,
        val from: String?,
        val to: String?,
        val data: MessageData?,
        val time: Long?
    )

    data class MessageData(
        @SerializedName("Text")
        val textData: TextData?
    )

    data class TextData(
        val text: String?
    )

    data class SendMessage(
        val from: String,
        val to: String,
        val data: MessageData
    )

    interface ChatApi {
        @GET("channel/{name}")
        suspend fun getMessages(
            @Path("name") name: String,
            @retrofit2.http.Query("reverse") reverse: Boolean = true
        ): List<Message>

        @POST("messages")
        suspend fun sendMessage(
            @Header("X-Auth-Token") token: String,
            @Body message: SendMessage
        ): Response<ResponseBody>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val chatName = intent.getStringExtra("chat") ?: ""
        val listView = findViewById<ListView>(R.id.messagesList)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val sendButton = findViewById<Button>(R.id.sendButton)

        val token = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("token", "") ?: ""

        val username = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("login", "") ?: ""

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

        fun loadMessages() {
            lifecycleScope.launch {
                try {
                    val messages = api.getMessages(chatName)

                    val messageTexts = messages.map { message ->
                        val author = message.from ?: "unknown"
                        val text = message.data?.textData?.text ?: "[не текстовое сообщение]"
                        "$author: $text"
                    }

                    val adapter = ArrayAdapter(
                        this@MessagesActivity,
                        android.R.layout.simple_list_item_1,
                        messageTexts
                    )

                    listView.adapter = adapter

                } catch (e: Exception) {
                    Toast.makeText(
                        this@MessagesActivity,
                        "Ошибка загрузки: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        loadMessages()

        sendButton.setOnClickListener {
            val text = messageInput.text.toString()

            if (text.isBlank()) {
                Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val message = SendMessage(
                        from = username,
                        to = chatName,
                        data = MessageData(
                            textData = TextData(text)
                        )
                    )

                    val response = api.sendMessage(token, message)

                    if (response.isSuccessful) {
                        messageInput.text.clear()
                        loadMessages()
                    }
                    else {
                        val errorText = response.errorBody()?.string()

                        Toast.makeText(
                            this@MessagesActivity,
                            "Ошибка ${response.code()}: $errorText",
                            Toast.LENGTH_LONG
                        ).show()

                        android.util.Log.d("MY_LOG", "SEND ERROR: $errorText")
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@MessagesActivity,
                        "Ошибка сети: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}