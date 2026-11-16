package com.example.rgbcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontroller.domain.RemoteAction
import com.example.rgbcontroller.domain.RgbRemotePreset
import com.example.rgbcontroller.domain.RgbRemotePresets
import com.example.rgbcontroller.hardware.IrStatus
import com.example.rgbcontroller.hardware.IrTransmitter
import com.example.rgbcontroller.hardware.NecEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transmitter = IrTransmitter(applicationContext)
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModel.factory(transmitter))
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFB6C4FF), secondary = Color(0xFF71E2D0),
                background = Color(0xFF101114), surface = Color(0xFF1A1B20)
            )) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ControllerScreen(vm)
                }
            }
        }
    }
}

class MainViewModel(private val ir: IrTransmitter) : ViewModel() {
    var status by mutableStateOf<IrStatus?>(null); private set
    var message by mutableStateOf("Comprueba el emisor antes de probar los códigos."); private set
    var frequency by mutableStateOf("38000"); private set
    var pattern by mutableStateOf(""); private set
    var busy by mutableStateOf(false); private set
    var selectedPreset by mutableStateOf(RgbRemotePresets.all.first()); private set

    fun selectPreset(preset: RgbRemotePreset) { selectedPreset = preset }
    fun onFrequencyChanged(value: String) { frequency = value.filter(Char::isDigit).take(6) }
    fun onPatternChanged(value: String) { pattern = value.filter { it.isDigit() || it == ',' || it.isWhitespace() }.take(400) }
    fun hasCode(action: RemoteAction) = selectedPreset.commands.containsKey(action)

    fun diagnose() {
        status = ir.diagnose()
        message = when (val s = status) {
            is IrStatus.Ready -> if (s.ranges.isEmpty()) "Emisor detectado; Android no informó rangos de frecuencia."
                else "Emisor detectado. Rangos: ${s.ranges.joinToString { "${it.first}–${it.last} Hz" }}"
            IrStatus.NoEmitter -> "Este dispositivo no informa un emisor infrarrojo compatible."
            IrStatus.ServiceUnavailable -> "El servicio Consumer IR no está disponible en este dispositivo."
            IrStatus.UnsupportedApi -> "La versión de Android no admite ConsumerIrManager."
            is IrStatus.Error -> s.message
            null -> "Diagnóstico pendiente."
        }
    }

    suspend fun sendAction(action: RemoteAction) {
        if (status !is IrStatus.Ready) {
            message = "Primero pulsa Comprobar emisor IR."
            return
        }
        val command = selectedPreset.commands[action]
        if (command == null) {
            message = "Este perfil no tiene un código de referencia para ${action.label}."
            return
        }
        val raw = NecEncoder.encode(selectedPreset.address, command, selectedPreset.extendedAddress)
        transmit(NecEncoder.CARRIER_HZ, raw, "${action.label} · ${selectedPreset.name}")
    }

    suspend fun sendRaw() {
        if (status !is IrStatus.Ready) {
            message = "Primero pulsa Comprobar emisor IR."
            return
        }
        val hz = frequency.toIntOrNull()
        val tokens = pattern.split(',', ' ', '\n', '\t').filter(String::isNotBlank)
        val values = tokens.mapNotNull(String::toIntOrNull)
        if (hz == null || tokens.isEmpty() || values.size != tokens.size) {
            message = "Revisa la frecuencia y escribe duraciones numéricas separadas por comas."
            return
        }
        transmit(hz, values.toIntArray(), "Patrón manual")
    }

    private suspend fun transmit(hz: Int, raw: IntArray, label: String) {
        busy = true
        try {
            val result = withContext(Dispatchers.IO) { ir.transmit(hz, raw) }
            message = result.fold(
                { "$label transmitido por el teléfono. No se puede confirmar si la lámpara lo recibió." },
                { "No se pudo transmitir: ${it.message ?: "error del emisor IR"}" }
            )
        } catch (error: Exception) {
            message = "No se pudo transmitir: ${error.message ?: "error inesperado"}"
        } finally {
            busy = false
        }
    }

    companion object {
        fun factory(ir: IrTransmitter) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(ir) as T
        }
    }
}

@Composable
private fun ControllerScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var presetMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.diagnose() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("RGB Controller", style = MaterialTheme.typography.headlineMedium)
        Text("Control infrarrojo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = selectedTab == 0, onClick = { selectedTab = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Colores") }
            SegmentedButton(selected = selectedTab == 1, onClick = { selectedTab = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Prueba manual") }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Estado del emisor IR", style = MaterialTheme.typography.titleMedium)
                Text(vm.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = vm::diagnose, modifier = Modifier.fillMaxWidth()) { Text("Comprobar emisor IR") }
            }
        }

        if (selectedTab == 0) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Perfil de códigos candidato", style = MaterialTheme.typography.titleMedium)
                    Box {
                        OutlinedButton(onClick = { presetMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(vm.selectedPreset.name) }
                        DropdownMenu(expanded = presetMenuOpen, onDismissRequest = { presetMenuOpen = false }) {
                            RgbRemotePresets.all.forEach { preset ->
                                DropdownMenuItem(text = { Text(preset.name) }, onClick = {
                                    vm.selectPreset(preset)
                                    presetMenuOpen = false
                                })
                            }
                        }
                    }
                    Text("Se conservaron los códigos del perfil EF00 que ya responde. Los tonos añadidos son comandos candidatos de la misma dirección; el color real puede variar según la lámpara.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(vm.selectedPreset.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Receptor NEC · dirección ${if (vm.selectedPreset.extendedAddress) "%02X%02X".format(vm.selectedPreset.address and 0xFF, (vm.selectedPreset.address shr 8) and 0xFF) else "00/FF"} · 38 kHz",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (vm.hasCode(RemoteAction.POWER_TOGGLE)) {
                        Button(onClick = { scope.launch { vm.sendAction(RemoteAction.POWER_TOGGLE) } }, enabled = !vm.busy && vm.status is IrStatus.Ready, modifier = Modifier.fillMaxWidth()) {
                            Text("⏻  Encender / apagar")
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(RemoteAction.POWER_ON, RemoteAction.POWER_OFF).forEach { action ->
                                OutlinedButton(onClick = { scope.launch { vm.sendAction(action) } }, enabled = !vm.busy && vm.hasCode(action) && vm.status is IrStatus.Ready, modifier = Modifier.weight(1f)) {
                                    Text(action.label)
                                }
                            }
                        }
                    }

                    val colorButtons = listOf(
                        Triple(RemoteAction.RED, Color(0xFFD94343), "Rojo"),
                        Triple(RemoteAction.REDDISH, Color(0xFFF05238), "Coral"),
                        Triple(RemoteAction.ORANGE, Color(0xFFFF7D22), "Naranja"),
                        Triple(RemoteAction.YELLOWISH, Color(0xFFFFC247), "Ámbar"),
                        Triple(RemoteAction.YELLOW, Color(0xFFE2CE24), "Amarillo"),
                        Triple(RemoteAction.YELLOW_ALT, Color(0xFFD8C52B), "Amar. alt"),
                        Triple(RemoteAction.GREEN, Color(0xFF38A66B), "Verde"),
                        Triple(RemoteAction.GREENISH, Color(0xFF82C84A), "Lima"),
                        Triple(RemoteAction.LIGHT_GREEN, Color(0xFF91C94D), "Verde claro"),
                        Triple(RemoteAction.TURQUOISE, Color(0xFF24B29E), "Turquesa"),
                        Triple(RemoteAction.CYAN, Color(0xFF22AEB5), "Cian"),
                        Triple(RemoteAction.AQUA, Color(0xFF339FD2), "Aqua"),
                        Triple(RemoteAction.BLUE, Color(0xFF4169E1), "Azul"),
                        Triple(RemoteAction.DEEPBLUE, Color(0xFF30428F), "Az. prof."),
                        Triple(RemoteAction.PURPLE, Color(0xFF8B55C7), "Morado"),
                        Triple(RemoteAction.MAGENTA, Color(0xFFD83C9A), "Magenta"),
                        Triple(RemoteAction.PINK, Color(0xFFEF5680), "Rosa"),
                        Triple(RemoteAction.PINK_ALT, Color(0xFFFF79AB), "Rosa alt."),
                        Triple(RemoteAction.TANGERINE, Color(0xFFF07827), "Mandarina"),
                        Triple(RemoteAction.SKY, Color(0xFF55A9E8), "Celeste"),
                        Triple(RemoteAction.ROSE, Color(0xFFE97CA6), "Rosa claro"),
                        Triple(RemoteAction.WHITE, Color(0xFFBFC5D2), "Blanco")
                    )
                    colorButtons.filter { vm.hasCode(it.first) }.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (action, tint, shortLabel) ->
                                Button(
                                    onClick = { scope.launch { vm.sendAction(action) } },
                                    enabled = !vm.busy && vm.hasCode(action) && vm.status is IrStatus.Ready,
                                    colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 7.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text(shortLabel, style = MaterialTheme.typography.labelSmall, maxLines = 2) }
                            }
                        }
                    }

                    if (vm.hasCode(RemoteAction.BRIGHTER) || vm.hasCode(RemoteAction.DARKER)) {
                        Text("Brillo", style = MaterialTheme.typography.titleSmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(RemoteAction.BRIGHTER, RemoteAction.DARKER).forEach { action ->
                                OutlinedButton(onClick = { scope.launch { vm.sendAction(action) } }, enabled = !vm.busy && vm.hasCode(action) && vm.status is IrStatus.Ready, modifier = Modifier.weight(1f)) {
                                    Text(action.label)
                                }
                            }
                        }
                    }

                    Text("Efectos", style = MaterialTheme.typography.titleSmall)
                    listOf(RemoteAction.FLASH, RemoteAction.STROBE, RemoteAction.FADE, RemoteAction.SMOOTH)
                        .filter { vm.hasCode(it) }.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { action ->
                                    OutlinedButton(
                                        onClick = { scope.launch { vm.sendAction(action) } },
                                        enabled = !vm.busy && vm.status is IrStatus.Ready,
                                        modifier = Modifier.weight(1f)
                                    ) { Text(action.label) }
                                }
                            }
                        }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Transmitir patrón manual", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = vm.frequency, onValueChange = vm::onFrequencyChanged, label = { Text("Frecuencia portadora (Hz)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = vm.pattern, onValueChange = vm::onPatternChanged, label = { Text("Marca, espacio, … (µs)") }, placeholder = { Text("Duraciones medidas separadas por comas") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Text("Empieza con una marca y alterna marca/espacio. Usa duraciones medidas de tu control original; el patrón debe durar menos de 2 s.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { scope.launch { vm.sendRaw() } }, enabled = !vm.busy && vm.status is IrStatus.Ready && vm.pattern.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (vm.busy) "Transmitiendo…" else "Transmitir patrón") }
                }
            }
        }
        Text("El teléfono solo transmite IR; no puede aprender ni confirmar la recepción de la lámpara.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
