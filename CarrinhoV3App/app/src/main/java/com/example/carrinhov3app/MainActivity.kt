package com.example.carrinhov3app

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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import androidx.compose.foundation.layout.height // Certifique-se de que este import está presente
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip

// Simulação de um bluetoothSocket.
var bluetoothSocketOutputStream: OutputStream? = null
// Endereço MAC do ESP32 (substitua pelo endereço correto)
const val ESP32_MAC_ADDRESS = "F8:B3:B7:2C:6E:3A" // Substitua pelo MAC do seu ESP32
//3C:E9:0E:54:AF:B6

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


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelaControleRemoto()
        }
    }
}

@Composable
fun MenuCentralUI(
    statusConexao: String,
    onConectarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onConectarClick) {
            Text("Conectar")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(statusConexao, color = CoresApp.corDoTexto)
    }
}

@Composable
fun TelaControleRemoto() {
    var velocidade by remember { mutableStateOf(0f) }
    var direcao by remember { mutableStateOf(90f) }
    var statusConexao by remember { mutableStateOf("Desconectado") }

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

    val resetarDirecao = false
    val resetarVelocidade = false
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
            direcao = 90f

        }
        if (resetarVelocidade) {
            velocidade = 0f
        }
        enviarComando(velocidade.toInt().toShort(), direcao.toInt().toByte())
    }

    fun conectarBluetooth() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            statusConexao = "Bluetooth não suportado"
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            statusConexao = "Bluetooth desativado"
            return
        }
        val device = bluetoothAdapter.getRemoteDevice(ESP32_MAC_ADDRESS)
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        try {
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            bluetoothSocketOutputStream = socket.outputStream
            statusConexao = "Conectado"
            Log.d("BluetoothConnect", "Conectado ao ESP32")
        } catch (e: IOException) {
            statusConexao = "Falha ao conectar"
            Log.e("BluetoothConnect", "Erro ao conectar: ${e.message}")
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CoresApp.corDeFundo
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Coluna da Esquerda: Controle da Direção (Horizontal) ---
            SliderDirecao(
                value = direcao,
                onValueChange = { novaDirecao -> processarAtualizacaoControle(novaDirecao = novaDirecao) },
                onValueChangeFinished = { processarAtualizacaoControle(novaDirecao = 90f, resetarDirecao = true) },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2f)
            )

            // Chamada para a UI do Menu Central, passando a função de lógica como callback
            MenuCentralUI(
                statusConexao = statusConexao,
                onConectarClick = { conectarBluetooth() }, // A lógica de conexão está agora em uma função separada
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            )

            // --- Coluna da Direita: Controle do Motor (Vertical) ---
            SliderVelocidade(
                value = velocidade,
                onValueChange = { novaVelocidade -> processarAtualizacaoControle(novaVelocidade = novaVelocidade) },
                onValueChangeFinished = { processarAtualizacaoControle(novaVelocidade = 0f, resetarVelocidade = true) },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2f)
            )
        }
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
                    modifier = Modifier
                        .height(50.dp)
                        .background(color = CoresApp.corDaTrilhaInativaSlider)
                )
                    Box(
                        modifier = Modifier
                            .height(50.dp)
                            .background(color = CoresApp.corDaTrilhaInativaSlider)
                    )
            }
        },
    )
}

@Composable
fun SliderDirecao(
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
        Text("Direção: ${value.toInt()}", color = CoresApp.corDoTexto)
        SliderEstiloXL(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..180f,
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
            modifier = Modifier
        )
        SliderEstiloXL(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = -255f..255f,
            modifier = Modifier.fillMaxSize().rotate(-90f) // Garante que o slider ocupe o espaço
        )
    }
}
