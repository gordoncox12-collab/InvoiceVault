package za.co.invoicevault.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun statusToString(v: InvoiceStatus) = v.name
    @TypeConverter fun stringToStatus(v: String) = InvoiceStatus.valueOf(v)
    @TypeConverter fun discountToString(v: DiscountType) = v.name
    @TypeConverter fun stringToDiscount(v: String) = DiscountType.valueOf(v)
    @TypeConverter fun themeToString(v: ThemeMode) = v.name
    @TypeConverter fun stringToTheme(v: String) = ThemeMode.valueOf(v)
    @TypeConverter fun accentToString(v: AccentPalette) = v.name
    @TypeConverter fun stringToAccent(v: String) = AccentPalette.valueOf(v)
    @TypeConverter fun layoutToString(v: TemplateLayout) = v.name
    @TypeConverter fun stringToLayout(v: String) = TemplateLayout.valueOf(v)
    @TypeConverter fun kindToString(v: FileKind) = v.name
    @TypeConverter fun stringToKind(v: String) = FileKind.valueOf(v)
}

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY name")
    fun observeAll(): Flow<List<Business>>

    @Query("SELECT * FROM businesses ORDER BY name")
    suspend fun all(): List<Business>

    @Query("SELECT * FROM businesses WHERE id = :id")
    suspend fun get(id: String): Business?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Business)

    @Delete
    suspend fun delete(item: Business)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE businessId = :businessId ORDER BY name")
    fun observeForBusiness(businessId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun get(id: String): Customer?

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND name = :name LIMIT 1")
    suspend fun findByName(businessId: String, name: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Customer)

    @Delete
    suspend fun delete(item: Customer)
}

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY issuedOn DESC")
    fun observeForBusiness(businessId: String): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY issuedOn DESC")
    fun observeForCustomer(customerId: String): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getWithDetails(id: String): InvoiceWithDetails?

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun get(id: String): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Invoice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLines(lines: List<InvoiceLine>)

    @Query("DELETE FROM invoice_lines WHERE invoiceId = :invoiceId")
    suspend fun clearLines(invoiceId: String)

    @Delete
    suspend fun delete(item: Invoice)

    @Query("UPDATE invoices SET status = 'OVERDUE' WHERE status = 'SENT' AND dueOn < :today")
    suspend fun markOverdue(today: Long)
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts WHERE customerId = :customerId ORDER BY receivedOn DESC")
    fun observeForCustomer(customerId: String): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE businessId = :businessId ORDER BY receivedOn DESC")
    fun observeForBusiness(businessId: String): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun get(id: String): Receipt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Receipt)

    @Delete
    suspend fun delete(item: Receipt)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE customerId = :customerId ORDER BY updatedAt DESC")
    fun observeForCustomer(customerId: String): Flow<List<CustomerNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CustomerNote)

    @Delete
    suspend fun delete(item: CustomerNote)
}

@Dao
interface FileDao {
    @Query("SELECT * FROM vault_files WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeForCustomer(customerId: String): Flow<List<VaultFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VaultFile)

    @Delete
    suspend fun delete(item: VaultFile)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates WHERE businessId = :businessId OR businessId = '' ORDER BY name")
    fun observeForBusiness(businessId: String): Flow<List<InvoiceTemplate>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun get(id: String): InvoiceTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InvoiceTemplate)

    @Delete
    suspend fun delete(item: InvoiceTemplate)
}

@Dao
interface PrefsDao {
    @Query("SELECT * FROM prefs WHERE id = 1")
    fun observe(): Flow<AppPrefs?>

    @Query("SELECT * FROM prefs WHERE id = 1")
    suspend fun get(): AppPrefs?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(prefs: AppPrefs)
}

@Database(
    entities = [
        Business::class,
        Customer::class,
        Invoice::class,
        InvoiceLine::class,
        Receipt::class,
        CustomerNote::class,
        VaultFile::class,
        InvoiceTemplate::class,
        AppPrefs::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InvoiceVaultDatabase : RoomDatabase() {
    abstract fun businesses(): BusinessDao
    abstract fun customers(): CustomerDao
    abstract fun invoices(): InvoiceDao
    abstract fun receipts(): ReceiptDao
    abstract fun notes(): NoteDao
    abstract fun files(): FileDao
    abstract fun templates(): TemplateDao
    abstract fun prefs(): PrefsDao
}
