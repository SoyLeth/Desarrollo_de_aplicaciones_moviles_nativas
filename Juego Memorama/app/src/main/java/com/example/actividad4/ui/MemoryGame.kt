// === MemoryGame.kt ===

package com.example.actividad4.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.actividad4.R

// ✅ Bluetooth
import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat
import com.example.actividad4.bt.BtSession
import com.example.actividad4.bt.BluetoothService

// ✅ XML de partida
import com.example.actividad4.storage.MatchHeader
import com.example.actividad4.storage.MatchXml
import java.io.File

// Corrutinas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ===== Modelo / Config =====
enum class Player { A, B }
enum class GameMode { PVP, AI, BT }

data class BoardSpec(val cols: Int, val rows: Int) { val pairs: Int get() = (cols * rows) / 2 }
val BOARD_4x4 = BoardSpec(4, 4)
val BOARD_4x5 = BoardSpec(4, 5)
val BOARD_4x6 = BoardSpec(4, 6)

/** Añade tus drawables aquí */
private val LOCAL_IMAGE_POOL = listOf(
    R.drawable.portada_red, R.drawable.portada_1989, R.drawable.portada_midnights,
    R.drawable.portada_lover, R.drawable.portada_ttpd, R.drawable.portada_tloas,
    R.drawable.portada_roy, R.drawable.portada_botanica, R.drawable.portada_cuartoazul,
    R.drawable.portada_wishbone, R.drawable.portada_ten, R.drawable.portada_perfectas,
    R.drawable.portada_lmd, R.drawable.portada_tsou, R.drawable.portada_snsd,
    R.drawable.portada_mp3, R.drawable.portada_mayhem, R.drawable.portada_ameri,
    R.drawable.portada_reputation, R.drawable.portada_cnc
)

private val DEFAULT_BACK = R.drawable.dorso

data class Card(
    val id: Int,
    val pairId: Int,
    val frontResId: Int,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

/* matchPath queda para load/guardar universal */
data class GameState(
    val cards: List<Card>,
    val currentPlayer: Player = Player.A,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val playerAName: String = "Jugador A",
    val playerBName: String = "Jugador B",
    val playerAColor: Color = Color(0xFF003D6D),
    val playerBColor: Color = Color(0xFF6B2E5F),
    val flippedBuffer: List<Int> = emptyList(),
    val gameOver: Boolean = false,
    val backResId: Int = DEFAULT_BACK,
    val board: BoardSpec = BOARD_4x4,
    val mode: GameMode = GameMode.PVP,
    val inputLocked: Boolean = false, // bloquea taps del HUMANO (IA puede actuar)
    val aiErrorRate: Float = 0.15f,
    val rngSeed: Long? = null,
    val matchPath: String? = null,     // para load / referencia; el guardado final genera un XML nuevo
    val appliedMoves: Int = 0,         // (no crítico en esta versión; queda por compat)
    val localIsA: Boolean = true,      // BT: Host ≡ A local
    val keepTurnOnStreak: Boolean = true,

    // ✅ BT: Host autoritativo + “esperando confirmación”
    val btAuthoritative: Boolean = false,  // Host = true, Cliente = false
    val btAwaitingApply: Boolean = false   // Cliente: espera APPLY del host
)

// ===== ViewModel =====
class MemoryViewModel : ViewModel() {

    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCleared() {
        super.onCleared()
        vmScope.cancel()
    }

    private val _state = mutableStateOf(GameState(cards = emptyList()))
    val state: State<GameState> = _state

    private val aiMemory: MutableMap<Int, MutableSet<Int>> = mutableMapOf()
    private val aiForgetRate: Float = 0.58f

    // ✅ Buffer universal de jugadas (Local/IA/BT) — se escribe SOLO al guardar
    private val inMemMoves = mutableListOf<Int>()
    private fun startRecordingLocal() { inMemMoves.clear() }
    fun recordedMoves(): List<Int> = inMemMoves.toList()

    fun setBtAuthoritative(isHost: Boolean) {
        _state.value = _state.value.copy(btAuthoritative = isHost)
    }

    fun setBtAwaitingApply(waiting: Boolean) {
        _state.value = _state.value.copy(btAwaitingApply = waiting)
    }

    fun prepareNewGame(
        board: BoardSpec,
        nameA: String,
        nameB: String,
        colorA: Color,
        colorB: Color,
        mode: GameMode,
        aiErrorRate: Float
    ) {
        aiMemory.clear()
        startRecordingLocal()

        _state.value = _state.value.copy(
            cards = buildLocalDeck(board, null),
            currentPlayer = Player.A,
            scoreA = 0,
            scoreB = 0,
            playerAName = nameA.ifBlank { "Jugador A" },
            playerBName = if (mode == GameMode.AI) "CPU" else nameB.ifBlank { "Jugador B" },
            playerAColor = colorA,
            playerBColor = if (mode == GameMode.AI) cpuRed else colorB,
            flippedBuffer = emptyList(),
            gameOver = false,
            board = board,
            mode = mode,
            inputLocked = false,
            aiErrorRate = aiErrorRate.coerceIn(0f, 1f),
            rngSeed = null,
            matchPath = null,
            appliedMoves = 0,
            localIsA = true,
            btAuthoritative = false,
            btAwaitingApply = false
        )
    }

    fun prepareNewGameWithSeed(
        board: BoardSpec,
        nameA: String,
        nameB: String,
        colorA: Color,
        colorB: Color,
        mode: GameMode,
        aiErrorRate: Float,
        seed: Long,
        firstPlayer: Player,
        matchPath: String?,
        localIsA: Boolean,
        isBtHost: Boolean
    ) {
        aiMemory.clear()
        startRecordingLocal()

        _state.value = _state.value.copy(
            cards = buildLocalDeck(board, seed),
            currentPlayer = firstPlayer,
            scoreA = 0,
            scoreB = 0,
            playerAName = nameA.ifBlank { "Jugador A" },
            playerBName = if (mode == GameMode.AI) "CPU" else nameB.ifBlank { "Jugador B" },
            playerAColor = colorA,
            playerBColor = if (mode == GameMode.AI) cpuRed else colorB,
            flippedBuffer = emptyList(),
            gameOver = false,
            board = board,
            mode = mode,
            inputLocked = false,
            aiErrorRate = aiErrorRate.coerceIn(0f, 1f),
            rngSeed = seed,
            matchPath = matchPath,
            appliedMoves = 0,
            localIsA = localIsA,
            btAuthoritative = (mode == GameMode.BT && isBtHost),
            btAwaitingApply = false
        )
    }

    private fun buildLocalDeck(board: BoardSpec, seed: Long?): List<Card> {
        val neededPairs = board.pairs
        val pool = LOCAL_IMAGE_POOL
        require(pool.isNotEmpty()) { "LOCAL_IMAGE_POOL está vacío. Agrega tus drawables." }

        val fronts = mutableListOf<Int>()
        var remaining = neededPairs
        var baseSeed = seed ?: System.currentTimeMillis()

        while (remaining > 0) {
            val shuffled = pool.shuffled(Random(baseSeed))
            val takeCount = minOf(remaining, shuffled.size)
            fronts += shuffled.take(takeCount)
            remaining -= takeCount
            baseSeed += 37
        }

        return fronts.flatMapIndexed { p, resId ->
            listOf(
                Card(id = p * 2, pairId = p, frontResId = resId),
                Card(id = p * 2 + 1, pairId = p, frontResId = resId)
            )
        }.shuffled(Random(seed ?: System.currentTimeMillis()))
    }

    /**
     * @return true si el tap se aplicó.
     *
     * fromRemote      = jugada del rival (me llega por BT y NO es mi turno)
     * fromNetConfirm  = confirmación de MI jugada (me llega por BT y SÍ es mi turno)
     */
    fun onCardTapped(
        index: Int,
        fromAI: Boolean = false,
        fromRemote: Boolean = false,
        fromNetConfirm: Boolean = false
    ): Boolean {
        val s = _state.value
        val c = s.cards.getOrNull(index) ?: return false

        // inputLocked bloquea SOLO al humano local.
        if (s.inputLocked && !fromAI && !fromRemote && !fromNetConfirm) return false

        // Turno duro en BT
        if (s.mode == GameMode.BT) {
            val localTurn = if (s.localIsA) Player.A else Player.B
            val isMyTurn = (s.currentPlayer == localTurn)

            if (!fromRemote && !fromNetConfirm && !isMyTurn) return false
            if (fromRemote && isMyTurn) return false
            if (fromNetConfirm && !isMyTurn) return false
        }

        if (c.isMatched || c.isFaceUp || s.flippedBuffer.size == 2 || s.gameOver) return false

        // Registro universal en memoria (se escribe solo al guardar)
        inMemMoves.add(index)

        val cards1 = s.cards.toMutableList()
        cards1[index] = c.copy(isFaceUp = true)

        aiMemory.getOrPut(c.pairId) { mutableSetOf() }.add(index)
        val buf1 = s.flippedBuffer + index

        if (buf1.size == 2) {
            _state.value = s.copy(cards = cards1, flippedBuffer = buf1, inputLocked = true)

            val (i1, i2) = buf1
            val a = cards1[i1]; val b = cards1[i2]

            if (a.pairId == b.pairId) {
                // Match
                cards1[i1] = a.copy(isMatched = true)
                cards1[i2] = b.copy(isMatched = true)
                aiMemory[a.pairId]?.clear()

                val scoreA = if (s.currentPlayer == Player.A) s.scoreA + 1 else s.scoreA
                val scoreB = if (s.currentPlayer == Player.B) s.scoreB + 1 else s.scoreB
                val done = cards1.all { it.isMatched }

                _state.value = s.copy(
                    cards = cards1,
                    scoreA = scoreA,
                    scoreB = scoreB,
                    flippedBuffer = emptyList(),
                    gameOver = done,
                    inputLocked = false
                )
            } else {
                // Mismatch
                vmScope.launch {
                    delay(700)
                    val st = _state.value
                    val cards2 = st.cards.toMutableList()
                    val a2 = cards2[i1]; val b2 = cards2[i2]
                    cards2[i1] = a2.copy(isFaceUp = false)
                    cards2[i2] = b2.copy(isFaceUp = false)

                    _state.value = st.copy(
                        cards = cards2,
                        flippedBuffer = emptyList(),
                        currentPlayer = if (st.keepTurnOnStreak)
                            (if (st.currentPlayer == Player.A) Player.B else Player.A)
                        else st.currentPlayer,
                        inputLocked = false
                    )
                }
            }
        } else {
            _state.value = s.copy(cards = cards1, flippedBuffer = buf1)
        }

        return true
    }

    private fun aiMaybeForget(rate: Float) {
        if (rate <= 0f) return
        val r = Random(System.currentTimeMillis())
        val toModify = aiMemory.keys.toList()
        for (pid in toModify) {
            val set = aiMemory[pid] ?: continue
            if (set.isEmpty()) continue
            val roll = r.nextFloat()
            when {
                roll < rate * 0.6f -> set.clear()
                roll < rate -> set.remove(set.random(r))
            }
        }
    }

    fun performAIMove() {
        val s0 = _state.value
        if (s0.mode != GameMode.AI) return
        if (s0.currentPlayer != Player.B) return
        if (s0.gameOver) return
        if (s0.flippedBuffer.isNotEmpty()) return

        vmScope.launch {
            _state.value = _state.value.copy(inputLocked = true)

            aiMaybeForget(aiForgetRate)

            val errorRate = _state.value.aiErrorRate.coerceIn(0f, 1f)
            val rnd = Random(System.currentTimeMillis())

            var knownPair: Pair<Int, Int>? = aiMemory.entries.firstNotNullOfOrNull { (_, idxSet) ->
                val alive = idxSet.filter { idx ->
                    val cc = _state.value.cards.getOrNull(idx)
                    cc != null && !cc.isMatched && !cc.isFaceUp
                }
                if (alive.size >= 2) alive[0] to alive[1] else null
            }

            if (knownPair != null && rnd.nextFloat() < errorRate) knownPair = null

            var singleKnown: Int? = null
            if (knownPair == null) {
                singleKnown = aiMemory.entries.firstNotNullOfOrNull { (_, idxSet) ->
                    idxSet.firstOrNull { idx ->
                        val cc = _state.value.cards.getOrNull(idx)
                        cc != null && !cc.isMatched && !cc.isFaceUp
                    }
                }
                if (singleKnown != null && rnd.nextFloat() < errorRate) singleKnown = null
            }

            val (i1, i2) = if (knownPair != null) {
                knownPair!!
            } else if (singleKnown != null) {
                val pid = _state.value.cards[singleKnown].pairId
                val mate = _state.value.cards.withIndex()
                    .firstOrNull { (ix, c) -> c.pairId == pid && ix != singleKnown && !c.isMatched && !c.isFaceUp }
                    ?.index
                if (mate != null) singleKnown to mate else {
                    val pool = _state.value.cards.withIndex()
                        .filter { (_, c) -> !c.isMatched && !c.isFaceUp }
                        .map { it.index }
                        .shuffled()
                        .firstOrNull { it != singleKnown } ?: singleKnown
                    singleKnown to pool
                }
            } else {
                val pool = _state.value.cards.withIndex()
                    .filter { (_, c) -> !c.isMatched && !c.isFaceUp }
                    .map { it.index }
                if (pool.size >= 2) pool[0] to pool[1] else (pool.firstOrNull() ?: 0) to (pool.getOrNull(1) ?: 0)
            }

            onCardTapped(i1, fromAI = true)
            delay(450)
            onCardTapped(i2, fromAI = true)

            delay(900)

            val sAfter = _state.value
            _state.value = sAfter.copy(inputLocked = (sAfter.currentPlayer == Player.B))
        }
    }

    fun reshuffleKeepImages() {
        aiMemory.clear()
        startRecordingLocal()

        val s = _state.value
        if (s.cards.isEmpty()) return

        val pairFronts: List<Int> =
            s.cards.groupBy { it.pairId }
                .toSortedMap()
                .values
                .map { it.first().frontResId }

        val deck = pairFronts.flatMapIndexed { p, resId ->
            listOf(
                Card(id = p * 2, pairId = p, frontResId = resId, isFaceUp = false, isMatched = false),
                Card(id = p * 2 + 1, pairId = p, frontResId = resId, isFaceUp = false, isMatched = false)
            )
        }.shuffled(Random(System.currentTimeMillis()))

        _state.value = s.copy(
            cards = deck,
            currentPlayer = Player.A,
            scoreA = 0,
            scoreB = 0,
            flippedBuffer = emptyList(),
            gameOver = false,
            inputLocked = false,
            btAwaitingApply = false
        )
    }

    fun updateAppliedMoves(n: Int) {
        _state.value = _state.value.copy(appliedMoves = n)
    }
}

// ===== Navegación simple =====
private enum class Screen { HOME, MENU, GAME, BT_SETUP, BT_VOTE, LOAD }

@Composable
fun AppRoot(vm: MemoryViewModel) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var pendingMode by remember { mutableStateOf(GameMode.PVP) }

    // ✅ Para reproducción (cuando vienes de LOAD)
    var replayMoves by remember { mutableStateOf<List<Int>?>(null) }

    // BT handshake params
    var btSeed by remember { mutableLongStateOf(0L) }
    var btBoard by remember { mutableStateOf(BOARD_4x4) }
    var btFirst by remember { mutableStateOf(Player.A) }

    // nombres/colores para local/IA (y opcionalmente BT)
    var nameA by remember { mutableStateOf("") }
    var nameB by remember { mutableStateOf("") }
    var colorA by remember { mutableStateOf(paletteForHumans.first()) }
    var colorB by remember { mutableStateOf(paletteForHumans.getOrElse(1) { paletteForHumans.first() }) }

    when (screen) {
        Screen.HOME -> HomeScreen(
            onPvP = { pendingMode = GameMode.PVP; screen = Screen.MENU },
            onPvAI = { pendingMode = GameMode.AI; screen = Screen.MENU },
            onBluetooth = { screen = Screen.BT_SETUP },
            onLoad = { screen = Screen.LOAD }
        )

        Screen.MENU -> MenuScreen(
            mode = pendingMode,
            onStart = { board, nA, nB, cA, cB, mode, aiError ->
                nameA = nA; nameB = nB; colorA = cA; colorB = cB
                replayMoves = null // ✅ no es reproducción

                val seed = System.currentTimeMillis()
                vm.prepareNewGameWithSeed(
                    board = board,
                    nameA = nA.ifBlank { "Jugador A" },
                    nameB = if (mode == GameMode.AI) "CPU" else nB.ifBlank { "Jugador B" },
                    colorA = cA,
                    colorB = if (mode == GameMode.AI) cpuRed else cB,
                    mode = mode,
                    aiErrorRate = aiError,
                    seed = seed,
                    firstPlayer = Player.A,
                    matchPath = null,
                    localIsA = true,
                    isBtHost = false
                )
                screen = Screen.GAME
            },
            onBack = { screen = Screen.HOME }
        )

        Screen.GAME -> MemoryScreen(
            vm = vm,
            replayMoves = replayMoves,
            onReplayDone = { replayMoves = null },
            onGoToMenu = {
                BtSession.clearSession()
                screen = Screen.HOME
            },
            onRestartBtVote = { screen = Screen.BT_VOTE },
            onRestartLocal = { mode ->
                BtSession.clearSession()
                replayMoves = null
                pendingMode = mode
                screen = Screen.MENU
            }
        )

        Screen.BT_SETUP -> BtSetupScreen(
            onBack = {
                BtSession.clearSession()
                screen = Screen.HOME
            },
            onConnected = { isHost ->
                pendingMode = GameMode.BT
                replayMoves = null
                vm.setBtAuthoritative(isHost)
                screen = Screen.BT_VOTE
            }
        )

        Screen.BT_VOTE -> BtVotingScreen(
            onCancel = {
                BtSession.clearSession()
                screen = Screen.HOME
            },
            onStartAgreed = { board, seed, first ->
                btBoard = board; btSeed = seed; btFirst = first
                replayMoves = null

                val isHost = BtSession.isHost()
                vm.prepareNewGameWithSeed(
                    board = board,
                    nameA = "Jugador A",
                    nameB = "Jugador B",
                    colorA = paletteForHumans.first(),
                    colorB = paletteForHumans.getOrElse(1) { paletteForHumans.first() },
                    mode = GameMode.BT,
                    aiErrorRate = 0f,
                    seed = seed,
                    firstPlayer = first,
                    matchPath = null,
                    localIsA = isHost,
                    isBtHost = isHost
                )
                screen = Screen.GAME
            }
        )

        Screen.LOAD -> LoadGameScreen(
            vm = vm,
            onCancel = { screen = Screen.HOME },
            onLoaded = { moves ->
                replayMoves = moves
                screen = Screen.GAME
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onPvP: () -> Unit,
    onPvAI: () -> Unit,
    onBluetooth: () -> Unit,
    onLoad: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Memorama", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onPvP, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Juego local") }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPvAI, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Jugador contra IA") }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBluetooth, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Juego por Bluetooth") }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onLoad, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Cargar partida") }
        }
    }
}

// ===== Paleta =====
private val cpuRed = Color(0xFF7F1D1D)
private val paletteBase: List<Color> = listOf(
    Color(0xFF003D6D), Color(0xFF6B2E5F), Color(0xFF0D9488), Color(0xFF2563EB), Color(0xFF16A34A),
    Color(0xFFF59E0B), Color(0xFF7C3AED), Color(0xFF0891B2), Color(0xFF334155), Color(0xFF111827)
)
private val paletteForHumans: List<Color> = paletteBase

// ===== Menú =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    mode: GameMode,
    onStart: (BoardSpec, String, String, Color, Color, GameMode, Float) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedBoard by remember { mutableStateOf(BOARD_4x4) }
    var nameA by remember { mutableStateOf("") }
    var colorA by remember { mutableStateOf(paletteForHumans.first()) }
    var nameB by remember { mutableStateOf("") }
    var colorB by remember { mutableStateOf(paletteForHumans.getOrElse(1) { paletteForHumans.first() }) }
    var aiError by remember { mutableStateOf(0.15f) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configurar partida", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Regresar") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = nameA,
                onValueChange = { nameA = it },
                label = { Text("Nombre Jugador A") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Color Jugador A", fontWeight = FontWeight.SemiBold)
            ColorPaletteRow(paletteForHumans, colorA) { colorA = it }

            if (mode == GameMode.PVP) {
                Divider()
                OutlinedTextField(
                    value = nameB,
                    onValueChange = { nameB = it },
                    label = { Text("Nombre Jugador B") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Color Jugador B", fontWeight = FontWeight.SemiBold)
                ColorPaletteRow(paletteForHumans, colorB) { colorB = it }
            } else if (mode == GameMode.AI) {
                Divider()
                Text("Rival: CPU", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Color CPU: "); Spacer(Modifier.width(8.dp))
                    Surface(color = cpuRed, shape = CircleShape) { Box(Modifier.size(20.dp)) }
                }
                Spacer(Modifier.height(8.dp))
                Text("Índice de error de la CPU: ${"%.0f".format(aiError * 100)}%")
                Slider(
                    value = aiError,
                    onValueChange = { aiError = it.coerceIn(0f, 1f) },
                    steps = 9,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Divider()
            Text("Tamaño del tablero", fontWeight = FontWeight.SemiBold)
            BoardRadio("4 × 4 (8 parejas)", selectedBoard == BOARD_4x4) { selectedBoard = BOARD_4x4 }
            BoardRadio("4 × 5 (10 parejas)", selectedBoard == BOARD_4x5) { selectedBoard = BOARD_4x5 }
            BoardRadio("4 × 6 (12 parejas)", selectedBoard == BOARD_4x6) { selectedBoard = BOARD_4x6 }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val finalNameB = if (mode == GameMode.AI) "CPU" else nameB.ifBlank { "Jugador B" }
                    val finalColorB = if (mode == GameMode.AI) cpuRed else colorB
                    onStart(selectedBoard, nameA, finalNameB, colorA, finalColorB, mode, if (mode == GameMode.AI) aiError else 0f)
                }
            ) { Text("Comenzar") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ColorPaletteRow(colors: List<Color>, selected: Color, onSelect: (Color) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { c ->
            val isSelected = c == selected
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(width = if (isSelected) 3.dp else 1.dp, color = if (isSelected) Color.Black else Color.LightGray, shape = CircleShape)
                    .padding(2.dp)
                    .clickable { onSelect(c) }
            ) { Surface(color = c, shape = CircleShape, modifier = Modifier.fillMaxSize()) {} }
        }
    }
}

@Composable
private fun BoardRadio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

// ===== Pantalla de Juego =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    vm: MemoryViewModel,
    replayMoves: List<Int>?,            // ✅ si viene de LOAD, aquí llegan los movimientos
    onReplayDone: () -> Unit,           // ✅ se llama al terminar (para limpiar replayMoves en AppRoot)
    onGoToMenu: () -> Unit,
    onRestartBtVote: () -> Unit,
    onRestartLocal: (GameMode) -> Unit
) {
    val s by vm.state
    val context = LocalContext.current

    // ==========================
    // Reproducción (sin overlay)
    // ==========================
    var replaying by remember { mutableStateOf(false) }
    var isReplaySession by remember { mutableStateOf(false) }
    var lastReplayMoves by remember { mutableStateOf<List<Int>>(emptyList()) }
    var replayKey by remember { mutableIntStateOf(0) }

    // Velocidad de reproducción (más lenta)
    val REPLAY_START_DELAY_MS = 650L
    val REPLAY_TAP_GAP_MS = 500L
    val REPLAY_POST_PAIR_PAUSE_MS = 350L
    val REPLAY_FINAL_PAUSE_MS = 650L

    suspend fun waitUntil(pred: () -> Boolean) {
        while (!pred()) delay(25)
    }

    // Cuando llegan moves (LOAD), activamos sesión de reproducción y disparo 1 vez
    LaunchedEffect(replayMoves) {
        val moves = replayMoves ?: return@LaunchedEffect
        isReplaySession = true
        lastReplayMoves = moves
        replayKey++
        onReplayDone() // limpia la señal en AppRoot; la sesión sigue aquí
    }

    // Ejecuta reproducción
    LaunchedEffect(replayKey) {
        if (!isReplaySession) return@LaunchedEffect
        val moves = lastReplayMoves
        if (moves.isEmpty()) return@LaunchedEffect

        replaying = true
        delay(REPLAY_START_DELAY_MS)

        for (idx in moves) {
            // Espera a que sea "seguro" aplicar el siguiente tap
            waitUntil {
                val st = vm.state.value
                st.flippedBuffer.size < 2 && !st.inputLocked && !st.gameOver
            }

            // Intenta aplicar el tap (si falla por timing, reintenta)
            var applied = vm.onCardTapped(idx, fromAI = true)
            if (!applied) {
                repeat(4) {
                    delay(60)
                    if (vm.onCardTapped(idx, fromAI = true)) { applied = true; return@repeat }
                }
            }

            delay(REPLAY_TAP_GAP_MS)

            // Si hay 2 cartas volteadas o lock, espera resolución
            if (vm.state.value.flippedBuffer.size == 2 || vm.state.value.inputLocked) {
                waitUntil {
                    val st = vm.state.value
                    st.flippedBuffer.isEmpty() && !st.inputLocked
                }
                delay(REPLAY_POST_PAIR_PAUSE_MS)
            }
        }

        delay(REPLAY_FINAL_PAUSE_MS)
        replaying = false
    }

    // ==========================
    // Bluetooth (sin XML live)
    // ==========================
    LaunchedEffect(s.mode) {
        if (s.mode == GameMode.BT) {
            BtSession.onXmlReceived = null

            BtSession.onLineMessage = { raw ->
                when {
                    raw.startsWith("REQ_TAP ") && vm.state.value.btAuthoritative -> {
                        val idxStr = raw.removePrefix("REQ_TAP ").trim()
                        val idx = idxStr.toIntOrNull()
                        if (idx != null) {
                            val st = vm.state.value
                            val remotePlayer = if (st.localIsA) Player.B else Player.A
                            val remoteTurn = st.currentPlayer == remotePlayer

                            val canApply =
                                remoteTurn &&
                                        st.flippedBuffer.size < 2 &&
                                        !st.gameOver &&
                                        !st.inputLocked

                            if (canApply) {
                                val applied = vm.onCardTapped(index = idx, fromAI = false, fromRemote = true)
                                if (applied) BtSession.sendLine("APPLY $idx")
                            }
                        }
                    }

                    raw.startsWith("APPLY ") && !vm.state.value.btAuthoritative -> {
                        val idxStr = raw.removePrefix("APPLY ").trim()
                        val idx = idxStr.toIntOrNull()
                        if (idx != null) {
                            val st = vm.state.value
                            if (st.btAwaitingApply) {
                                vm.onCardTapped(index = idx, fromAI = false, fromRemote = false, fromNetConfirm = true)
                                vm.setBtAwaitingApply(false)
                            } else {
                                vm.onCardTapped(index = idx, fromAI = false, fromRemote = true, fromNetConfirm = false)
                            }
                        }
                    }

                    raw == "POSTGAME VOTE" -> onRestartBtVote()
                    raw == "POSTGAME EXIT" -> { BtSession.clearSession(); onGoToMenu() }
                }
            }

        } else {
            BtSession.onLineMessage = null
            BtSession.onXmlReceived = null
        }
    }

    // Turno IA
    LaunchedEffect(s.currentPlayer, s.flippedBuffer, s.gameOver, s.mode) {
        if (s.mode == GameMode.AI && s.currentPlayer == Player.B && s.flippedBuffer.isEmpty() && !s.gameOver) {
            vm.performAIMove()
        }
    }

    val showSettings = s.mode != GameMode.BT
    var menuCardVisible by remember { mutableStateOf(false) }

    // Contador post-game SOLO si NO es reproducción
    var postCount by remember(s.gameOver) { mutableIntStateOf(15) }
    LaunchedEffect(s.gameOver, isReplaySession) {
        if (s.gameOver && !isReplaySession) {
            postCount = 15
            while (postCount > 0) { delay(1000); postCount-- }
        }
    }

    // Guardar XML (NO se muestra en reproducción)
    fun saveCurrentXml(): File? {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val dst = File(outDir, "partida_${ts}.xml")

        val st = vm.state.value
        val header = MatchHeader(
            boardCols = st.board.cols,
            boardRows = st.board.rows,
            seed = st.rngSeed ?: System.currentTimeMillis(),
            playerAName = st.playerAName,
            playerBName = st.playerBName,
            playerAColor = st.playerAColor.value.toLong(),
            playerBColor = st.playerBColor.value.toLong()
        )

        return try {
            val tmp = MatchXml.writeNewMatch(context, header)
            vm.recordedMoves().forEach { move -> MatchXml.appendMove(tmp, move) }
            tmp.inputStream().use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
            dst
        } catch (_: Throwable) { null }
    }

    val activePlayer = s.currentPlayer
    val activeColor = if (activePlayer == Player.A) s.playerAColor else s.playerBColor

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Surface(
                        color = activeColor.copy(alpha = 0.18f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = activeColor, shape = CircleShape) { Box(Modifier.size(10.dp)) }
                            Text(
                                text = when {
                                    isReplaySession -> "Reproducción"
                                    s.mode == GameMode.AI && s.currentPlayer == Player.B ->
                                        "Turno: ${s.playerBName} (IA)"
                                    s.mode == GameMode.BT -> {
                                        val soyA = s.localIsA
                                        val localTurn = if (soyA) Player.A else Player.B
                                        "Turno: " + if (s.currentPlayer == localTurn) "Tú" else "Rival"
                                    }
                                    else ->
                                        "Turno: " + if (s.currentPlayer == Player.A) s.playerAName else s.playerBName
                                },
                                color = Color.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                },
                actions = {
                    if (showSettings && !isReplaySession) { // ✅ en replay no tiene sentido abrir ajustes
                        IconButton(onClick = { menuCardVisible = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = Color.Black)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize().background(Color.White)) {

            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScorePill(
                        name = s.playerAName,
                        score = s.scoreA,
                        color = s.playerAColor,
                        isActive = (s.currentPlayer == Player.A)
                    )
                    ScorePill(
                        name = s.playerBName,
                        score = s.scoreB,
                        color = s.playerBColor,
                        isActive = (s.currentPlayer == Player.B)
                    )
                }

                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(s.board.cols),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = true
                ) {
                    items(items = s.cards, key = { it.id }) { c ->
                        FlipCard(
                            faceUp = c.isFaceUp || c.isMatched,
                            matched = c.isMatched,
                            frontResId = c.frontResId,
                            backResId = s.backResId,
                            onClick = {
                                // ✅ En reproducción: no permitir taps del usuario
                                if (isReplaySession || replaying) return@FlipCard

                                val st = vm.state.value
                                val idx = st.cards.indexOf(c)

                                when (st.mode) {
                                    GameMode.PVP -> {
                                        if (!st.inputLocked) vm.onCardTapped(idx)
                                    }

                                    GameMode.AI -> {
                                        if (!st.inputLocked && st.currentPlayer == Player.A) vm.onCardTapped(idx)
                                    }

                                    GameMode.BT -> {
                                        val localPlayer = if (st.localIsA) Player.A else Player.B
                                        val myTurn = st.currentPlayer == localPlayer

                                        val canTap = myTurn &&
                                                st.flippedBuffer.size < 2 &&
                                                !st.gameOver &&
                                                !st.inputLocked

                                        if (!canTap) return@FlipCard

                                        if (st.btAuthoritative) {
                                            val applied = vm.onCardTapped(idx, fromRemote = false)
                                            if (applied) BtSession.sendLine("APPLY $idx")
                                        } else {
                                            if (!st.btAwaitingApply) {
                                                vm.setBtAwaitingApply(true)
                                                BtSession.sendLine("REQ_TAP $idx")
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Ajustes (no BT, no replay)
            if (showSettings && menuCardVisible && !isReplaySession) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))
                        .clickable { menuCardVisible = false }
                )
                Card(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).widthIn(max = 280.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Ajustes rápidos", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                        Divider()
                        Button(
                            onClick = {
                                menuCardVisible = false
                                vm.reshuffleKeepImages()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Nueva partida")
                        }
                        OutlinedButton(
                            onClick = { menuCardVisible = false; onGoToMenu() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Home, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Volver al menú")
                        }
                    }
                }
            }

            // Fin de partida
            if (s.gameOver) {

                val msg = if (isReplaySession) {
                    "Reproducción finalizada"
                } else {
                    when (s.mode) {
                        GameMode.BT -> {
                            val soyA = s.localIsA
                            val localScore = if (soyA) s.scoreA else s.scoreB
                            val remoteScore = if (soyA) s.scoreB else s.scoreA
                            when {
                                localScore > remoteScore -> "¡Ganaste!"
                                localScore < remoteScore -> "¡Perdiste!"
                                else -> "¡Empate!"
                            }
                        }
                        else -> when {
                            s.scoreA > s.scoreB -> "¡Ganaste!"
                            s.scoreB > s.scoreA -> "¡Ganó el rival!"
                            else -> "¡Empate!"
                        }
                    }
                }

                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(msg, style = MaterialTheme.typography.titleMedium, color = Color.Black)

                        if (isReplaySession) {
                            // ✅ SOLO repetir o salir
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                                Button(
                                    onClick = {
                                        val st = vm.state.value
                                        // Reinicia y vuelve a reproducir
                                        vm.prepareNewGameWithSeed(
                                            board = st.board,
                                            nameA = st.playerAName,
                                            nameB = st.playerBName,
                                            colorA = st.playerAColor,
                                            colorB = st.playerBColor,
                                            mode = GameMode.PVP,
                                            aiErrorRate = 0f,
                                            seed = st.rngSeed ?: System.currentTimeMillis(),
                                            firstPlayer = Player.A,
                                            matchPath = st.matchPath,
                                            localIsA = true,
                                            isBtHost = false
                                        )
                                        replayKey++
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Repetir partida") }

                                OutlinedButton(
                                    onClick = {
                                        BtSession.clearSession()
                                        onGoToMenu()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Salir") }
                            }

                        } else {
                            // ✅ UI normal (no reproducción)
                            val savedHint = remember { mutableStateOf<String?>(null) }
                            Button(
                                onClick = { val saved = saveCurrentXml(); savedHint.value = saved?.absolutePath },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Guardar partida (XML)") }

                            savedHint.value?.let { path ->
                                Text("Guardado en:\n$path", style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155))
                            }

                            Divider()

                            Text(
                                text = if (s.mode == GameMode.BT) "Nueva votación en: ${postCount}s" else "Nueva partida en: ${postCount}s",
                                color = Color.Black
                            )

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        if (s.mode == GameMode.BT) {
                                            BtSession.sendLine("POSTGAME VOTE")
                                            onRestartBtVote()
                                        } else {
                                            onRestartLocal(s.mode)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Nueva partida") }

                                OutlinedButton(
                                    onClick = {
                                        if (s.mode == GameMode.BT) BtSession.sendLine("POSTGAME EXIT")
                                        BtSession.clearSession()
                                        onGoToMenu()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Salir") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScorePill(name: String, score: Int, color: Color, isActive: Boolean) {
    Surface(
        color = if (isActive) color.copy(alpha = 0.16f) else Color(0xFFEFF1F4),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.border(
            width = if (isActive) 2.dp else 0.dp,
            color = if (isActive) color.copy(alpha = 0.55f) else Color.Transparent,
            shape = MaterialTheme.shapes.large
        )
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = color, shape = CircleShape) { Box(Modifier.size(12.dp)) }
            Text("$name: $score", color = Color.Black, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FlipCard(
    faceUp: Boolean,
    matched: Boolean,
    frontResId: Int,
    backResId: Int,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (faceUp) 180f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "flip"
    )
    val showFront = rotation > 90f
    val density = androidx.compose.ui.platform.LocalDensity.current

    val frontPainter = painterResource(frontResId)
    val backPainter = painterResource(backResId)

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16 * density.density
            }
    ) {
        val contentMod = Modifier.fillMaxSize().graphicsLayer { if (showFront) scaleX = -1f }
        Image(
            painter = if (showFront) frontPainter else backPainter,
            contentDescription = if (showFront) "Carta frente" else "Carta dorso",
            contentScale = ContentScale.Crop,
            modifier = contentMod
        )
        if (matched) {
            Box(
                Modifier.fillMaxSize().border(
                    3.dp,
                    Color.White.copy(alpha = 0.6f),
                    MaterialTheme.shapes.medium
                )
            )
        }
    }
}

/* ============================
   Bluetooth: Setup + Votación
   ============================ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BtSetupScreen(
    onBack: () -> Unit,
    onConnected: (isHost: Boolean) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var hasBt by remember { mutableStateOf(true) }
    var btEnabled by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    val devices = remember { mutableStateListOf<BluetoothDevice>() }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isHost by remember { mutableStateOf(true) }
    var myName by remember { mutableStateOf("") }

    val btService = remember {
        BluetoothService(
            context = context,
            onDeviceFound = { dev -> if (devices.none { it.address == dev.address }) devices.add(dev) },
            onDiscoveryFinished = { scanning = false },
            onConnected = { sock, fromServer ->
                BtSession.attachSocket(sock, fromServer)
                connectionStatus = if (fromServer) "Conectado (Host)" else "Conectado (Cliente)"
                onConnected(fromServer)
            },
            onError = { th -> errorMsg = th.message ?: th.toString() }
        )
    }

    DisposableEffect(Unit) {
        btService.register()
        hasBt = btService.isSupported()
        btEnabled = btService.isEnabled()
        myName = btService.selfName()
        onDispose { btService.unregister() }
    }

    fun hasScanPermission(): Boolean {
        val pScan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        val pConn = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        return pScan == PackageManager.PERMISSION_GRANTED && pConn == PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bluetooth") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasBt) {
                Text("Este dispositivo no soporta Bluetooth.", color = Color.Red)
            } else {
                if (!btEnabled) {
                    Button(onClick = { btService.requestEnable(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Activar Bluetooth")
                    }
                    Text("Pulsa y acepta el diálogo del sistema.")
                } else {
                    Text("Bluetooth activado ✅")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rol: "); Spacer(Modifier.width(12.dp))
                    AssistChip(onClick = { isHost = true }, label = { Text("Crear sala (Host)") })
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = { isHost = false }, label = { Text("Buscar (Cliente)") })
                }

                if (btEnabled) {
                    Spacer(Modifier.height(6.dp))
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            Text("Mi dispositivo", style = MaterialTheme.typography.labelLarge, color = Color.Black)
                            Text(myName.ifBlank { "(sin nombre)" }, color = Color.Black)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (isHost) {
                            Button(
                                onClick = {
                                    devices.clear()
                                    connectionStatus = "Esperando conexión..."
                                    btService.stopDiscovery()
                                    btService.requestDiscoverable(context, 180)
                                    btService.startServer()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Crear sala (Host)") }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                                Button(
                                    onClick = {
                                        if (!hasScanPermission()) {
                                            errorMsg = "Permisos BLUETOOTH_SCAN/CONNECT requeridos (Android 12+)."
                                            return@Button
                                        }
                                        devices.clear()
                                        scanning = true
                                        btService.startDiscovery()
                                    },
                                    enabled = !scanning,
                                    modifier = Modifier.weight(1f)
                                ) { Text(if (scanning) "Buscando..." else "Buscar usuarios") }

                                OutlinedButton(
                                    onClick = { btService.stopDiscovery(); scanning = false },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Detener búsqueda") }
                            }
                        }
                    }
                }

                if (!isHost && devices.isNotEmpty()) {
                    Divider(); Text("Usuarios cercanos", color = Color.Black)
                    devices.forEach { dev ->
                        ListItem(
                            headlineContent = { Text(dev.name ?: "(Sin nombre)") },
                            supportingContent = { Text("Dispositivo disponible") },
                            modifier = Modifier.clickable {
                                if (!hasScanPermission()) {
                                    errorMsg = "Otorga permisos (SCAN/CONNECT) en Ajustes."
                                    return@clickable
                                }
                                connectionStatus = "Conectando a ${dev.name ?: "dispositivo"}..."
                                btService.connectTo(dev)
                            }
                        )
                    }
                }

                connectionStatus?.let { Text(it) }
                errorMsg?.let { Text("Error: $it", color = Color.Red) }
            }
        }
    }
}

/** Votación + arranque sincronizado (seed + primer turno = A por política "servidor primero"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BtVotingScreen(
    onCancel: () -> Unit,
    onStartAgreed: (BoardSpec, Long, Player) -> Unit
) {
    BackHandler { onCancel() }

    var myVote by remember { mutableStateOf<BoardSpec?>(null) }
    var remoteVote by remember { mutableStateOf<BoardSpec?>(null) }
    var secondsLeft by remember { mutableIntStateOf(10) }
    var decided by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BtSession.onLineMessage = { line ->
            when {
                line.startsWith("VOTE ") -> {
                    remoteVote = parseBoard(line.removePrefix("VOTE ").trim())
                    if (myVote != null && !decided) resolveVote(myVote!!, remoteVote!!, onStartAgreed) { decided = true }
                }
                line.startsWith("VOTERES ") -> {
                    val parts = line.removePrefix("VOTERES ").trim().split(" ")
                    if (parts.size >= 3) {
                        val board = parseBoard(parts[0]) ?: BOARD_4x4
                        val seed = parts[1].toLongOrNull() ?: System.currentTimeMillis()
                        val first = if (parts[2].equals("A", true)) Player.A else Player.B
                        decided = true
                        onStartAgreed(board, seed, first)
                    }
                }
            }
        }
    }

    LaunchedEffect(secondsLeft, decided) {
        if (!decided && secondsLeft > 0) {
            delay(1000)
            secondsLeft--
            if (secondsLeft == 0 && BtSession.isHost()) {
                val final = myVote ?: remoteVote ?: listOf(BOARD_4x4, BOARD_4x5, BOARD_4x6).random()
                val seed = System.currentTimeMillis()
                val first = Player.A
                BtSession.sendLine("VOTERES ${fmtBoard(final)} $seed A")
                decided = true
                onStartAgreed(final, seed, first)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Votación de tablero") },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Vota el tamaño del tablero (10 s) • $secondsLeft s")
            VoteButton("4 × 4 (8 parejas)", BOARD_4x4, myVote) { choice ->
                myVote = choice; BtSession.sendLine("VOTE ${fmtBoard(choice)}")
                if (remoteVote != null && !decided) resolveVote(choice, remoteVote!!, onStartAgreed) { decided = true }
            }
            VoteButton("4 × 5 (10 parejas)", BOARD_4x5, myVote) { choice ->
                myVote = choice; BtSession.sendLine("VOTE ${fmtBoard(choice)}")
                if (remoteVote != null && !decided) resolveVote(choice, remoteVote!!, onStartAgreed) { decided = true }
            }
            VoteButton("4 × 6 (12 parejas)", BOARD_4x6, myVote) { choice ->
                myVote = choice; BtSession.sendLine("VOTE ${fmtBoard(choice)}")
                if (remoteVote != null && !decided) resolveVote(choice, remoteVote!!, onStartAgreed) { decided = true }
            }
            Spacer(Modifier.height(8.dp))
            Text("Reglas: Si coinciden, se usa ese tablero. Si difieren, el Host decide 50/50. Si nadie vota, se elige aleatorio.")
        }
    }
}

@Composable
private fun VoteButton(label: String, spec: BoardSpec, current: BoardSpec?, onPick: (BoardSpec) -> Unit) {
    if (current == spec) Button(onClick = { onPick(spec) }) { Text(label) }
    else OutlinedButton(onClick = { onPick(spec) }) { Text(label) }
}

private fun fmtBoard(b: BoardSpec) = "${b.cols}x${b.rows}"
private fun parseBoard(s: String): BoardSpec? = try {
    val (c, r) = s.lowercase().split("x").map { it.trim().toInt() }
    when {
        c == 4 && r == 4 -> BOARD_4x4
        c == 4 && r == 5 -> BOARD_4x5
        c == 4 && r == 6 -> BOARD_4x6
        else -> null
    }
} catch (_: Throwable) { null }

/** Host decide y notifica con VOTERES (primer turno = A). */
private fun resolveVote(
    mine: BoardSpec,
    theirs: BoardSpec,
    onStartAgreed: (BoardSpec, Long, Player) -> Unit,
    onDecided: () -> Unit
) {
    if (BtSession.isHost()) {
        val selected = if (mine == theirs) mine else if (Random(System.currentTimeMillis()).nextBoolean()) mine else theirs
        val seed = System.currentTimeMillis()
        val first = Player.A
        BtSession.sendLine("VOTERES ${fmtBoard(selected)} $seed A")
        onDecided()
        onStartAgreed(selected, seed, first)
    }
}

/* ============================
   Cargar partida (por ruta)
   ============================ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadGameScreen(
    vm: MemoryViewModel,
    onCancel: () -> Unit,
    onLoaded: (moves: List<Int>) -> Unit
) {
    BackHandler { onCancel() }
    val context = LocalContext.current

    // ✅ Carpeta por defecto SIN CAMBIOS
    val defaultDir = remember {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    }
    var currentDir by remember { mutableStateOf<File>(defaultDir) }
    var manualPath by remember { mutableStateOf(currentDir.absolutePath) }
    var files by remember { mutableStateOf(listXmlSaves(currentDir)) }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    // Confirmaciones borrado
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    fun tryScan(path: String) {
        val f = File(path)
        if (f.exists() && f.isDirectory) {
            currentDir = f
            files = listXmlSaves(f)
            status = "Mostrando ${files.size} partidas en: ${f.absolutePath}"
        } else {
            status = "Ruta inválida: $path"
        }
    }

    fun boardFromHeader(h: MatchHeader): BoardSpec = when {
        h.boardCols == 4 && h.boardRows == 4 -> BOARD_4x4
        h.boardCols == 4 && h.boardRows == 5 -> BOARD_4x5
        h.boardCols == 4 && h.boardRows == 6 -> BOARD_4x6
        else -> BoardSpec(h.boardCols, h.boardRows)
    }

    fun loadFile(file: File) {
        loading = true
        status = "Leyendo ${file.name}..."
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val (header, moves) = MatchXml.readMatch(file)
                val board = boardFromHeader(header)

                vm.prepareNewGameWithSeed(
                    board = board,
                    nameA = header.playerAName,
                    nameB = header.playerBName,
                    colorA = Color(header.playerAColor),
                    colorB = Color(header.playerBColor),
                    mode = GameMode.PVP,
                    aiErrorRate = 0f,
                    seed = header.seed,
                    firstPlayer = Player.A,
                    matchPath = file.absolutePath,
                    localIsA = true,
                    isBtHost = false
                )

                loading = false
                onLoaded(moves) // ✅ reproducción se hace en MemoryScreen
            } catch (t: Throwable) {
                status = "Error leyendo XML: ${t.message}"
                loading = false
            }
        }
    }

    fun refreshList() {
        files = listXmlSaves(currentDir)
    }

    fun deleteFile(f: File) {
        try {
            val ok = f.delete()
            status = if (ok) "Borrado: ${f.name}" else "No se pudo borrar: ${f.name}"
        } catch (t: Throwable) {
            status = "Error borrando: ${t.message}"
        }
        refreshList()
    }

    fun deleteAllXml() {
        var count = 0
        files.forEach { f ->
            try { if (f.delete()) count++ } catch (_: Throwable) {}
        }
        status = "Borrados $count archivo(s) XML en: ${currentDir.absolutePath}"
        refreshList()
    }

    // Dialog borrar uno
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Borrar archivo") },
            text = { Text("¿Seguro que quieres borrar '${target.name}'?") },
            confirmButton = {
                Button(onClick = { deleteFile(target); deleteTarget = null }) { Text("Borrar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    // Dialog borrar todos
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Borrar todos los XML") },
            text = { Text("¿Seguro que quieres borrar TODOS los 'partida_*.xml' en:\n${currentDir.absolutePath}?") },
            confirmButton = {
                Button(onClick = { deleteAllXml(); confirmDeleteAll = false }) { Text("Borrar todo") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmDeleteAll = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cargar partida (XML)") },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        },
        containerColor = Color.White
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Carpeta actual:", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = manualPath,
                onValueChange = { manualPath = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ruta absoluta (ej. /storage/emulated/0/Download)") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { tryScan(manualPath) }) { Text("Escanear") }
                OutlinedButton(onClick = { manualPath = defaultDir.absolutePath; tryScan(manualPath) }) { Text("Carpeta por defecto") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { confirmDeleteAll = true },
                    modifier = Modifier.weight(1f),
                    enabled = files.isNotEmpty() && !loading
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Borrar todos XML")
                }
                OutlinedButton(
                    onClick = { refreshList(); status = "Lista actualizada." },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                ) { Text("Actualizar") }
            }

            Divider()

            Text("Partidas encontradas: ${files.size}")
            if (files.isEmpty()) {
                Text("No hay archivos 'partida_*.xml' en esta carpeta.", color = Color(0xFF6B7280))
            } else {
                files.forEach { f ->
                    ElevatedCard(
                        onClick = { if (!loading) loadFile(f) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontWeight = FontWeight.SemiBold)
                                Text("Ubicación: ${f.parent}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                                Text("Tamaño: ${f.length()} bytes", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                                Text("Modificado: ${Date(f.lastModified())}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                            }
                            IconButton(
                                onClick = { deleteTarget = f },
                                enabled = !loading
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Borrar XML")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            status?.let { Text(it, color = Color(0xFF334155)) }

            if (loading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        }
    }
}

/** Lista archivos partida_*.xml ordenados por fecha desc. */
private fun listXmlSaves(dir: File): List<File> =
    dir.listFiles { f ->
        f.isFile && f.name.lowercase(Locale.getDefault()).startsWith("partida_") && f.name.endsWith(".xml")
    }?.sortedByDescending { it.lastModified() } ?: emptyList()
