package com.byleth.wereable.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.byleth.wereable.presentation.theme.WereableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    WereableTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            // Hora arriba
            TimeText(
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Juego de gato
            TicTacToeScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
            )
        }
    }
}

@Composable
fun TicTacToeScreen(modifier: Modifier = Modifier) {
    // Estado del tablero
    var board by remember { mutableStateOf(List(9) { null as Char? }) }
    var currentPlayer by remember { mutableStateOf('X') }
    var winner by remember { mutableStateOf<Char?>(null) }
    var isDraw by remember { mutableStateOf(false) }

    fun resetBoard() {
        board = List(9) { null }
        currentPlayer = 'X'
        winner = null
        isDraw = false
    }

    fun checkWinner(b: List<Char?>): Char? {
        val lines = listOf(
            listOf(0, 1, 2),
            listOf(3, 4, 5),
            listOf(6, 7, 8),
            listOf(0, 3, 6),
            listOf(1, 4, 7),
            listOf(2, 5, 8),
            listOf(0, 4, 8),
            listOf(2, 4, 6)
        )
        for (line in lines) {
            val (a, b1, c) = line
            val va = b[a]
            if (va != null && va == b[b1] && va == b[c]) {
                return va
            }
        }
        return null
    }

    fun onCellClick(index: Int) {
        if (board[index] != null || winner != null || isDraw) return

        val newBoard = board.toMutableList()
        newBoard[index] = currentPlayer
        board = newBoard

        val w = checkWinner(newBoard)
        if (w != null) {
            winner = w
        } else if (!newBoard.any { it == null }) {
            isDraw = true
        } else {
            currentPlayer = if (currentPlayer == 'X') 'O' else 'X'
        }
    }

    // Lista con scroll para que siempre puedas llegar al botón
    ScalingLazyColumn(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Texto de estado (único texto arriba del tablero)
        item {
            val statusText = when {
                winner != null -> "Ganó $winner"
                isDraw -> "Empate"
                else -> "Turno: $currentPlayer"
            }

            Text(
                text = statusText,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = when {
                    winner != null -> MaterialTheme.colors.secondary
                    isDraw -> MaterialTheme.colors.onBackground
                    currentPlayer == 'X' -> MaterialTheme.colors.primary
                    else -> MaterialTheme.colors.secondary
                }
            )
        }

        // Tablero 3x3
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (col in 0 until 3) {
                            val index = row * 3 + col
                            CellButton(
                                value = board[index],
                                enabled = (board[index] == null && winner == null && !isDraw),
                                onClick = { onCellClick(index) },
                                modifier = Modifier
                                    .size(44.dp)   // tamaño ajustado para que quepa todo
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Botón de nueva partida: SOLO si ya terminó (ganador o empate)
        if (winner != null || isDraw) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { resetBoard() },
                    modifier = Modifier
                        .width(110.dp)
                        .height(34.dp)
                ) {
                    Text(
                        text = "Nueva partida",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CellButton(
    value: Char?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor: Color
    val contentColor: Color

    when (value) {
        'X' -> {
            // Fondo más claro y texto blanco
            bgColor = MaterialTheme.colors.primary.copy(alpha = 0.4f)
            contentColor = Color.White
        }
        'O' -> {
            bgColor = MaterialTheme.colors.secondary.copy(alpha = 0.4f)
            contentColor = Color.White
        }
        else -> {
            // Casilla vacía: círculo gris claro
            bgColor = MaterialTheme.colors.onBackground.copy(alpha = 0.2f)
            contentColor = MaterialTheme.colors.onBackground
        }
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = bgColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = value?.toString() ?: "",
            fontSize = 20.sp
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}
