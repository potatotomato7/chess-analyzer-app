package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.*
import com.example.myapplication.voice.VoiceInputButton
import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveException
import com.github.bhlangonijr.chesslib.pgn.PgnHolder
import com.github.bhlangonijr.chesslib.File as ChessFile
import java.io.File
import com.example.myapplication.ui.Piece
import com.example.myapplication.ui.PieceType
import com.github.bhlangonijr.chesslib.Rank
import android.content.Context
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.example.myapplication.StockfishEngine
import java.io.*


class MainActivity : ComponentActivity() {

    private lateinit var board: Board
    private var selectedSquare: Square? = null
    private var moveHistory = mutableListOf<Move>()
    private var moveIndex by mutableStateOf(0)

    private var boardState by mutableStateOf(Array(8) { arrayOfNulls<Piece?>(8) })
    private var moves by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMicPermission()

        board = Board()
        board.loadFromFen(Constants.startStandardFENPosition)
        boardState = convertBoardTo2DArray(board)

        val engine = StockfishEngine(this)
        val llm = LLMHelper("")// anyone who downloads app put openai api key here;

        val chatResponse = mutableStateOf("")

        if (engine.start()) {
            engine.sendCommand("uci")
            Thread.sleep(500)
            println(engine.readOutput())

            engine.sendCommand("position startpos moves e2e4 e7e5")
            engine.sendCommand("go depth 15")
            Thread.sleep(2000)
            val output = engine.readOutput()
            println("Stockfish says:\n$output")

            val analysis = parseStockfishOutput(output)
            llm.askChatGPT(
                "Given the current chess position, Stockfish suggests move ${analysis.bestMove} with evaluation score ${analysis.score}. Can you explain this like a friendly chess coach?",
            ) { response ->
                chatResponse.value = response
            }

            engine.stop()
        }

        setContent {
            val showPgnDialog = remember { mutableStateOf(false) }
            val pgnText = remember { mutableStateOf("") }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.weight(2f)) {
                            Row {
                                Button(onClick = { showPgnDialog.value = true }) {
                                    Text("Paste PGN")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                VoiceInputButton { spokenText ->
                                    // You can trigger LLM here too
                                }
                            }

                            ChessBoard(
                                boardState = boardState,
                                onSquareClick = { square ->
                                    if (selectedSquare == null) {
                                        selectedSquare = square
                                    } else {
                                        val move = Move(selectedSquare, square)
                                        try {
                                            if (board.legalMoves().contains(move)) {
                                                board.doMove(move)
                                                moveHistory.add(move)
                                                moveIndex++
                                                boardState = convertBoardTo2DArray(board)
                                                moves = moveHistory.map { it.toString() }
                                            }
                                        } catch (e: MoveException) {
                                            e.printStackTrace()
                                        }
                                        selectedSquare = null
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            LazyColumn {
                                items(moves.chunked(2)) { pair ->
                                    Row {
                                        Text("${moves.indexOf(pair[0]) / 2 + 1}.", color = Color.DarkGray)
                                        Text(" ${pair.getOrNull(0) ?: ""} ", color = Color.Black)
                                        Text(pair.getOrNull(1) ?: "", color = Color.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("AI Insight:", style = MaterialTheme.typography.titleMedium)
                            Text(chatResponse.value, color = Color.DarkGray)
                        }
                    }

                    if (showPgnDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showPgnDialog.value = false },
                            confirmButton = {
                                Button(onClick = {
                                    try {
                                        val file = File(cacheDir, "pasted.pgn")
                                        file.writeText(pgnText.value)
                                        importGameFromPgnFile(file)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    showPgnDialog.value = false
                                }) {
                                    Text("Import")
                                }
                            },
                            dismissButton = {
                                Button(onClick = { showPgnDialog.value = false }) {
                                    Text("Cancel")
                                }
                            },
                            title = { Text("Paste PGN") },
                            text = {
                                TextField(
                                    value = pgnText.value,
                                    onValueChange = { pgnText.value = it },
                                    label = { Text("PGN Text") },
                                    singleLine = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestMicPermission() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
        }
    }

    private fun reloadBoardToMoveIndex() {
        board.loadFromFen(Constants.startStandardFENPosition)
        for (i in 0 until moveIndex) {
            board.doMove(moveHistory[i])
        }
        boardState = convertBoardTo2DArray(board)
    }

    private fun convertBoardTo2DArray(board: Board): Array<Array<Piece?>> {
        val result = Array(8) { arrayOfNulls<Piece?>(8) }
        for (rankIndex in 0..7) {
            for (fileIndex in 0..7) {
                val rank = Rank.values()[7 - rankIndex]
                val file = ChessFile.values()[fileIndex]
                val square = Square.encode(rank, file)
                val libPiece = board.getPiece(square)

                if (libPiece != null && libPiece != com.github.bhlangonijr.chesslib.Piece.NONE) {
                    val type = PieceType.valueOf(libPiece.pieceType.name)
                    val color = if (libPiece.pieceSide == Side.WHITE) PieceColor.WHITE else PieceColor.BLACK
                    result[rankIndex][fileIndex] = Piece(type, color)
                }
            }
        }
        return result
    }

    private fun importGameFromPgnFile(pgnFile: File) {
        val pgn = PgnHolder(pgnFile.absolutePath)
        pgn.loadPgn()
        val game = pgn.games.firstOrNull() ?: return
        moveHistory = game.halfMoves.toMutableList()
        moveIndex = moveHistory.size
        reloadBoardToMoveIndex()
        moves = moveHistory.map { it.toString() }
    }
}