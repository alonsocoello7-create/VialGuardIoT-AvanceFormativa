package cl.inacap.vialguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.inacap.vialguard.ui.theme.VialGuardIoTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VialGuardIoTTheme { AvanceApp() } }
    }
}

private data class StructureDemo(val name: String, val route: String, val type: String, val status: String)
private val structures = listOf(
    StructureDemo("Puente de demostración", "Maqueta del laboratorio", "Puente", "Habilitado"),
    StructureDemo("Carretera de demostración", "Ejemplo académico", "Carretera", "Inspección"),
    StructureDemo("Acceso de demostración", "Ejemplo académico", "Acceso", "Sin datos")
)

@Composable
private fun AvanceApp(vm: AvanceViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    var page by rememberSaveable(s.currentUser) { mutableStateOf("Inicio") }
    var selected by rememberSaveable(s.currentUser) { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Todas") }
    Scaffold(bottomBar = {
        if (s.currentUser != null) NavigationBar {
            listOf("Inicio", "Estado").forEach { tab ->
                NavigationBarItem(selected = page == tab, onClick = { page = tab },
                    icon = { Text(if (tab == "Inicio") "⌂" else "≡") }, label = { Text(tab) })
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("VialGuard IoT", style = MaterialTheme.typography.headlineMedium)
            Text("Avance formativo VialGuard", style = MaterialTheme.typography.titleMedium)
            if (s.currentUser == null) {
                Text(if (s.accountExists) "Accede con tu cuenta local protegida." else "Primera apertura: crea tu cuenta local con una contraseña de al menos 10 caracteres, letras y números.")
                OutlinedTextField(value = s.username, onValueChange = vm::username, label = { Text("Usuario de demostración") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = s.password, onValueChange = vm::password, label = { Text("Contraseña") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Button(onClick = vm::login, enabled = !s.busy) {
                    Text(if (s.accountExists) "Iniciar sesión" else "Crear cuenta local",
                        color = MaterialTheme.colorScheme.onPrimary)
                }
                Text("Credenciales protegidas localmente. Firebase queda pendiente de integración.")
            } else {
                Text("Sesión: ${s.currentUser}")
                TextButton(onClick = vm::logout, enabled = !s.busy) { Text("Cerrar sesión") }
                Text("Datos de demostración. No son mediciones de un puente real.")
                when (page) {
                    "Detalle" -> {
                        TextButton(onClick = { page = "Estado" }) { Text("Volver a Estado") }
                        Text(structures[selected].name, style = MaterialTheme.typography.headlineSmall)
                        Text(structures[selected].route)
                        Text("Estado de ejemplo: ${structures[selected].status}")
                        if (selected == 0) {
                            Text("Distancia de ejemplo: 100 cm")
                            Text("Inclinación de ejemplo: 0 grados")
                            Text("Firebase: bibliotecas agregadas. Conexión pendiente de configuración.")
                            Text("Estas lecturas son de demostración y todavía no provienen del ESP32.")
                        } else Text("Detalle de ejemplo. Comunicación IoT pendiente.")
                    }
                    else -> {
                        Text(if (page == "Inicio") "Monitoreo de estructuras viales" else "Estado de estructuras",
                            style = MaterialTheme.typography.headlineSmall)
                        Text("Consulta la maqueta y revisa sus lecturas de prueba.")
                        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Buscar estructura") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (page == "Estado") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Todas", "Puente", "Carretera").forEach { type ->
                                FilterChip(selected = filter == type, onClick = { filter = type }, label = { Text(type) })
                            }
                        }
                        Text("Ejemplos: 1 habilitado, 1 inspección, 1 sin datos")
                        val visible = structures.withIndex().filter {
                            (it.value.name.contains(query, ignoreCase = true) || it.value.route.contains(query, ignoreCase = true)) &&
                                (page == "Inicio" || filter == "Todas" || it.value.type == filter)
                        }
                        if (visible.isEmpty()) Text("No hay estructuras con ese filtro.")
                        visible.forEach { (index, item) ->
                            OutlinedCard(Modifier.fillMaxWidth().clickable { selected = index; page = "Detalle" }) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${item.type} · ${item.route}")
                                    Text("Estado de ejemplo: ${item.status}")
                                }
                            }
                        }
                    }
                }
            }
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (s.message.isNotBlank()) Text(s.message)
        }
    }
}
