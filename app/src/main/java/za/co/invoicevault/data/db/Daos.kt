package za.co.invoicevault.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import za.co.invoicevault.domain.FolderCategory
import za.co.invoicevault.domain.InvoiceStatus

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY name")
    fun observeAll(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses ORDER BY name")
    suspend fun all(): List<BusinessEntity>

    @Query("SELECT * FROM businesses WHERE id = :id")
    suspend fun byId(id: Long): BusinessEntity?

    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: BusinessEntity): Long

    @Update
    suspend fun update(entity: BusinessEntity)

    @Delete
    suspend fun delete(entity: BusinessEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE businessId = :businessId ORDER BY name")
    fun observeForBusiness(businessId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId ORDER BY name")
    suspend fun forBusiness(businessId: Long): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun byId(id: Long): CustomerEntity?

    @Insert
    suspend fun insert(entity: CustomerEntity): Long

    @Update
    suspend fun update(entity: CustomerEntity)

    @Delete
    suspend fun delete(entity: CustomerEntity)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY issueDate DESC")
    fun observeForBusiness(businessId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY issueDate DESC")
    fun observeForCustomer(customerId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY issueDate DESC")
    suspend fun all(): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE businessId = :businessId")
    suspend fun forBusiness(businessId: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun byId(id: Long): InvoiceEntity?

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun withLines(id: Long): InvoiceWithLines?

    @Query("SELECT number FROM invoices WHERE businessId = :businessId")
    suspend fun numbersForBusiness(businessId: Long): List<String>

    @Insert
    suspend fun insert(entity: InvoiceEntity): Long

    @Update
    suspend fun update(entity: InvoiceEntity)

    @Query("UPDATE invoices SET status = :status, paidAt = :paidAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: InvoiceStatus, paidAt: Long?, updatedAt: Long)

    @Delete
    suspend fun delete(entity: InvoiceEntity)
}

@Dao
interface InvoiceLineDao {
    @Query("SELECT * FROM invoice_lines WHERE invoiceId = :invoiceId ORDER BY position")
    suspend fun forInvoice(invoiceId: Long): List<InvoiceLineEntity>

    @Insert
    suspend fun insertAll(lines: List<InvoiceLineEntity>)

    @Query("DELETE FROM invoice_lines WHERE invoiceId = :invoiceId")
    suspend fun deleteForInvoice(invoiceId: Long)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates ORDER BY name")
    suspend fun all(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun byId(id: Long): TemplateEntity?

    @Insert
    suspend fun insert(entity: TemplateEntity): Long

    @Update
    suspend fun update(entity: TemplateEntity)

    @Delete
    suspend fun delete(entity: TemplateEntity)
}

@Dao
interface FolderFileDao {
    @Query("SELECT * FROM folder_files WHERE customerId = :customerId AND category = :category ORDER BY createdAt DESC")
    fun observe(customerId: Long, category: FolderCategory): Flow<List<FolderFileEntity>>

    @Query("SELECT * FROM folder_files WHERE customerId = :customerId ORDER BY createdAt DESC")
    suspend fun forCustomer(customerId: Long): List<FolderFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FolderFileEntity): Long

    @Delete
    suspend fun delete(entity: FolderFileEntity)
}
