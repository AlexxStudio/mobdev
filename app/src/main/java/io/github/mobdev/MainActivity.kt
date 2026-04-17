package io.github.mobdev

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var requestButton: Button
    private lateinit var contactsList: ListView

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                requestButton.visibility = View.GONE
                showContacts()
            } else {
                statusText.text = "Нет доступа к контактам"
                requestButton.visibility = View.VISIBLE
                contactsList.visibility = View.GONE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        requestButton = findViewById(R.id.requestPermissionButton)
        contactsList = findViewById(R.id.contactsList)

        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            requestButton.visibility = View.GONE
            showContacts()
        } else {
            statusText.text = "Нет доступа к контактам"
            requestButton.visibility = View.VISIBLE
            contactsList.visibility = View.GONE
        }

        requestButton.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    @SuppressLint("Range")
    private fun fetchContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        ) ?: return emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val name = it.getString(
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                ) ?: "Без имени"

                val phone = it.getString(
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                ) ?: ""

                contacts.add(
                    Contact(
                        name = name,
                        phoneNumber = phone,
                        email = null
                    )
                )
            }
        }

        return contacts
    }

    private fun showContacts() {
        val contacts = fetchContacts()

        statusText.text = "Найдено контактов: ${contacts.size}"
        contactsList.visibility = View.VISIBLE

        val contactNames = contacts.map { "${it.name} — ${it.phoneNumber}" }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            contactNames
        )

        contactsList.adapter = adapter

        contactsList.setOnItemClickListener { _, _, position, _ ->
            val contact = contacts[position]

            val intent = Intent(this, ContactDetailsActivity::class.java)
            intent.putExtra("name", contact.name)
            intent.putExtra("phone", contact.phoneNumber)
            intent.putExtra("email", contact.email)

            startActivity(intent)
        }
    }
}