package za.co.invoicevault.data.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ShareHelper(private val context: Context) {
    private val authority: String = "${context.packageName}.fileprovider"

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, authority, file)

    fun emailIntent(file: File, subject: String, body: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uriFor(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun whatsappIntent(file: File, text: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, uriFor(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun whatsappChooser(file: File, text: String): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, uriFor(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share invoice PDF")
    }
}
