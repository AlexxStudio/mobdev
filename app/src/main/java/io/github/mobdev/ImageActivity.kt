package io.github.mobdev

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.load

class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        // стрелка назад в тулбаре
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val imageView = findViewById<ImageView>(R.id.fullImage)

        val imageUrl = intent.getStringExtra("url")

        imageView.load(imageUrl) {
            crossfade(true)
        }
    }

    // обработка стрелки назад
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}