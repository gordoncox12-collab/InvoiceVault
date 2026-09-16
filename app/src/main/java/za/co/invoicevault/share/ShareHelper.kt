package za.co.invoicevault.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ShareHelper(private val context: Context) {

    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun shareFile(file: File, mime: String, title: String, text: String, targetPackage: String? = null) {
        val uri = uriFor(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage != null) setPackage(targetPackage)
        }
        val chooser = Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareViaWhatsApp(file: File, text: String) {
        try {
            shareFile(file, "application/pdf", "InvoiceVault", text, "com.whatsapp")
        } catch (_: Exception) {
            shareFile(file, "application/pdf", "InvoiceVault", text, null)
        }
    }

    fun shareViaEmail(file: File, subject: String, body: String, to: String?) {
        val uri = uriFor(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            if (!to.isNullOrBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            shareFile(file, "application/pdf", subject, body, null)
        }
    }
}
