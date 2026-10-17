package dev.monthly.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val chartColors = listOf(Color(0xFF626EE9), Color(0xFF2BB99B), Color(0xFFF0AA55), Color(0xFFB48ADF))

@Composable
fun BudgetApp(storage: BudgetStorage) {
    val scheme = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFFBDC2FF))
        else lightColorScheme(primary = Color(0xFF5661D7))
    MaterialTheme(colorScheme = scheme) { BudgetScreen(storage) }
}

@Composable
private fun BudgetScreen(storage: BudgetStorage) {
    val store = remember(storage) { BudgetStore(storage) }
    val scope = rememberCoroutineScope()
    var payments by remember { mutableStateOf<List<Payment>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }

    LaunchedEffect(store, retry) {
        loadFailed = false
        try { payments = store.load() }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            loadFailed = true
        }
    }

    fun save(updated: List<Payment>) {
        if (saving) return
        saving = true
        saveFailed = false
        scope.launch {
            try {
                store.save(updated)
                payments = updated
                showEditor = false
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                saveFailed = true
            } finally { saving = false }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            if (payments != null && payments.orEmpty().size < 1000) {
                FloatingActionButton(onClick = {
                    if (!saving) {
                        // Keep the draft identity if a save finishes during activity recreation.
                        selectedId = (1L..1001L).first { id -> payments.orEmpty().none { it.id == id } }
                        saveFailed = false
                        showEditor = true
                    }
                }, modifier = Modifier.semantics { contentDescription = "Add payment" }) {
                    Text("+", fontSize = 28.sp)
                }
            }
        },
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            when {
                loadFailed -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn’t open your saved budget.")
                    Text("Your saved data has been kept.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { retry++ }) { Text("Retry") }
                }
                payments == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> {
                    val bills = payments.orEmpty()
                    val total = bills.sumOf { it.cents }
                    LazyColumn(
                        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            Text("My budget", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Text("Monthly spending", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Monthly total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(money(total), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(24.dp))
                                    SpendingChart(bills, total)
                                }
                            }
                        }
                        item { Text("Your payments", style = MaterialTheme.typography.titleMedium) }
                        if (bills.isEmpty()) item { Text("No payments yet. Tap + to add one.") }
                        itemsIndexed(bills, key = { _, bill -> bill.id }) { index, bill ->
                            Surface(
                                onClick = { selectedId = bill.id; saveFailed = false; showEditor = true },
                                enabled = !saving,
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(chartColors[index % chartColors.size]))
                                    Column(Modifier.weight(1f)) {
                                        Text(bill.name, style = MaterialTheme.typography.titleSmall)
                                        Text(bill.dueDay?.let { "Due on day $it" } ?: "Monthly · No due date",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(money(bill.cents), style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                        item { Text("Saved on this device · USD", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }

    if (showEditor && payments != null) {
        val bills = payments.orEmpty()
        val selected = bills.find { it.id == selectedId }
        PaymentEditor(selected, saving, saveFailed,
            onDismiss = { if (!saving) showEditor = false },
            onSave = { name, cents, day ->
                val payment = Payment(checkNotNull(selectedId), name, cents, day)
                save(bills.upsert(payment))
            },
            onDelete = { save(bills.filterNot { it.id == selected?.id }) },
        )
    }
}

@Composable
private fun SpendingChart(bills: List<Payment>, total: Long) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Spending chart. Monthly total ${money(total)} across ${bills.size} payments." }) {
            val stroke = Stroke(width = 18.dp.toPx())
            val inset = stroke.width / 2
            val diameter = size.width - stroke.width
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = stroke)
            var start = -90f
            if (total > 0) bills.forEachIndexed { index, bill ->
                val sweep = (bill.cents.toDouble() / total * 360).toFloat()
                drawArc(chartColors[index % chartColors.size], start, sweep, false, topLeft, arcSize, style = stroke)
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${bills.size}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(if (bills.size == 1) "monthly bill" else "monthly bills", style = MaterialTheme.typography.bodySmall)
        }
    }
}
