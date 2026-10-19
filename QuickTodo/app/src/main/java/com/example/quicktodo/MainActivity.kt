package com.example.quicktodo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import android.os.Build

data class Todo(val id: Long, val title: String, val done: Boolean = false)

/** Stores todos in SharedPreferences, one per line: "id|done|title". */
class TodoStore(context: Context) {
    private val prefs = context.getSharedPreferences("todos", Context.MODE_PRIVATE)

    fun load(): List<Todo> =
        (prefs.getString("items", "") ?: "")
            .lines()
            .mapNotNull { line ->
                val parts = line.split("|", limit = 3)
                if (parts.size < 3) return@mapNotNull null
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                Todo(id, parts[2], parts[1] == "1")
            }

    fun save(items: List<Todo>) {
        val text = items.joinToString("\n") { "${it.id}|${if (it.done) 1 else 0}|${it.title.replace("\n", " ")}" }
        prefs.edit().putString("items", text).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TodoScreen()
                }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun TodoScreen() {
    val context = LocalContext.current
    val store = remember { TodoStore(context) }
    val todos = remember { mutableStateListOf<Todo>().apply { addAll(store.load()) } }
    var input by remember { mutableStateOf("") }

    fun persist() = store.save(todos.toList())
    fun add() {
        val title = input.trim()
        if (title.isEmpty()) return
        todos.add(0, Todo(System.currentTimeMillis(), title))
        input = ""
        persist()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text("QuickTodo", style = MaterialTheme.typography.headlineMedium)
        val remaining = todos.count { !it.done }
        Text(
            if (todos.isEmpty()) "Nothing here yet" else "$remaining of ${todos.size} remaining",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a task") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() })
            )
            Spacer(Modifier.padding(4.dp))
            Button(onClick = { add() }) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(todos, key = { it.id }) { todo ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = todo.done,
                            onCheckedChange = { checked ->
                                val i = todos.indexOfFirst { it.id == todo.id }
                                if (i >= 0) {
                                    todos[i] = todos[i].copy(done = checked)
                                    persist()
                                }
                            }
                        )
                        Text(
                            todo.title,
                            modifier = Modifier.weight(1f),
                            textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                            color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = {
                            todos.removeAll { it.id == todo.id }
                            persist()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }

        if (todos.any { it.done }) {
            TextButton(
                onClick = {
                    todos.removeAll { it.done }
                    persist()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Clear completed")
            }
        }
    }
}
