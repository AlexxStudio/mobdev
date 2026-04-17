package io.github.mobdev

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ContactDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_details)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val nameText = findViewById<TextView>(R.id.nameText)
        val phoneText = findViewById<TextView>(R.id.phoneText)
        val emailText = findViewById<TextView>(R.id.emailText)

        val name = intent.getStringExtra("name") ?: "Не указано"
        val phone = intent.getStringExtra("phone") ?: "Не указано"
        val email = intent.getStringExtra("email") ?: "Не указано"

        nameText.text = getString(R.string.name_label, name)
        phoneText.text = getString(R.string.phone_label, phone)
        emailText.text = getString(R.string.email_label, email)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}