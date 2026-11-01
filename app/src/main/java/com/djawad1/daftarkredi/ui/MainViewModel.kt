package com.djawad1.daftarkredi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.djawad1.daftarkredi.data.AppDatabase
import com.djawad1.daftarkredi.data.CustomerEntity
import com.djawad1.daftarkredi.data.LedgerEntryEntity
import com.djawad1.daftarkredi.data.ProductEntity
import com.djawad1.daftarkredi.data.Repository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(AppDatabase.get(application))

    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val entries = repository.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val products = repository.products.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCustomer(name: String, phone: String, note: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.addCustomer(CustomerEntity(name = name.trim(), phone = phone.trim(), note = note.trim()))
    }

    fun updateCustomer(customer: CustomerEntity) = viewModelScope.launch { repository.updateCustomer(customer) }
    fun deleteCustomer(id: Long) = viewModelScope.launch { repository.deleteCustomer(id) }

    fun addEntry(customerId: Long, kind: Int, amount: Double, note: String, timestamp: Long = System.currentTimeMillis()) = viewModelScope.launch {
        if (amount > 0.0 && amount.isFinite()) {
            repository.addEntry(LedgerEntryEntity(customerId = customerId, kind = kind, amount = amount, note = note.trim(), timestamp = timestamp))
        }
    }

    fun updateEntry(entry: LedgerEntryEntity) = viewModelScope.launch {
        if (entry.amount > 0.0 && entry.amount.isFinite()) repository.updateEntry(entry)
    }

    fun deleteEntry(id: Long) = viewModelScope.launch { repository.deleteEntry(id) }

    fun findProduct(barcode: String, onResult: (ProductEntity?) -> Unit) = viewModelScope.launch {
        onResult(repository.findProduct(barcode))
    }

    fun addProduct(name: String, barcode: String, price: Double, imagePath: String, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        if (name.isNotBlank() && price >= 0.0 && price.isFinite()) {
            val id = repository.addProduct(ProductEntity(name = name.trim(), barcode = barcode.trim(), price = price, imagePath = imagePath))
            onSaved(id)
        }
    }

    fun updateProduct(product: ProductEntity) = viewModelScope.launch { repository.updateProduct(product) }
    fun deleteProduct(id: Long) = viewModelScope.launch { repository.deleteProduct(id) }

    fun restore(
        importedCustomers: List<CustomerEntity>,
        importedEntries: List<LedgerEntryEntity>,
        importedProducts: List<ProductEntity>,
        onDone: (Boolean, String) -> Unit
    ) = viewModelScope.launch {
        try {
            repository.restorePreservingIds(importedCustomers, importedEntries, importedProducts)
            onDone(true, "تمت استعادة النسخة الاحتياطية بنجاح")
        } catch (error: Exception) {
            onDone(false, error.message ?: "تعذّرت استعادة النسخة الاحتياطية")
        }
    }
}
