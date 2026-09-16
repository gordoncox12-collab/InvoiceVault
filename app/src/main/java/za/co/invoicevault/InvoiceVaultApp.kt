package za.co.invoicevault

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import za.co.invoicevault.data.db.InvoiceVaultDatabase
import za.co.invoicevault.data.excel.SpreadsheetService
import za.co.invoicevault.data.files.VaultFileStore
import za.co.invoicevault.data.pdf.InvoicePdfWriter
import za.co.invoicevault.data.prefs.SettingsStore
import za.co.invoicevault.data.repo.VaultRepository
import za.co.invoicevault.data.share.ShareHelper

class AppContainer(app: Application) {
    val database = InvoiceVaultDatabase.create(app)
    val files = VaultFileStore(app)
    val settings = SettingsStore(app)
    val repository = VaultRepository(
        db = database,
        files = files,
        pdf = InvoicePdfWriter(),
        sheets = SpreadsheetService(),
        share = ShareHelper(app),
        settings = settings
    )
}

class InvoiceVaultApp : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.repository.bootstrap() }
    }
}
