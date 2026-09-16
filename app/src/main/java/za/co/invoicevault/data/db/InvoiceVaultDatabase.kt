package za.co.invoicevault.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BusinessEntity::class,
        CustomerEntity::class,
        InvoiceEntity::class,
        InvoiceLineEntity::class,
        TemplateEntity::class,
        FolderFileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InvoiceVaultDatabase : RoomDatabase() {
    abstract fun businesses(): BusinessDao
    abstract fun customers(): CustomerDao
    abstract fun invoices(): InvoiceDao
    abstract fun lines(): InvoiceLineDao
    abstract fun templates(): TemplateDao
    abstract fun files(): FolderFileDao

    companion object {
        fun create(context: Context): InvoiceVaultDatabase =
            Room.databaseBuilder(context, InvoiceVaultDatabase::class.java, "invoice_vault.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
