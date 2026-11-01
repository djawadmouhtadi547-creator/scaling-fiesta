package com.djawad1.daftarkredi.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {
    val customers: Flow<List<CustomerEntity>> = db.customerDao().observeAll()
    val entries: Flow<List<LedgerEntryEntity>> = db.ledgerDao().observeAll()
    val products: Flow<List<ProductEntity>> = db.productDao().observeAll()

    suspend fun addCustomer(customer: CustomerEntity) = db.customerDao().insert(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = db.customerDao().update(customer)
    suspend fun deleteCustomer(id: Long) = db.customerDao().delete(id)
    suspend fun addEntry(entry: LedgerEntryEntity) = db.ledgerDao().insert(entry)
    suspend fun updateEntry(entry: LedgerEntryEntity) = db.ledgerDao().update(entry)
    suspend fun deleteEntry(id: Long) = db.ledgerDao().delete(id)
    suspend fun findProduct(barcode: String) = db.productDao().findByBarcode(barcode)
    suspend fun addProduct(product: ProductEntity) = db.productDao().insert(product)
    suspend fun updateProduct(product: ProductEntity) = db.productDao().update(product)
    suspend fun deleteProduct(id: Long) = db.productDao().delete(id)

    suspend fun restorePreservingIds(
        customers: List<CustomerEntity>,
        entries: List<LedgerEntryEntity>,
        products: List<ProductEntity>
    ) {
        db.withTransaction {
            db.ledgerDao().deleteAll()
            db.customerDao().deleteAll()
            db.productDao().deleteAll()
            customers.forEach { db.customerDao().insert(it) }
            entries.forEach { db.ledgerDao().insert(it) }
            products.forEach { db.productDao().insert(it) }
        }
    }
}
