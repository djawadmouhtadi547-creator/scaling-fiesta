package com.djawad1.daftarkredi.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.djawad1.daftarkredi.data.CustomerEntity
import com.djawad1.daftarkredi.data.LedgerEntryEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.Layout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.StaticLayout

fun shareCustomerStatementText(context: Context, customer: CustomerEntity, entries: List<LedgerEntryEntity>, currency: String) {
    val balance = entries.sumOf { if (it.kind == 0) it.amount else -it.amount }
    val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar", "DZ")).format(Date())
    val lines = entries.sortedBy { it.timestamp }.joinToString("\n") {
        val label = if (it.kind == 0) "دين" else "تسديد"
        val whenText = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar", "DZ")).format(Date(it.timestamp))
        "$whenText — $label: ${money(it.amount, currency)} ${it.note}"
    }
    val text = "كشف حساب الزبون\nالاسم: ${customer.name}\nالهاتف: ${customer.phone}\nالتاريخ: $date\n\n${lines.ifBlank { "لا توجد عمليات بعد" }}\n\nالمبلغ المتبقي: ${money(balance, currency)}"
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, "مشاركة كشف الحساب"))
}

fun shareCustomerStatementPdf(context: Context, customer: CustomerEntity, entries: List<LedgerEntryEntity>, currency: String) {
    val balance = entries.sumOf { if (it.kind == 0) it.amount else -it.amount }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar", "DZ"))
    val body = buildString {
        append("كشف حساب الزبون\n")
        append("الاسم: ${customer.name}\nالهاتف: ${customer.phone.ifBlank { "غير مسجل" }}\n")
        append("تاريخ الكشف: ${dateFormat.format(Date())}\n\n")
        entries.sortedByDescending { it.timestamp }.forEach {
            append("${dateFormat.format(Date(it.timestamp))}\n")
            append("${if (it.kind == 0) "دين جديد" else "دفعة سداد"}: ${money(it.amount, currency)}\n")
            if (it.note.isNotBlank()) append("ملاحظة: ${it.note}\n")
            append("------------------------------\n")
        }
        append("\nالمبلغ المتبقي: ${money(balance, currency)}")
    }

    val document = PdfDocument()
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15f; color = android.graphics.Color.BLACK }
    val layout = StaticLayout.Builder.obtain(body, 0, body.length, paint, 535)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setTextDirection(TextDirectionHeuristics.RTL)
        .setLineSpacing(5f, 1.15f)
        .build()
    val printableHeight = 770
    val pageCount = ((layout.height + printableHeight - 1) / printableHeight).coerceAtLeast(1)
    for (pageNumber in 0 until pageCount) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber + 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.save()
        page.canvas.translate(30f, 34f - pageNumber * printableHeight)
        layout.draw(page.canvas)
        page.canvas.restore()
        document.finishPage(page)
    }

    val output = File(context.cacheDir, "statement_${customer.id}.pdf")
    FileOutputStream(output).use { document.writeTo(it) }
    document.close()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
    val intent = Intent(Intent.ACTION_SEND).setType("application/pdf")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "مشاركة كشف الحساب PDF"))
}

fun money(value: Double, currency: String): String = String.format(Locale.US, "%.2f %s", value, currency)
