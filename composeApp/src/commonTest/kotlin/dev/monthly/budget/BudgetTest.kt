package dev.monthly.budget

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BudgetTest {
    @Test
    fun amountsUseExactCents() {
        assertEquals(22000L, parseCents("220"))
        assertEquals(610L, parseCents(" 6.10 "))
        assertEquals(1L, parseCents("0.01"))
        assertEquals("$226.00", money(22600))
        assertEquals("$0.01", money(1))
    }

    @Test
    fun invalidAmountsCannotBeSaved() {
        listOf("", "0", "-1", "1.001", "NaN", "1e3", "1,000", "1000000000").forEach {
            assertNull(parseCents(it), it)
        }
    }

    @Test
    fun firstLaunchIncludesTheTwoBills() = runTest {
        val bills = BudgetStore(MemoryStorage()).load()
        assertEquals(22600L, bills.sumOf { it.cents })
        assertEquals(Payment(1, "Rent", 22000, 25), bills[0])
        assertEquals(Payment(2, "Cellcard SIM", 600), bills[1])
    }

    @Test
    fun localStoragePreservesNamesAndOptionalDueDates() = runTest {
        val storage = MemoryStorage()
        val bills = listOf(Payment(7, "ទឹក · \"Water\"", 1234, 31))
        BudgetStore(storage).save(bills)
        assertEquals(bills, BudgetStore(storage).load())
    }

    @Test
    fun deletingEveryBillDoesNotRestoreTheDefaults() = runTest {
        val storage = MemoryStorage()
        BudgetStore(storage).save(emptyList())
        assertEquals(emptyList(), BudgetStore(storage).load())
    }

    @Test
    fun resavingARestoredDraftUpdatesTheSamePayment() {
        val original = listOf(Payment(1, "Rent", 22000, 25))
        val draft = Payment(2, "Water", 500)
        val committed = original.upsert(draft)
        val restoredAndEdited = committed.upsert(draft.copy(cents = 700))
        assertEquals(2, restoredAndEdited.size)
        assertEquals(700L, restoredAndEdited.last().cents)
        assertEquals(original.first(), restoredAndEdited.first())
    }

    @Test
    fun invalidRecordsAreRejected() = runTest {
        val store = BudgetStore(MemoryStorage())
        for (bill in listOf(
            Payment(1, " ", 100), Payment(1, "Rent", -1),
            Payment(1, "Rent", 100, 0), Payment(1, "Rent", 100, 32),
        )) assertFailsWith<IllegalArgumentException> { store.save(listOf(bill)) }
        assertFailsWith<IllegalArgumentException> {
            store.save(listOf(Payment(1, "A", 100), Payment(1, "B", 100)))
        }
        assertFailsWith<IllegalArgumentException> {
            store.save(List(1001) { Payment(it.toLong() + 1, "Bill", 100) })
        }
    }

    @Test
    fun damagedDataIsNotSilentlyReplacedWithSampleBills() = runTest {
        val storage = MemoryStorage("not json")
        assertFailsWith<Exception> { BudgetStore(storage).load() }
        assertEquals("not json", storage.value)
    }

    private class MemoryStorage(var value: String? = null) : BudgetStorage {
        override suspend fun read() = value
        override suspend fun write(value: String) { this.value = value }
    }
}
