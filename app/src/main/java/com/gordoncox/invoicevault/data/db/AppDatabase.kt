package com.gordoncox.invoicevault.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gordoncox.invoicevault.data.entity.AppSettingsEntity
import com.gordoncox.invoicevault.data.entity.BusinessEntity
import com.gordoncox.invoicevault.data.entity.CustomerEntity
import com.gordoncox.invoicevault.data.entity.FolderFileEntity
import com.gordoncox.invoicevault.data.entity.InvoiceEntity
import com.gordoncox.invoicevault.data.entity.InvoiceImageEntity
import com.gordoncox.invoicevault.data.entity.InvoiceLineItemEntity
import com.gordoncox.invoicevault.data.entity.InvoiceTemplateEntity
import com.gordoncox.invoicevault.data.entity.NoteEntity
import com.gordoncox.invoicevault.data.entity.TransactionEntity

@Database(
    entities = [
        BusinessEntity::class,
        CustomerEntity::class,
        InvoiceEntity::class,
        InvoiceLineItemEntity::class,
        InvoiceImageEntity::class,
        TransactionEntity::class,
        NoteEntity::class,
        FolderFileEntity::class,
        InvoiceTemplateEntity::class,
        AppSettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun customerDao(): CustomerDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun noteDao(): NoteDao
    abstract fun folderFileDao(): FolderFileDao
    abstract fun templateDao(): TemplateDao
    abstract fun settingsDao(): SettingsDao
}
