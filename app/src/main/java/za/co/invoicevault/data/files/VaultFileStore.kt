package za.co.invoicevault.data.files

import android.content.Context
import android.net.Uri
import za.co.invoicevault.domain.FolderCategory
import java.io.File
import java.io.InputStream

class VaultFileStore(private val context: Context) {

    fun vaultRoot(): File = File(context.filesDir, "vault").apply { mkdirs() }

    fun customerDir(businessId: Long, customerId: Long): File =
        File(vaultRoot(), "business-$businessId/customer-$customerId").apply { mkdirs() }

    fun categoryDir(businessId: Long, customerId: Long, category: FolderCategory): File =
        File(customerDir(businessId, customerId), category.name.lowercase()).apply { mkdirs() }

    fun ensureCustomerTree(businessId: Long, customerId: Long) {
        FolderCategory.entries.forEach { categoryDir(businessId, customerId, it) }
    }

    fun sharedCache(): File = File(context.cacheDir, "shared").apply { mkdirs() }

    fun copyFromUri(uri: Uri, destination: File): File {
        destination.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open $uri" }
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return destination
    }

    fun writeBytes(destination: File, bytes: ByteArray): File {
        destination.parentFile?.mkdirs()
        destination.writeBytes(bytes)
        return destination
    }

    fun writeStream(destination: File, input: InputStream): File {
        destination.parentFile?.mkdirs()
        destination.outputStream().use { output -> input.copyTo(output) }
        return destination
    }

    fun uniqueFile(dir: File, name: String): File {
        val safe = name.replace(Regex("[^A-Za-z0-9._-]+"), "_")
        var candidate = File(dir, safe.ifBlank { "file" })
        var n = 1
        while (candidate.exists()) {
            val dot = safe.lastIndexOf('.')
            val base = if (dot > 0) safe.substring(0, dot) else safe
            val ext = if (dot > 0) safe.substring(dot) else ""
            candidate = File(dir, "${base}_$n$ext")
            n++
        }
        return candidate
    }
}
