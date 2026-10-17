@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.monthly.budget

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

fun MainViewController() = ComposeUIViewController { BudgetApp(IosStorage) }

private object IosStorage : BudgetStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun read(): String? = withContext(Dispatchers.Default) {
        defaults.stringForKey("monthly-budget.payments")
    }

    override suspend fun write(value: String): Unit = withContext(Dispatchers.Default) {
        defaults.setObject(value, forKey = "monthly-budget.payments")
    }
}
