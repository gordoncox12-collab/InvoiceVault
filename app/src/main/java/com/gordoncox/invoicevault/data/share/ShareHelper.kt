package com.gordoncox.invoicevault.data.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class ShareHelper(private val context: Context) {
    private fun uriFor(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    fun sharePdf(file: File, title: String, body: String, email: String? = null) {
        val uri = uriFor(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
            if (!email.isNullOrBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $title").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun emailPdf(file: File, title: String, body: String, email: String?) {
        val uri = uriFor(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
            if (!email.isNullOrBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Email $title").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun whatsappPdf(file: File, body: String) {
        val uri = uriFor(file)
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in packages) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                setPackage(pkg)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        }
        sharePdf(file, "Invoice", body, null)
    }
}
