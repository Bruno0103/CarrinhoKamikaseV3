package com.example.carrinhov3app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import java.util.UUID
import androidx.compose.foundation.layout.height // Certifique-se de que este import está presente
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip

/**
 * Objeto para centralizar as cores da aplicação, melhorando a consistência e legibilidade.
 */
object CoresApp {
    val corDeFundo = Color(22, 22, 22)
    val corDoTexto = Color.White
    val corDoPolegarSlider = Color.Red
    val corDaTrilhaAtivaSlider = Color.White
    val corDaTrilhaInativaSlider = Color.Gray
}

// Simulação de um bluetoothSocket.
var bluetoothSocketOutputStream: OutputStream? = null


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelaControleRemoto()
        }
    }
}

@SuppressLint("MissingPermission") // Cuidado: Adicionado para simplicidade. Peça permissões em um app real.
@Composable
fun TelaControleRemoto() {
    var velocidade by remember { mutableStateOf(0f) }
    var statusConexao by remember { mutableStateOf("Desconectado") }

    var minDirecao by remember { mutableStateOf("0") }
    var maxDirecao by remember { mutableStateOf("180") }
    var centroDirecao by remember { mutableStateOf("90") }
    var direcao by remember { mutableStateOf(centroDirecao.toFloatOrNull() ?: 90f) }

    // Estado para o endereço MAC selecionado e a lista de dispositivos
    var macAddressSelecionado by remember { mutableStateOf<String?>(null) }
    var dispositivosPareados by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // Carrega os dispositivos pareados uma vez quando o Composable é iniciado
    LaunchedEffect(Unit) {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            return@LaunchedEffect
        }
        dispositivosPareados = bluetoothAdapter?.bondedDevices?.map { device ->
            Pair(device.name, device.address)
        } ?: emptyList()
    }

    fun enviarComando(velocidadeAtual: Short, direcaoAtual: Byte) {
        val buffer = byteArrayOf(
            (velocidadeAtual.toInt() shr 8).toByte(), // byte alto
            velocidadeAtual.toByte(),                 // byte baixo
            direcaoAtual
        )
        Log.d("BluetoothSend", "Enviando: vel=${velocidadeAtual}, dir=${direcaoAtual}")
        try {
            bluetoothSocketOutputStream?.write(buffer)
        } catch (e: IOException) {
            Log.e("BluetoothSend", "Erro ao enviar dados: ${e.message}")
        }
    }

    fun processarAtualizacaoControle(
        novaVelocidade: Float? = null,
        novaDirecao: Float? = null,
        resetarDirecao: Boolean = false,
        resetarVelocidade: Boolean = false
    ) {
        if (novaVelocidade != null) {
            velocidade = novaVelocidade
        }
        if (novaDirecao != null) {
            direcao = novaDirecao
        }
        if (resetarDirecao) {
            direcao = centroDirecao.toFloatOrNull() ?: 90f

        }
        if (resetarVelocidade) {
            velocidade = 0f
        }
        enviarComando(velocidade.toInt().toShort(), direcao.toInt().toByte())
    }

    val coroutineScope = rememberCoroutineScope()
    fun conectarBluetooth() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            statusConexao = "BT não suportado"
            return
        }
        if (macAddressSelecionado == null) {
            statusConexao = "Selecione um dispositivo"
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            statusConexao = "BT desativado"
            return
        }
        val device = bluetoothAdapter.getRemoteDevice(macAddressSelecionado)
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID padrão para SPP

        coroutineScope.launch(Dispatchers.IO) { // Executa a conexão em uma thread de background
            try {
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                withContext(Dispatchers.Main) { statusConexao = "Conectando..." }

                socket.connect() // Esta é a chamada de bloqueio

                // Se a conexão for bem-sucedida, atualize a UI na thread principal
                withContext(Dispatchers.Main) {
                    bluetoothSocketOutputStream = socket.outputStream
                    statusConexao = "Conectado"
                    Log.d("BluetoothConnect", "Conectado ao ESP32")
                }
            } catch (e: IOException) {
                // Se ocorrer um erro, atualize a UI na thread principal
                withContext(Dispatchers.Main) {
                    statusConexao = "Falha ao conectar"
                    Log.e("BluetoothConnect", "Erro ao conectar: ${e.message}", e)
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CoresApp.corDeFundo
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(CoresApp.corDeFundo),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Coluna da Esquerda: Controle da Direção (Horizontal) ---
            SliderDirecao(
                value = direcao,
                min = minDirecao.toFloatOrNull() ?: 0f,
                max = maxDirecao.toFloatOrNull() ?: 180f,
                onValueChange = { novaDirecao -> processarAtualizacaoControle(novaDirecao = novaDirecao) },
                onValueChangeFinished = { processarAtualizacaoControle(resetarDirecao = true) },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            )

            // Chamada para a UI do Menu Central, passando a função de lógica como callback
            MenuCentralUI(
                statusConexao = statusConexao,
                minDirecao = minDirecao, onMinDirecaoChange = { minDirecao = it },
                maxDirecao = maxDirecao, onMaxDirecaoChange = { maxDirecao = it },
                centroDirecao = centroDirecao, onCentroDirecaoChange = { centroDirecao = it },
                dispositivosPareados = dispositivosPareados,
                macAddressSelecionado = macAddressSelecionado,
                onMacAddressChange = { macAddressSelecionado = it },
                onConectarClick = { conectarBluetooth() }, // A lógica de conexão está agora em uma função separada
                modifier = Modifier
                    .fillMaxSize() // Ocupa todo o espaço do Box pai
                    .weight(1f)
                    .padding(16.dp) 
            )

            // --- Coluna da Direita: Controle do Motor (Vertical) ---
            SliderVelocidade(
                value = velocidade,
                onValueChange = { novaVelocidade -> processarAtualizacaoControle(novaVelocidade = novaVelocidade) },
                onValueChangeFinished = { processarAtualizacaoControle(novaVelocidade = 0f, resetarVelocidade = true) },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun MenuCentralUI(
    statusConexao: String,
    minDirecao: String,
    onMinDirecaoChange: (String) -> Unit,
    maxDirecao: String,
    onMaxDirecaoChange: (String) -> Unit,
    centroDirecao: String,
    onCentroDirecaoChange: (String) -> Unit,
    dispositivosPareados: List<Pair<String, String>>,
    macAddressSelecionado: String?,
    onMacAddressChange: (String) -> Unit,
    onConectarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputColors = TextFieldDefaults.colors(
        focusedTextColor = CoresApp.corDoTexto,
        unfocusedTextColor = CoresApp.corDoTexto,
        cursorColor = CoresApp.corDoPolegarSlider,
        focusedContainerColor = Color.DarkGray,
        unfocusedContainerColor = Color.DarkGray,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

    var menuExpandido by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(text = "Menu Central", color = CoresApp.corDoTexto)

        TextField(
            value = centroDirecao,
            onValueChange = onCentroDirecaoChange,
            label = { Text("Centro") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = inputColors
        )
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Top
        ){ TextField(
            value = maxDirecao,
            onValueChange = onMaxDirecaoChange,
            label = { Text("Máximo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = inputColors,
            modifier = Modifier.weight(1f)
        )

            TextField(
                value = minDirecao,
                onValueChange = onMinDirecaoChange,
                label = { Text("Mínimo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = inputColors,
                modifier = Modifier.weight(1f)
            ) }


        // --- Menu Suspenso para Dispositivos Bluetooth ---
        Box {
            OutlinedButton(onClick = { menuExpandido = true }) {
                val nomeDispositivo = dispositivosPareados.find { it.second == macAddressSelecionado }?.first
                Text(nomeDispositivo ?: "Selecione um Dispositivo", color = CoresApp.corDoTexto)
            }
            DropdownMenu(
                expanded = menuExpandido,
                onDismissRequest = { menuExpandido = false }
            ) {
                dispositivosPareados.forEach { (nome, endereco) ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(nome ?: "Dispositivo sem nome")
                                Text(endereco, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        onClick = {
                            onMacAddressChange(endereco)
                            menuExpandido = false
                        }
                    )
                }
            }
        }
        // --- Fim do Menu Suspenso ---

        Button(onClick = onConectarClick, enabled = macAddressSelecionado != null) {
            Text("Conectar")
        }
        Text(statusConexao, color = CoresApp.corDoTexto)
    }
}

/**
 * Um Composable de Slider com um estilo "XL" pré-definido, usando a API Material 3 mais recente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderEstiloXL(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Define as cores uma vez para reutilização e consistência
    val sliderColors = SliderDefaults.colors(
        thumbColor = CoresApp.corDoPolegarSlider,
        activeTrackColor = CoresApp.corDaTrilhaAtivaSlider,
        inactiveTrackColor = CoresApp.corDaTrilhaInativaSlider
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps,
        modifier = modifier,
        interactionSource = interactionSource,
        colors = sliderColors, // Aplica as cores ao componente Slider principal
        thumb = {
            // Para customizar o tamanho do polegar, aplicamos um Modifier.size()
            // ao SliderDefaults.Thumb.
            SliderDefaults.Thumb(
                interactionSource = interactionSource, // ou 'it' se fornecido pela lambda
                //thumbSize = DpSize(30.dp, 30.dp)
                modifier = Modifier
                        .width(20.dp)
                        .height(60.dp)
                )
        },
        track = { sliderState ->
            // Customiza a trilha para ser mais alta e com cantos arredondados
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(CoresApp.corDaTrilhaInativaSlider)
                ) {
                    // Representa a parte ativa da trilha
                    val activeTrackWidth = sliderState.valueRange.let {
                        val range = it.endInclusive - it.start
                        if (range == 0f) 0f else (sliderState.value - it.start) / range
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(activeTrackWidth)
                            .background(CoresApp.corDaTrilhaAtivaSlider)
                    )
                }
            }
        },
    )
}

@Composable
fun SliderDirecao(
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text("Direção: ${value.toInt()}", color = CoresApp.corDoTexto)
        SliderEstiloXL(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = min..max,
            modifier = Modifier.rotate(180f)
        )
    }
}

@Composable
fun SliderVelocidade(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            text = "Velocidade: ${value.toInt()}",
            color = CoresApp.corDoTexto,
            modifier = Modifier.padding(bottom = 80.dp)
        )
        SliderEstiloXL(
            value = value,
            onValueChange = { newValue ->
                // Chama a função de callback com o valor calculado.
                onValueChange(newValue)
            },
            onValueChangeFinished = onValueChangeFinished,
            // O slider visual agora trabalha na faixa de -255 a 255.
            valueRange = -255f..255f,
            modifier = Modifier
                .fillMaxHeight()
                .width(250.dp)
                .rotate(-90f) // Garante que o slider ocupe o espaço
        )
    }
}
