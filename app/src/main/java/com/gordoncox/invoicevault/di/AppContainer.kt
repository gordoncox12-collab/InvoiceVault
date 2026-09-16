package com.gordoncox.invoicevault.di

import android.content.Context
import androidx.room.Room
import com.gordoncox.invoicevault.data.db.AppDatabase
import com.gordoncox.invoicevault.data.excel.ExcelService
import com.gordoncox.invoicevault.data.files.LocalStorage
import com.gordoncox.invoicevault.data.pdf.InvoicePdfGenerator
import com.gordoncox.invoicevault.data.repo.VaultRepository
import com.gordoncox.invoicevault.data.seed.SeedData
import com.gordoncox.invoicevault.data.share.ShareHelper
import kotlinx.coroutines.runBlocking

class AppContainer(context: Context) {
    val db: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "invoicevault.db",
    ).fallbackToDestructiveMigration().build()

    val storage = LocalStorage(context)
    val excel = ExcelService(context)
    val pdf = InvoicePdfGenerator(context, storage)
    val share = ShareHelper(context)
    val repo = VaultRepository(context, db, storage, excel, pdf)

    init {
        runBlocking {
            SeedData.insertIfEmpty(db, storage, excel, context.filesDir)
            repo.markOverdue()
        }
    }
}
