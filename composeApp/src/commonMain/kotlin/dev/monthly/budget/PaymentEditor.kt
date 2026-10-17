package dev.monthly.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun PaymentEditor(
    payment: Payment?, saving: Boolean, saveFailed: Boolean,
    onDismiss: () -> Unit, onSave: (String, Long, Int?) -> Unit, onDelete: () -> Unit,
) {
    var name by rememberSaveable(payment?.id) { mutableStateOf(payment?.name.orEmpty()) }
    var amount by rememberSaveable(payment?.id) { mutableStateOf(payment?.let { money(it.cents).drop(1) }.orEmpty()) }
    var day by rememberSaveable(payment?.id) { mutableStateOf(payment?.dueDay?.toString().orEmpty()) }
    val cents = parseCents(amount)
    val dueDay = day.toIntOrNull()
    val validDay = day.isEmpty() || (Regex("[0-9]{1,2}").matches(day) && dueDay != null && dueDay in 1..31)
    val valid = name.isNotBlank() && name.trim().length <= 60 && cents != null && validDay

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (payment == null) "Add payment" else "Edit payment") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), enabled = !saving,
                    label = { Text("Name") }, singleLine = true, isError = name.trim().length > 60)
                OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), enabled = !saving,
                    label = { Text("Monthly amount (USD)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amount.isNotEmpty() && cents == null,
                    supportingText = { if (amount.isNotEmpty() && cents == null) Text("Enter a positive amount with up to 2 decimals.") })
                OutlinedTextField(day, { day = it }, Modifier.fillMaxWidth(), enabled = !saving,
                    label = { Text("Due day (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !validDay, supportingText = { Text("Day of the month, 1–31") })
                if (saveFailed) Text("Couldn’t save. Your changes are still here; please try again.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { cents?.let { onSave(name.trim(), it, dueDay) } }, enabled = valid && !saving) {
                Text(if (saving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            if (payment != null) TextButton(onClick = onDelete, enabled = !saving) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        },
    )
}
