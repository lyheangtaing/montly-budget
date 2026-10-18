package dev.monthly.budget

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class Payment(val id: Long, val name: String, val cents: Long, val dueDay: Int? = null)

internal fun List<Payment>.upsert(payment: Payment): List<Payment> =
    if (any { it.id == payment.id }) map { if (it.id == payment.id) payment else it } else this + payment

// Integer cents avoid floating-point rounding in stored amounts and totals.
internal fun parseCents(input: String): Long? {
    val text = input.trim()
    if (!Regex("[0-9]{1,9}(\\.[0-9]{1,2})?").matches(text)) return null
    val parts = text.split('.')
    val cents = parts[0].toLong() * 100 + (parts.getOrNull(1) ?: "0").padEnd(2, '0').toLong()
    return cents.takeIf { it > 0 }
}

internal fun money(cents: Long) = "$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

/** Implementations use app-private storage and perform disk work off the UI thread. */
interface BudgetStorage {
    suspend fun read(): String?
    suspend fun write(value: String)
}

internal class BudgetStore(private val storage: BudgetStorage) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun load(): List<Payment> {
        val raw = storage.read() ?: return emptyList()
        val saved = json.decodeFromString<SavedBudget>(raw)
        require(saved.version == 1) { "Unsupported budget version" }
        return saved.payments.also(::validate)
    }

    suspend fun save(payments: List<Payment>) {
        validate(payments)
        storage.write(json.encodeToString(SavedBudget(payments = payments)))
    }

    private fun validate(payments: List<Payment>) {
        require(payments.size <= 1000 && payments.map { it.id }.distinct().size == payments.size)
        require(payments.all {
            it.id > 0 && it.id < Long.MAX_VALUE && it.name.isNotBlank() && it.name.length <= 60 &&
                it.cents in 1..99_999_999_999L && (it.dueDay == null || it.dueDay in 1..31)
        }) { "Invalid payment" }
    }

    @Serializable
    private data class SavedBudget(val version: Int = 1, val payments: List<Payment>)
}
