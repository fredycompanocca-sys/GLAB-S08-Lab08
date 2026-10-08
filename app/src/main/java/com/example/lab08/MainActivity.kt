package com.example.lab08

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import kotlinx.coroutines.launch
import com.example.lab08.ui.theme.Lab08Theme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                val taskDao = db.taskDao()
                val viewModel = TaskViewModel(taskDao)

                TaskScreen(viewModel)
            }
        }
    }
}

@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var newTaskDescription by remember { mutableStateOf("") }
    var editingTaskId by remember { mutableStateOf<Int?>(null) } // null = no se edita ninguna
    var editingText by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf("Todas") } // "Todas", "Completadas", "Pendientes"
    var busqueda by remember { mutableStateOf("") }
    var orden by remember { mutableStateOf("Fecha") } // "Fecha", "Nombre", "Estado"
    var categoriaFiltro by remember { mutableStateOf("Todas") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = newTaskDescription,
            onValueChange = { newTaskDescription = it },
            label = { Text("Nueva tarea") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (newTaskDescription.isNotEmpty()) {
                    viewModel.addTask(newTaskDescription)
                    newTaskDescription = ""
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Agregar tarea")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { filtro = "Todas" }) { Text("Todas") }
            Button(onClick = { filtro = "Completadas" }) { Text("Completadas") }
            Button(onClick = { filtro = "Pendientes" }) { Text("Pendientes") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Buscar tarea") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { orden = "Fecha" }) { Text("Por fecha") }
            Button(onClick = { orden = "Nombre" }) { Text("Por nombre") }
            Button(onClick = { orden = "Estado" }) { Text("Por estado") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Todas", "General", "Trabajo", "Personal", "Urgente").forEach { cat ->
                Button(onClick = { categoriaFiltro = cat }) { Text(cat) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        val tareasFiltradas = when (filtro) {
            "Completadas" -> tasks.filter { it.isCompleted }
            "Pendientes" -> tasks.filter { !it.isCompleted }
            else -> tasks
        }
            .filter { it.description.contains(busqueda, ignoreCase = true) }
            .filter { categoriaFiltro == "Todas" || it.category == categoriaFiltro }
            .let { lista ->
                when (orden) {
                    "Nombre" -> lista.sortedBy { it.description }
                    "Estado" -> lista.sortedBy { it.isCompleted }
                    else -> lista.sortedByDescending { it.createdAt }
                }
            }
        tareasFiltradas.forEach { task ->  // <-- esta línea REEMPLAZA tu "tasks.forEach { task ->"
            if (editingTaskId == task.id) {
                // Modo edición: muestra un TextField con el botón de guardar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextField(
                        value = editingText,
                        onValueChange = { editingText = it },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        viewModel.editTask(task, editingText)
                        editingTaskId = null
                    }) {
                        Text("Guardar")
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(text = "${task.description} [${task.priority} | ${task.category}]")
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(onClick = { viewModel.toggleTaskCompletion(task) }) {
                            Text(if (task.isCompleted) "Completada" else "Pendiente")
                        }
                        Button(onClick = {
                            val siguiente = when (task.priority) {
                                "Alta" -> "Media"
                                "Media" -> "Baja"
                                else -> "Alta"
                            }
                            viewModel.changePriority(task, siguiente)
                        }) {
                            Text("Prioridad")
                        }
                        Button(onClick = {
                            val siguienteCategoria = when (task.category) {
                                "General" -> "Trabajo"
                                "Trabajo" -> "Personal"
                                "Personal" -> "Urgente"
                                else -> "General"
                            }
                            viewModel.changeCategory(task, siguienteCategoria)
                        }) {
                            Text("Categoria")
                        }
                        Button(onClick = {
                            editingTaskId = task.id
                            editingText = task.description
                        }) {
                            Text("Editar")
                        }
                        Button(onClick = { viewModel.deleteTask(task) }) {
                            Text("Eliminar")

                        }
                    }
                }
            }
        }

        Button(
            onClick = { coroutineScope.launch { viewModel.deleteAllTasks() } },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Eliminar todas las tareas")
        }
    }
}


