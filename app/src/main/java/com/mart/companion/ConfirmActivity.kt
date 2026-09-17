package com.mart.companion

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ConfirmActivity : Activity() {

    companion object {
        private var pendingLatch: CountDownLatch? = null
        private var pendingResult = AtomicBoolean(false)

        // Dipanggil dari server HTTP (thread background), blocking sampai user respons atau timeout
        fun requestConfirmation(context: android.content.Context, description: String, timeoutSeconds: Long = 15): Boolean {
            val latch = CountDownLatch(1)
            pendingLatch = latch
            pendingResult.set(false)

            val intent = android.content.Intent(context, ConfirmActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("description", description)
            context.startActivity(intent)

            val completed = latch.await(timeoutSeconds, TimeUnit.SECONDS)
            return completed && pendingResult.get()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val description = intent.getStringExtra("description") ?: "Aksi tidak diketahui"

        AlertDialog.Builder(this)
            .setTitle("Mart minta izin")
            .setMessage("Mart ingin melakukan:\n\n$description\n\nIzinkan?")
            .setCancelable(false)
            .setPositiveButton("Izinkan") { _, _ ->
                pendingResult.set(true)
                pendingLatch?.countDown()
                finish()
            }
            .setNegativeButton("Tolak") { _, _ ->
                pendingResult.set(false)
                pendingLatch?.countDown()
                finish()
            }
            .show()
    }
}
