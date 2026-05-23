package io.github.mobdev

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    interface ChatApi {
        @POST("login")
        suspend fun login(@Body body: LoginRequest): Response<okhttp3.ResponseBody>
    }

    data class LoginRequest(
        val name: String,
        val pwd: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etLogin = findViewById<EditText>(R.id.etLogin)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

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

        btnLogin.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()

            lifecycleScope.launch {
                try {
                    val response = api.login(LoginRequest(login, password))

                    if (response.isSuccessful) {

                        val token = response.body()?.string() ?: ""

                        android.util.Log.d("MY_LOG", "TOKEN: $token")

                        getSharedPreferences("auth", MODE_PRIVATE)
                            .edit()
                            .putString("token", token)
                            .putString("login", login)
                            .apply()

                        Toast.makeText(
                            this@MainActivity,
                            "Вход выполнен",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = android.content.Intent(this@MainActivity, ChatsActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else if (response.code() == 401) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.login_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка сети",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}