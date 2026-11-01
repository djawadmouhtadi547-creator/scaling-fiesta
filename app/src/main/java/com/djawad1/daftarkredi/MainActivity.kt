package com.djawad1.daftarkredi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.djawad1.daftarkredi.data.CustomerEntity
import com.djawad1.daftarkredi.data.LedgerEntryEntity
import com.djawad1.daftarkredi.data.ProductEntity
import com.djawad1.daftarkredi.ui.BarcodeScannerScreen
import com.djawad1.daftarkredi.ui.DaftarTheme
import com.djawad1.daftarkredi.ui.MainViewModel
import com.djawad1.daftarkredi.util.BackupUtils
import com.djawad1.daftarkredi.util.money
import com.djawad1.daftarkredi.util.shareCustomerStatementPdf
import com.djawad1.daftarkredi.util.shareCustomerStatementText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                DaftarAlKrediApp()
            }
        }
    }
}

private data class InvoiceLine(
    val key: Long = System.nanoTime(),
    val productId: Long? = null,
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val imagePath: String = ""
)

@Composable
fun DaftarAlKrediApp(vm: MainViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    val products by vm.products.collectAsState()
    val preferences = remember { context.getSharedPreferences("settings", 0) }

    var themeName by remember { mutableStateOf(preferences.getString("theme", "light") ?: "light") }
    var currency by remember { mutableStateOf(preferences.getString("currency", "دج") ?: "دج") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf(preferences.getString("pin", "") ?: "") }
    var unlocked by remember { mutableStateOf(pin.isBlank()) }
    var pinInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var backupToRestore by remember { mutableStateOf<BackupUtils.BackupData?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            val json = BackupUtils.createJson(customers, entries, products)
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } }
                .onSuccess { message = "تم حفظ النسخة الاحتياطية" }
                .onFailure { message = "تعذّر حفظ النسخة: ${it.message}" }
        }
    }
    val appScope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            // File parsing runs off the UI thread, then Room replaces the local data in one transaction.
            appScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("تعذّر فتح الملف")
                        BackupUtils.parseJson(text, context)
                    }
                }.onSuccess { backup ->
                    backupToRestore = backup
                }.onFailure { message = "ملف النسخة غير صالح: ${it.message}" }
            }
        }
    }

    DaftarTheme(themeName) {
        if (!unlocked) {
            PinUnlockDialog(
                value = pinInput,
                onValueChange = { pinInput = it.take(8) },
                onConfirm = {
                    if (pinInput == pin) { unlocked = true; pinInput = "" }
                    else { message = "الرمز غير صحيح"; pinInput = "" }
                }
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        val labels = listOf("الزبائن", "الإحصائيات", "الكاشير", "الحاسبة", "الإعدادات")
                        val icons = listOf(Icons.Default.Group, Icons.Default.ShowChart, Icons.Default.Storefront, Icons.Default.Calculate, Icons.Default.Settings)
                        labels.forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(icons[index], contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, maxLines = 1) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (selectedTab) {
                        0 -> CustomersScreen(customers, entries, currency, vm, onMessage = { message = it })
                        1 -> StatisticsScreen(customers, entries, currency)
                        2 -> CashierScreen(customers, entries, products, currency, vm, onMessage = { message = it })
                        3 -> CalculatorScreen(currency)
                        else -> SettingsScreen(
                            themeName = themeName,
                            currency = currency,
                            pinEnabled = pin.isNotBlank(),
                            onTheme = { themeName = it; preferences.edit().putString("theme", it).apply() },
                            onCurrency = { currency = it; preferences.edit().putString("currency", it).apply() },
                            onSetPin = { newPin -> pin = newPin; unlocked = true; preferences.edit().putString("pin", newPin).apply() },
                            onRemovePin = { pin = ""; unlocked = true; preferences.edit().remove("pin").apply() },
                            onExport = { exportLauncher.launch("daftar-al-kredi-backup.json") },
                            onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                            onMessage = { message = it }
                        )
                    }
                }
            }
        }
    }

    backupToRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text("تأكيد استعادة النسخة") },
            text = { Text("سيتم استبدال البيانات الحالية بـ ${backup.customers.size} زبون و${backup.entries.size} عملية و${backup.products.size} منتج. لا يمكن التراجع عن ذلك. هل تريد المتابعة؟") },
            confirmButton = {
                Button(onClick = {
                    vm.restore(backup.customers, backup.entries, backup.products) { _, result -> message = result }
                    backupToRestore = null
                }) { Text("استعادة البيانات") }
            },
            dismissButton = { TextButton(onClick = { backupToRestore = null }) { Text("إلغاء") } }
        )
    }

    if (message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { message = "" },
            confirmButton = { TextButton(onClick = { message = "" }) { Text("حسناً") } },
            text = { Text(message, textAlign = TextAlign.Center) }
        )
    }
}

@Composable
private fun PinUnlockDialog(value: String, onValueChange: (String) -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Default.Lock, null) },
        title = { Text("التطبيق مقفل", textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("أدخل رمز PIN للمتابعة")
                OutlinedTextField(value, onValueChange, label = { Text("رمز PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("فتح التطبيق") } }
    )
}

@Composable
private fun ScreenTitle(title: String, subtitle: String = "") {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle.isNotBlank()) Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun customerBalance(customerId: Long, entries: List<LedgerEntryEntity>): Double =
    entries.asSequence().filter { it.customerId == customerId }.sumOf { if (it.kind == 0) it.amount else -it.amount }

@Composable
private fun CustomersScreen(
    customers: List<CustomerEntity>,
    entries: List<LedgerEntryEntity>,
    currency: String,
    vm: MainViewModel,
    onMessage: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("الاسم") }
    var sortMenu by remember { mutableStateOf(false) }
    var addCustomer by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    val totalDebt = customers.sumOf { customerBalance(it.id, entries).coerceAtLeast(0.0) }
    val filtered = customers.filter { it.name.contains(query, true) || it.phone.contains(query, true) }
    val sorted = when (sort) {
        "أكبر دين" -> filtered.sortedByDescending { customerBalance(it.id, entries) }
        "آخر عملية" -> filtered.sortedByDescending { customer -> entries.filter { it.customerId == customer.id }.maxOfOrNull { it.timestamp } ?: 0L }
        else -> filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        ScreenTitle("الزبائن", "سجّل الديون والدفعات وتابع المبالغ المتبقية")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("إجمالي الديون المتبقية", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 15.sp)
                Text(money(totalDebt, currency), color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("عدد الزبائن: ${customers.size}", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("ابحث بالاسم أو رقم الهاتف") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("قائمة الزبائن", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Box {
                OutlinedButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Tune, null); Spacer(Modifier.width(5.dp)); Text("ترتيب: $sort") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf("الاسم", "أكبر دين", "آخر عملية").forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { sort = option; sortMenu = false })
                    }
                }
            }
        }
        Button(onClick = { addCustomer = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("إضافة زبون جديد", fontSize = 17.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (sorted.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (query.isBlank()) "لا يوجد زبائن بعد. أضف أول زبون للبدء." else "لا توجد نتائج مطابقة للبحث", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sorted, key = { it.id }) { customer ->
                    val balance = customerBalance(customer.id, entries)
                    CustomerRow(customer, balance, currency) { selectedCustomer = customer }
                }
            }
        }
    }

    if (addCustomer) CustomerFormDialog(onDismiss = { addCustomer = false }, onSave = { name, phone, note -> vm.addCustomer(name, phone, note); addCustomer = false })
    selectedCustomer?.let { customer ->
        CustomerDetailDialog(
            customer = customer,
            entries = entries.filter { it.customerId == customer.id }.sortedByDescending { it.timestamp },
            currency = currency,
            onDismiss = { selectedCustomer = null },
            onAddEntry = { kind, amount, note, timestamp -> vm.addEntry(customer.id, kind, amount, note, timestamp) },
            onUpdateEntry = { vm.updateEntry(it) },
            onDeleteEntry = { vm.deleteEntry(it) },
            onDeleteCustomer = { vm.deleteCustomer(customer.id); selectedCustomer = null },
            onShareText = { shareCustomerStatementText(it, customer, entries.filter { e -> e.customerId == customer.id }, currency) },
            onSharePdf = { shareCustomerStatementPdf(it, customer, entries.filter { e -> e.customerId == customer.id }, currency) },
            onEditCustomer = { updated -> vm.updateCustomer(updated) }
        )
    }
}

@Composable
private fun CustomerRow(customer: CustomerEntity, balance: Double, currency: String, onClick: () -> Unit) {
    val owing = balance > 0.009
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(customer.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (customer.phone.isNotBlank()) Text(customer.phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (owing) "عليه مبلغ" else "مسدّد بالكامل", color = if (owing) Color(0xFFB3261E) else Color(0xFF16834A), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(balance.coerceAtLeast(0.0), currency), color = if (owing) Color(0xFFB3261E) else Color(0xFF16834A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("المتبقي", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CustomerFormDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit, initial: CustomerEntity? = null) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة زبون" else "تعديل بيانات الزبون") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم الزبون *") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("رقم الهاتف (اختياري)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(note, { note = it }, label = { Text("ملاحظة (اختياري)") }, minLines = 2)
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onSave(name, phone, note) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun CustomerDetailDialog(
    customer: CustomerEntity,
    entries: List<LedgerEntryEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onAddEntry: (Int, Double, String, Long) -> Unit,
    onUpdateEntry: (LedgerEntryEntity) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onDeleteCustomer: () -> Unit,
    onShareText: (android.content.Context) -> Unit,
    onSharePdf: (android.content.Context) -> Unit,
    onEditCustomer: (CustomerEntity) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var entryKind by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<LedgerEntryEntity?>(null) }
    var deleting by remember { mutableStateOf<LedgerEntryEntity?>(null) }
    var confirmDeleteCustomer by remember { mutableStateOf(false) }
    var editCustomer by remember { mutableStateOf(false) }
    val balance = entries.sumOf { if (it.kind == 0) it.amount else -it.amount }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.88f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(customer.name, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text(customer.phone.ifBlank { "بدون رقم هاتف" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editCustomer = true }) { Icon(Icons.Default.Edit, "تعديل") }
                    IconButton(onClick = onDismiss) { Text("✕", fontSize = 20.sp) }
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp)) {
                        Text("المبلغ المتبقي", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(money(balance.coerceAtLeast(0.0), currency), fontSize = 27.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { entryKind = 0 }, modifier = Modifier.weight(1f)) { Text("إضافة دين") }
                    OutlinedButton(onClick = { entryKind = 1 }, modifier = Modifier.weight(1f)) { Text("تسجيل دفعة") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = { onShareText(context) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(4.dp)); Text("مشاركة") }
                    FilledTonalButton(onClick = { onSharePdf(context) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(4.dp)); Text("PDF") }
                }
                if (customer.phone.isNotBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))) }
                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Call, null); Spacer(Modifier.width(4.dp)); Text("اتصال") }
                        OutlinedButton(onClick = {
                            val rawNumber = customer.phone.filter { it.isDigit() }
                            val number = when {
                                rawNumber.startsWith("0") -> "213${rawNumber.drop(1)}"
                                rawNumber.startsWith("213") -> rawNumber
                                else -> rawNumber
                            }
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number"))) }
                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Message, null); Spacer(Modifier.width(4.dp)); Text("واتساب") }
                    }
                }
                Text("سجل العمليات", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                if (entries.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("لا توجد عمليات لهذا الزبون") }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(entries, key = { it.id }) { entry ->
                            LedgerEntryRow(entry, currency, onEdit = { editing = entry }, onDelete = { deleting = entry })
                        }
                    }
                }
                OutlinedButton(onClick = { confirmDeleteCustomer = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("حذف الزبون وكل عملياته")
                }
            }
        }
    }

    entryKind?.let { kind ->
        EntryFormDialog(kind = kind, currency = currency, onDismiss = { entryKind = null }, onSave = { amount, note, timestamp -> onAddEntry(kind, amount, note, timestamp); entryKind = null })
    }
    editing?.let { entry ->
        EntryFormDialog(kind = entry.kind, currency = currency, initial = entry, onDismiss = { editing = null }, onSave = { amount, note, timestamp ->
            onUpdateEntry(entry.copy(amount = amount, note = note, timestamp = timestamp)); editing = null
        })
    }
    deleting?.let { entry ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("حذف العملية؟") }, text = { Text("سيتم حذف هذه العملية من سجل الزبون.") },
            confirmButton = { Button(onClick = { onDeleteEntry(entry.id); deleting = null }) { Text("حذف") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } })
    }
    if (confirmDeleteCustomer) {
        AlertDialog(onDismissRequest = { confirmDeleteCustomer = false }, title = { Text("حذف الزبون؟") }, text = { Text("سيتم حذف الزبون وجميع الديون والدفعات المرتبطة به. لا يمكن التراجع عن ذلك.") },
            confirmButton = { Button(onClick = { onDeleteCustomer(); confirmDeleteCustomer = false }) { Text("حذف نهائياً") } }, dismissButton = { TextButton(onClick = { confirmDeleteCustomer = false }) { Text("إلغاء") } })
    }
    if (editCustomer) CustomerFormDialog(initial = customer, onDismiss = { editCustomer = false }, onSave = { name, phone, note -> onEditCustomer(customer.copy(name = name, phone = phone, note = note)); editCustomer = false })
}

@Composable
private fun LedgerEntryRow(entry: LedgerEntryEntity, currency: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val date = remember(entry.timestamp) { SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("ar", "DZ")).format(Date(entry.timestamp)) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (entry.kind == 0) "دين جديد" else "دفعة سداد", fontWeight = FontWeight.Bold, color = if (entry.kind == 0) Color(0xFFB3261E) else Color(0xFF16834A))
                Text(date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.note.isNotBlank()) Text(entry.note, fontSize = 13.sp)
            }
            Text(money(entry.amount, currency), fontWeight = FontWeight.Bold)
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "تعديل") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun EntryFormDialog(kind: Int, currency: String, initial: LedgerEntryEntity? = null, onDismiss: () -> Unit, onSave: (Double, String, Long) -> Unit) {
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var timestampText by remember { mutableStateOf(initial?.timestamp?.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(it)) } ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "تعديل العملية" else if (kind == 0) "إضافة دين جديد" else "تسجيل دفعة سداد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("المبلغ ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(timestampText, { timestampText = it }, label = { Text("التاريخ والوقت: yyyy-MM-dd HH:mm") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("ملاحظة") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = amount.replace(',', '.').toDoubleOrNull()
                val time = runCatching { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(timestampText)?.time }.getOrNull() ?: System.currentTimeMillis()
                if (value != null && value > 0) onSave(value, note, time)
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun CashierScreen(
    customers: List<CustomerEntity>,
    entries: List<LedgerEntryEntity>,
    products: List<ProductEntity>,
    currency: String,
    vm: MainViewModel,
    onMessage: (String) -> Unit
) {
    var invoice by remember { mutableStateOf(emptyList<InvoiceLine>()) }
    var showScanner by remember { mutableStateOf(false) }
    var showProductForm by remember { mutableStateOf(false) }
    var productFormInitial by remember { mutableStateOf<ProductEntity?>(null) }
    var pendingBarcode by remember { mutableStateOf("") }
    var showLibrary by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("وجّه الكاميرا نحو باركود المنتج") }
    val total = invoice.sumOf { it.price * it.quantity }

    fun addProductLine(product: ProductEntity) {
        val existingIndex = invoice.indexOfFirst { it.productId == product.id }
        invoice = if (existingIndex >= 0) invoice.mapIndexed { index, line ->
            if (index == existingIndex) line.copy(quantity = line.quantity + 1) else line
        } else invoice + InvoiceLine(productId = product.id, name = product.name, price = product.price, imagePath = product.imagePath)
    }

    if (showScanner) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            BarcodeScannerScreen(
                onBarcode = { code ->
                    pendingBarcode = code
                    vm.findProduct(code) { product ->
                        if (product != null) {
                            addProductLine(product)
                            scanStatus = "تمت إضافة ${product.name} — امسح المنتج التالي"
                        } else {
                            showScanner = false
                            scanStatus = "منتج جديد: أدخل بياناته مرة واحدة"
                            productFormInitial = null
                            showProductForm = true
                        }
                    }
                },
                onClose = { showScanner = false }
            )
            Surface(Modifier.align(Alignment.TopCenter).fillMaxWidth(), color = Color.Black.copy(alpha = 0.55f)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(scanStatus, color = Color.White, modifier = Modifier.weight(1f), fontSize = 17.sp)
                    TextButton(onClick = { showScanner = false }) { Text("إغلاق", color = Color.White) }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        ScreenTitle("الكاشير", "امسح المنتجات واجمع الفاتورة ثم أضفها إلى حساب الزبون")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("إجمالي الفاتورة", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(money(total, currency), color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("${invoice.sumOf { it.quantity }} قطعة · ${invoice.size} منتج مختلف", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Icon(Icons.Default.ReceiptLong, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(45.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Button(onClick = { showScanner = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.width(4.dp)); Text("مسح باركود")
            }
            OutlinedButton(onClick = { showLibrary = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Inventory2, null); Spacer(Modifier.width(4.dp)); Text("المنتجات")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilledTonalButton(onClick = { pendingBarcode = ""; productFormInitial = null; showProductForm = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("إضافة منتج")
            }
            OutlinedButton(onClick = { invoice = emptyList() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("فاتورة جديدة")
            }
        }
        Text("محتويات الفاتورة", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        if (invoice.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
                    Text("ابدأ بمسح باركود منتج أو اختره من قائمة المنتجات", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(invoice, key = { it.key }) { line ->
                    InvoiceLineRow(
                        line = line, currency = currency,
                        onMinus = { invoice = invoice.map { if (it.key == line.key) it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) else it } },
                        onPlus = { invoice = invoice.map { if (it.key == line.key) it.copy(quantity = it.quantity + 1) else it } },
                        onRemove = { invoice = invoice.filterNot { it.key == line.key } }
                    )
                }
            }
        }
        Button(onClick = { if (invoice.isNotEmpty()) showCustomerPicker = true }, enabled = invoice.isNotEmpty() && customers.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Payments, null); Spacer(Modifier.width(8.dp)); Text("إضافة ${money(total, currency)} كدين على زبون", fontSize = 16.sp)
        }
        if (customers.isEmpty()) Text("أضف زبوناً أولاً من تبويب الزبائن حتى تتمكن من تسجيل الفاتورة كدين.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
    }

    if (showProductForm) {
        ProductFormDialog(
            initial = productFormInitial,
            initialBarcode = pendingBarcode,
            onDismiss = { showProductForm = false; pendingBarcode = ""; productFormInitial = null },
            onSave = { name, barcode, price, imagePath ->
                val editing = productFormInitial
                val continueScanning = editing == null && pendingBarcode.isNotBlank()
                if (editing != null) {
                    val updated = editing.copy(name = name, barcode = barcode, price = price, imagePath = imagePath)
                    vm.updateProduct(updated)
                    invoice = invoice.map { if (it.productId == editing.id) it.copy(name = name, price = price, imagePath = imagePath) else it }
                    onMessage("تم تحديث المنتج")
                } else {
                    vm.addProduct(name, barcode, price, imagePath) { id ->
                        invoice = invoice + InvoiceLine(productId = id, name = name, price = price, imagePath = imagePath)
                        if (continueScanning) {
                            scanStatus = "تم حفظ المنتج — امسح المنتج التالي"
                            showScanner = true
                        } else onMessage("تم حفظ المنتج وإضافته للفاتورة")
                    }
                }
                showProductForm = false
                pendingBarcode = ""
                productFormInitial = null
            }
        )
    }

    if (showLibrary) {
        ProductLibraryDialog(
            products = products,
            currency = currency,
            onDismiss = { showLibrary = false },
            onChoose = { product -> addProductLine(product); showLibrary = false },
            onEdit = { product -> showLibrary = false; productFormInitial = product; showProductForm = true },
            onDelete = { product -> vm.deleteProduct(product.id) }
        )
    }

    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("اختر الزبون لإضافة الفاتورة") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(customers, key = { it.id }) { customer ->
                        Card(Modifier.fillMaxWidth().clickable {
                            vm.addEntry(customer.id, 0, total, "فاتورة كاشير: ${invoice.joinToString { "${it.name} × ${it.quantity}" }}")
                            invoice = emptyList()
                            showCustomerPicker = false
                            onMessage("تمت إضافة الفاتورة إلى حساب ${customer.name}")
                        }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.SemiBold)
                                    Text("المتبقي: ${money(customerBalance(customer.id, entries).coerceAtLeast(0.0), currency)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("اختيار", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCustomerPicker = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun InvoiceLineRow(line: InvoiceLine, currency: String, onMinus: () -> Unit, onPlus: () -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (line.imagePath.isNotBlank() && File(line.imagePath).exists()) {
                AsyncImage(model = File(line.imagePath), contentDescription = line.name, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(line.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${money(line.price, currency)} × ${line.quantity}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(money(line.price * line.quantity, currency), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onMinus) { Text("−", fontSize = 23.sp) }
            Text(line.quantity.toString(), fontWeight = FontWeight.Bold)
            IconButton(onClick = onPlus) { Text("+", fontSize = 21.sp) }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ProductFormDialog(
    initial: ProductEntity? = null,
    initialBarcode: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember(initial?.id, initialBarcode) { mutableStateOf(initial?.name ?: "") }
    var barcode by remember(initial?.id, initialBarcode) { mutableStateOf(initial?.barcode?.takeIf { it.isNotBlank() } ?: initialBarcode) }
    var price by remember(initial?.id) { mutableStateOf(initial?.price?.toString() ?: "") }
    var imagePath by remember(initial?.id) { mutableStateOf(initial?.imagePath ?: "") }
    var imageError by remember { mutableStateOf("") }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = captureUri
        if (success && uri != null) {
            runCatching { BackupUtils.copyImageToApp(context, uri) }
                .onSuccess { imagePath = it; imageError = "" }
                .onFailure { imageError = "تعذّر حفظ الصورة" }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { BackupUtils.copyImageToApp(context, uri) }
                .onSuccess { imagePath = it; imageError = "" }
                .onFailure { imageError = "تعذّر حفظ الصورة" }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "حفظ منتج جديد" else "تعديل المنتج") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم المنتج *") }, singleLine = true)
                OutlinedTextField(barcode, { barcode = it }, label = { Text("الباركود") }, singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text("السعر") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (imagePath.isNotBlank() && File(imagePath).exists()) AsyncImage(File(imagePath), null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
                    else Box(Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, null) }
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = {
                            runCatching {
                                val file = File(context.cacheDir, "product_capture_${System.currentTimeMillis()}.jpg")
                                val outputUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                captureUri = outputUri
                                cameraLauncher.launch(outputUri)
                            }.onFailure { imageError = "تعذّر فتح الكاميرا" }
                        }) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(4.dp)); Text("تصوير") }
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("من المعرض") }
                    }
                }
                if (imageError.isNotBlank()) Text(imageError, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = price.replace(',', '.').toDoubleOrNull()
                if (name.isNotBlank() && value != null && value >= 0) onSave(name, barcode, value, imagePath)
            }) { Text("حفظ المنتج") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun ProductLibraryDialog(
    products: List<ProductEntity>, currency: String, onDismiss: () -> Unit,
    onChoose: (ProductEntity) -> Unit, onEdit: (ProductEntity) -> Unit, onDelete: (ProductEntity) -> Unit
) {
    var deleting by remember { mutableStateOf<ProductEntity?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.82f), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("المنتجات المحفوظة", fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("إغلاق") }
                }
                if (products.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("لم تحفظ منتجات بعد. امسح باركود منتج جديد أو أضفه يدوياً.", textAlign = TextAlign.Center) }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(products, key = { it.id }) { product ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (product.imagePath.isNotBlank() && File(product.imagePath).exists()) AsyncImage(File(product.imagePath), null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                                    else Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.Bold)
                                        Text("${money(product.price, currency)}${if (product.barcode.isNotBlank()) " · ${product.barcode}" else ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = { onChoose(product) }) { Text("إضافة") }
                                    IconButton(onClick = { onEdit(product) }) { Icon(Icons.Default.Edit, null) }
                                    IconButton(onClick = { deleting = product }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { product ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("حذف المنتج؟") }, text = { Text("سيُحذف ${product.name} من قائمة المنتجات المحفوظة.") },
            confirmButton = { Button(onClick = { onDelete(product); deleting = null }) { Text("حذف") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } })
    }
}

@Composable
private fun StatisticsScreen(customers: List<CustomerEntity>, entries: List<LedgerEntryEntity>, currency: String) {
    var period by remember { mutableIntStateOf(0) }
    var customStart by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))) }
    var customEnd by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    val now = System.currentTimeMillis()
    val weekCalendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val monthCalendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val parsedStart = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(customStart)?.time }.getOrNull() ?: (now - 30L * 24 * 60 * 60 * 1000)
    val parsedEnd = (runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(customEnd)?.time }.getOrNull() ?: now) + 24L * 60 * 60 * 1000 - 1
    val start = when (period) { 0 -> weekCalendar.timeInMillis; 1 -> monthCalendar.timeInMillis; else -> minOf(parsedStart, parsedEnd) }
    val end = when (period) { 0, 1 -> now; else -> maxOf(parsedStart, parsedEnd) }
    val inPeriod = entries.filter { it.timestamp in start..end }
    val debts = inPeriod.filter { it.kind == 0 }
    val payments = inPeriod.filter { it.kind == 1 }
    val totalNew = debts.sumOf { it.amount }
    val totalPaid = payments.sumOf { it.amount }
    val grouped = debts.groupBy { it.customerId }.mapValues { (_, list) -> list.sumOf { it.amount } }
    val biggest = grouped.maxByOrNull { it.value }?.let { pair -> customers.firstOrNull { it.id == pair.key }?.name to pair.value }
    val smallest = grouped.minByOrNull { it.value }?.let { pair -> customers.firstOrNull { it.id == pair.key }?.name to pair.value }
    val topDebtors = customers.map { it to customerBalance(it.id, entries).coerceAtLeast(0.0) }
        .filter { it.second > 0.009 }.sortedByDescending { it.second }.take(5)
    val weeklyDebts = entries.filter { it.kind == 0 && it.timestamp >= weekCalendar.timeInMillis && it.timestamp <= now }
    val weekDays = (0..6).map { offset ->
        val dayStart = weekCalendar.timeInMillis + offset * 24L * 60 * 60 * 1000
        val dayEnd = dayStart + 24L * 60 * 60 * 1000 - 1
        val amount = weeklyDebts.filter { it.timestamp in dayStart..dayEnd }.sumOf { it.amount }
        val label = SimpleDateFormat("EEE", Locale("ar", "DZ")).format(Date(dayStart))
        label to amount
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenTitle("الإحصائيات", "نظرة سريعة على حركة الديون والتسديدات")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("أسبوعي", "شهري", "مخصص").forEachIndexed { index, label ->
                if (period == index) Button(onClick = { period = index }, modifier = Modifier.weight(1f)) { Text(label) }
                else OutlinedButton(onClick = { period = index }, modifier = Modifier.weight(1f)) { Text(label) }
            }
        }
        if (period == 2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(customStart, { customStart = it }, label = { Text("من yyyy-MM-dd") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(customEnd, { customEnd = it }, label = { Text("إلى yyyy-MM-dd") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("ديون جديدة", money(totalNew, currency), Color(0xFFB3261E), Modifier.weight(1f))
                    MetricCard("مبالغ مسددة", money(totalPaid, currency), Color(0xFF16834A), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("عدد العمليات", inPeriod.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    MetricCard("صافي الحركة", money(totalNew - totalPaid, currency), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("حركة الديون خلال الأسبوع", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        val maxValue = (weekDays.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
                        val chartColor = MaterialTheme.colorScheme.primary
                        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                            val slot = size.width / 7f
                            val barWidth = slot * 0.55f
                            weekDays.forEachIndexed { index, pair ->
                                val barHeight = (pair.second / maxValue).toFloat() * (size.height - 8f)
                                val left = slot * index + (slot - barWidth) / 2f
                                drawRect(chartColor, topLeft = androidx.compose.ui.geometry.Offset(left, size.height - barHeight), size = androidx.compose.ui.geometry.Size(barWidth, barHeight))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            weekDays.forEach { (label, _) -> Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الزبائن حسب ديون الفترة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("أكثر زبون أخذ ديناً", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(biggest?.let { "${it.first ?: "زبون محذوف"} — ${money(it.second, currency)}" } ?: "لا توجد ديون في الفترة", fontWeight = FontWeight.Bold, color = Color(0xFFB3261E))
                        Divider()
                        Text("أقل زبون أخذ ديناً", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(smallest?.let { "${it.first ?: "زبون محذوف"} — ${money(it.second, currency)}" } ?: "لا توجد ديون في الفترة", fontWeight = FontWeight.Bold, color = Color(0xFF16834A))
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("أكبر 5 مدينين حالياً", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (topDebtors.isEmpty()) Text("لا يوجد زبائن عليهم ديون حالياً")
                        topDebtors.forEachIndexed { index, row ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(row.first.name, modifier = Modifier.weight(1f))
                                Text(money(row.second, currency), color = Color(0xFFB3261E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        }
    }
}

@Composable
private fun CalculatorScreen(currency: String) {
    var display by rememberSaveable { mutableStateOf("0") }
    var stored by rememberSaveable { mutableStateOf<Double?>(null) }
    var operation by rememberSaveable { mutableStateOf<String?>(null) }
    var replaceDisplay by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf("") }

    fun number(value: String) {
        display = when {
            replaceDisplay -> if (value == ".") "0." else value
            value == "." && display.contains('.') -> display
            display == "0" && value != "." -> value
            else -> display + value
        }
        replaceDisplay = false
    }
    fun calculate(a: Double, b: Double, op: String): Double? = when (op) {
        "+" -> a + b
        "−" -> a - b
        "×" -> a * b
        "÷" -> if (b == 0.0) null else a / b
        else -> b
    }
    fun chooseOperation(op: String) {
        val current = display.toDoubleOrNull() ?: 0.0
        if (stored != null && operation != null && !replaceDisplay) {
            val result = calculate(stored!!, current, operation!!)
            if (result == null) { display = "خطأ"; stored = null; operation = null; replaceDisplay = true; return }
            stored = result
            display = result.toString().removeSuffix(".0")
        } else stored = current
        operation = op
        replaceDisplay = true
        history = "${stored ?: current} $op"
    }
    fun equalsResult() {
        val a = stored ?: return
        val op = operation ?: return
        val b = display.toDoubleOrNull() ?: 0.0
        val result = calculate(a, b, op)
        display = result?.let { if (it.isFinite()) String.format(Locale.US, "%.8f", it).trimEnd('0').trimEnd('.') else "خطأ" } ?: "خطأ"
        history = "$a $op $b ="
        stored = null; operation = null; replaceDisplay = true
    }
    val keys = listOf(
        listOf("C", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("±", "0", ".", "=")
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenTitle("الآلة الحاسبة", "عمليات يومية سريعة وواضحة")
        Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                Text(history, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                Text(display, fontSize = 40.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 2)
                Text(currency, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val isOperator = key in listOf("÷", "×", "−", "+", "=")
                    val action: () -> Unit = {
                        when (key) {
                            "C" -> { display = "0"; stored = null; operation = null; replaceDisplay = false; history = "" }
                            "⌫" -> { display = if (display.length <= 1) "0" else display.dropLast(1); replaceDisplay = false }
                            "%" -> { display = ((display.toDoubleOrNull() ?: 0.0) / 100.0).toString(); replaceDisplay = true }
                            "±" -> { display = ((display.toDoubleOrNull() ?: 0.0) * -1).toString(); replaceDisplay = false }
                            "÷", "×", "−", "+" -> chooseOperation(key)
                            "=" -> equalsResult()
                            else -> number(key)
                        }
                    }
                    if (isOperator) Button(onClick = action, modifier = Modifier.weight(1f).height(62.dp)) { Text(key, fontSize = 23.sp) }
                    else OutlinedButton(onClick = action, modifier = Modifier.weight(1f).height(62.dp)) { Text(key, fontSize = 22.sp) }
                }
            }
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun SettingsScreen(
    themeName: String,
    currency: String,
    pinEnabled: Boolean,
    onTheme: (String) -> Unit,
    onCurrency: (String) -> Unit,
    onSetPin: (String) -> Unit,
    onRemovePin: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onMessage: (String) -> Unit
) {
    var currencyMenu by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val themes = listOf(
        Triple("light", "أبيض", Color(0xFFF5F7FA)),
        Triple("dark", "أسود", Color(0xFF15191F)),
        Triple("mint", "أخضر نعناعي", Color(0xFFBFE9D9)),
        Triple("orange", "برتقالي", Color(0xFFFFD0A8)),
        Triple("navy", "أزرق بحري", Color(0xFFB5D0E8))
    )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenTitle("الإعدادات", "خصّص التطبيق واحفظ بيانات تجارتك بأمان")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("لون النظام", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        themes.forEach { (id, label, color) ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onTheme(id) }.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(30.dp).clip(CircleShape).background(color))
                                Text(label, modifier = Modifier.weight(1f).padding(start = 10.dp))
                                if (themeName == id) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("العملة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Box {
                            OutlinedButton(onClick = { currencyMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("العملة الحالية: $currency") }
                            DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                                listOf("دج", "ر.س", "د.م", "€", "$", "£").forEach { item ->
                                    DropdownMenuItem(text = { Text(item) }, onClick = { onCurrency(item); currencyMenu = false })
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("قفل التطبيق", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (pinEnabled) "القفل برمز PIN مفعّل." else "يمكنك حماية التطبيق برمز PIN من اختيارك.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (pinEnabled) OutlinedButton(onClick = onRemovePin, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(7.dp)); Text("إلغاء قفل PIN") }
                        else Button(onClick = { pinDialog = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(7.dp)); Text("تفعيل قفل PIN") }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("النسخ الاحتياطي", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("صدّر الزبائن والديون والمنتجات في ملف JSON، ثم احتفظ به في الهاتف أو Google Drive.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("تصدير نسخة احتياطية") }
                        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("استعادة نسخة احتياطية") }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("حول التطبيق", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("دفتر الكريدي · الإصدار 1.0.0\nيعمل محلياً دون إنترنت. بياناتك محفوظة على هذا الهاتف.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (pinDialog) {
        AlertDialog(
            onDismissRequest = { pinDialog = false; newPin = ""; confirmPin = "" },
            title = { Text("إنشاء رمز PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(8) }, label = { Text("رمز PIN (4 أرقام أو أكثر)") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
                    OutlinedTextField(confirmPin, { confirmPin = it.filter(Char::isDigit).take(8) }, label = { Text("تأكيد الرمز") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
                }
            },
            confirmButton = { Button(onClick = {
                if (newPin.length >= 4 && newPin == confirmPin) { onSetPin(newPin); pinDialog = false; newPin = ""; confirmPin = "" }
                else onMessage("تأكد من أن الرمز 4 أرقام على الأقل وأن التأكيد مطابق")
            }) { Text("حفظ الرمز") } },
            dismissButton = { TextButton(onClick = { pinDialog = false }) { Text("إلغاء") } }
        )
    }
}
