package com.mart.companion

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 96, 48, 48)

        val title = TextView(this)
        title.text = "Mart Companion"
        title.textSize = 22f
        layout.addView(title)

        val desc = TextView(this)
        desc.text = "\nAktifkan Accessibility Service supaya Mart bisa membaca layar dan mengontrol aplikasi.\n"
        layout.addView(desc)

        val btn = Button(this)
        btn.text = "Buka Pengaturan Accessibility"
        btn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        layout.addView(btn)

        val status = TextView(this)
        status.text = "\nServer lokal berjalan di port 8765 setelah Accessibility Service diaktifkan."
        layout.addView(status)

        setContentView(layout)
    }
}
