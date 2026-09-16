package za.co.invoicevault

import android.app.Application
import android.content.Context
import za.co.invoicevault.data.VaultFileStore
import za.co.invoicevault.data.VaultRepository
import za.co.invoicevault.excel.ExcelService
import za.co.invoicevault.pdf.InvoicePdfRenderer
import za.co.invoicevault.share.ShareHelper

class InvoiceVaultApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val files = VaultFileStore(app)
    val repo = VaultRepository(app, files)
    val excel = ExcelService(app)
    val pdf = InvoicePdfRenderer()
    val share = ShareHelper(app)
}
