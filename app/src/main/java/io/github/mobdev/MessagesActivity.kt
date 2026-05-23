package io.github.mobdev

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
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
        val textData: TextData?,

        @SerializedName("Image")
        val imageData: ImageData?
    )

    data class TextData(
        val text: String?
    )

    data class ImageData(
        val link: String?
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
            @retrofit2.http.Query("limit") limit: Int = 20,
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

        fun logoutToLogin() {
            getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            val intent = Intent(this@MessagesActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        fun loadMessages() {
            lifecycleScope.launch {
                try {
                    val messages = api.getMessages(chatName)
                    listView.adapter = MessagesAdapter(messages)
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
                            textData = TextData(text),
                            imageData = null
                        )
                    )

                    val response = api.sendMessage(token, message)

                    if (response.isSuccessful) {
                        messageInput.text.clear()
                        loadMessages()
                    } else if (response.code() == 401) {
                        logoutToLogin()
                    } else {
                        Toast.makeText(
                            this@MessagesActivity,
                            "Ошибка ${response.code()}: ${response.errorBody()?.string()}",
                            Toast.LENGTH_LONG
                        ).show()
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

    inner class MessagesAdapter(
        private val messages: List<Message>
    ) : BaseAdapter() {

        override fun getCount(): Int = messages.size

        override fun getItem(position: Int): Message = messages[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val message = messages[position]

            val layout = LinearLayout(this@MessagesActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 16, 24, 16)
            }

            val authorView = TextView(this@MessagesActivity).apply {
                text = message.from ?: "unknown"
                textSize = 14f
            }

            layout.addView(authorView)

            val text = message.data?.textData?.text
            val imageLink = message.data?.imageData?.link

            if (!text.isNullOrBlank()) {
                val textView = TextView(this@MessagesActivity).apply {
                    this.text = text
                    textSize = 18f
                }

                layout.addView(textView)

            } else if (!imageLink.isNullOrBlank()) {
                val encodedLink = Uri.encode(imageLink)
                val thumbUrl = "https://faerytea.name/thumb/$encodedLink"
                val imageUrl = "https://faerytea.name/img/$encodedLink"

                val imageView = ImageView(this@MessagesActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        500
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP

                    load(thumbUrl)

                    setOnClickListener {
                        val intent = Intent(this@MessagesActivity, ImageActivity::class.java)
                        intent.putExtra("url", imageUrl)
                        startActivity(intent)
                    }
                }

                layout.addView(imageView)

            } else {
                val textView = TextView(this@MessagesActivity).apply {
                    this.text = "[пустое сообщение]"
                }

                layout.addView(textView)
            }

            return layout
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}