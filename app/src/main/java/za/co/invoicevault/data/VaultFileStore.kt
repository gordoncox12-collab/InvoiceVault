package za.co.invoicevault.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

enum class VaultFolder { INVOICES, RECEIPTS, EXCEL, IMAGES, NOTES }

class VaultFileStore(private val context: Context) {

    private fun root(): File = File(context.filesDir, "vault").apply { mkdirs() }

    fun businessDir(businessId: String): File =
        File(root(), businessId).apply { mkdirs() }

    fun customerDir(businessId: String, customerId: String): File =
        File(businessDir(businessId), customerId).apply { mkdirs() }

    fun folder(businessId: String, customerId: String, folder: VaultFolder): File =
        File(customerDir(businessId, customerId), folder.name.lowercase()).apply { mkdirs() }

    fun ensureCustomerTree(businessId: String, customerId: String) {
        VaultFolder.entries.forEach { folder(businessId, customerId, it) }
    }

    fun writeBytes(businessId: String, customerId: String, folder: VaultFolder, name: String, bytes: ByteArray): File {
        val target = File(folder(businessId, customerId, folder), sanitize(name))
        target.parentFile?.mkdirs()
        target.writeBytes(bytes)
        return target
    }

    fun copyFromUri(
        uri: Uri,
        businessId: String,
        customerId: String,
        folder: VaultFolder,
        name: String
    ): File {
        val target = File(folder(businessId, customerId, folder), sanitize(name))
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: error("Unable to read $uri")
        return target
    }

    fun copyFile(source: File, businessId: String, customerId: String, folder: VaultFolder, name: String): File {
        val target = File(folder(businessId, customerId, folder), sanitize(name))
        source.copyTo(target, overwrite = true)
        return target
    }

    fun sharedCache(name: String): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        return File(dir, sanitize(name))
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "file_${System.currentTimeMillis()}" }
}
