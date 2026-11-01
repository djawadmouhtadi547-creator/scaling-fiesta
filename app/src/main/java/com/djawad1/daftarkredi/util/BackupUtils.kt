package com.djawad1.daftarkredi.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.djawad1.daftarkredi.data.CustomerEntity
import com.djawad1.daftarkredi.data.LedgerEntryEntity
import com.djawad1.daftarkredi.data.ProductEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object BackupUtils {
    fun createJson(customers: List<CustomerEntity>, entries: List<LedgerEntryEntity>, products: List<ProductEntity>): String {
        val root = JSONObject().put("format", "daftar-al-kredi-backup").put("version", 1)
        root.put("customers", JSONArray().apply {
            customers.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone).put("note", it.note).put("createdAt", it.createdAt)) }
        })
        root.put("entries", JSONArray().apply {
            entries.forEach { put(JSONObject().put("id", it.id).put("customerId", it.customerId).put("kind", it.kind).put("amount", it.amount).put("timestamp", it.timestamp).put("note", it.note)) }
        })
        root.put("products", JSONArray().apply {
            products.forEach { product ->
                val imageBase64 = runCatching {
                    val file = File(product.imagePath)
                    if (product.imagePath.isNotBlank() && file.isFile) Base64.encodeToString(file.readBytes(), Base64.NO_WRAP) else ""
                }.getOrDefault("")
                put(JSONObject().put("id", product.id).put("name", product.name).put("barcode", product.barcode)
                    .put("price", product.price).put("imagePath", product.imagePath).put("imageBase64", imageBase64)
                    .put("createdAt", product.createdAt))
            }
        })
        return root.toString(2)
    }

    data class BackupData(
        val customers: List<CustomerEntity>,
        val entries: List<LedgerEntryEntity>,
        val products: List<ProductEntity>
    )

    fun parseJson(content: String, context: Context): BackupData {
        val root = JSONObject(content)
        require(root.optString("format") == "daftar-al-kredi-backup") { "هذا الملف ليس نسخة احتياطية لدفتر الكريدي" }
        val customers = buildList {
            val array = root.optJSONArray("customers") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(CustomerEntity(item.optLong("id"), item.optString("name"), item.optString("phone"), item.optString("note"), item.optLong("createdAt")))
            }
        }
        val entries = buildList {
            val array = root.optJSONArray("entries") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(LedgerEntryEntity(item.optLong("id"), item.optLong("customerId"), item.optInt("kind"), item.optDouble("amount"), item.optLong("timestamp"), item.optString("note")))
            }
        }
        val products = buildList {
            val array = root.optJSONArray("products") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val encodedImage = item.optString("imageBase64")
                val imagePath = if (encodedImage.isNotBlank()) {
                    val directory = File(context.filesDir, "product_images").apply { mkdirs() }
                    val imageFile = File(directory, "restored_${item.optLong("id")}_${System.currentTimeMillis()}.jpg")
                    imageFile.writeBytes(Base64.decode(encodedImage, Base64.DEFAULT))
                    imageFile.absolutePath
                } else item.optString("imagePath")
                add(ProductEntity(item.optLong("id"), item.optString("name"), item.optString("barcode"), item.optDouble("price"), imagePath, item.optLong("createdAt")))
            }
        }
        return BackupData(customers, entries, products)
    }

    fun copyImageToApp(context: Context, uri: Uri): String {
        val directory = File(context.filesDir, "product_images").apply { mkdirs() }
        val file = File(directory, "product_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "تعذّر قراءة الصورة" }
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return file.absolutePath
    }
}
