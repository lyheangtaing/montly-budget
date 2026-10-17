package dev.monthly.budget

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val storage = AndroidStorage(getSharedPreferences("monthly-budget", MODE_PRIVATE))
        setContent { BudgetApp(storage) }
    }
}

private class AndroidStorage(private val preferences: SharedPreferences) : BudgetStorage {
    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        preferences.getString("payments", null)
    }

    override suspend fun write(value: String): Unit = withContext(Dispatchers.IO) {
        check(preferences.edit().putString("payments", value).commit()) { "Could not save budget" }
    }
}
