package com.example.myapplication

import android.content.Context
import java.io.*
class StockfishEngine(context: Context) {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    init {
        try {
            val enginePath = File(context.filesDir, "stockfish")
            context.assets.open("stockfish").use { input ->
                    FileOutputStream(enginePath).use { output ->
                    input.copyTo(output)
            }
            }
            enginePath.setExecutable(true)

            process = Runtime.getRuntime().exec(enginePath.absolutePath)
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendCommand(command: String) {
        writer?.apply {
            write("$command\n")
            flush()
        }
    }

    fun getBestMove(fen: String): String? {
        sendCommand("uci")
        sendCommand("isready")
        sendCommand("ucinewgame")
        sendCommand("position fen $fen")
        sendCommand("go depth 15")

        var line: String?
        while (reader?.readLine().also { line = it } != null) {
            if (line!!.startsWith("bestmove")) {
                return line!!.split(" ")[1]
            }
        }
        return null
    }

    fun stop() {
        sendCommand("quit")
        writer?.close()
        reader?.close()
        process?.destroy()
    }
}
data class StockfishAnalysis(val bestMove: String?, val ponderMove: String?, val score: String?)

fun parseStockfishOutput(output: String): StockfishAnalysis {
    var bestMove: String? = null
    var ponder: String? = null
    var score: String? = null

    val lines = output.lines()

    for (line in lines) {
        if (line.startsWith("info") && "score cp" in line) {
            // Extract score
            val parts = line.split(" ")
            val cpIndex = parts.indexOf("cp")
            if (cpIndex != -1 && cpIndex + 1 < parts.size) {
                val centipawns = parts[cpIndex + 1].toIntOrNull()
                if (centipawns != null) {
                    score = "%.2f".format(centipawns / 100.0)
                }
            }
        }

        if (line.startsWith("info") && "score mate" in line) {
            // Extract mate score
            val parts = line.split(" ")
            val mateIndex = parts.indexOf("mate")
            if (mateIndex != -1 && mateIndex + 1 < parts.size) {
                score = "#${parts[mateIndex + 1]}"  // mate in x
            }
        }

        if (line.startsWith("bestmove")) {
            val parts = line.split(" ")
            bestMove = parts.getOrNull(1)
            ponder = parts.getOrNull(3)
        }
    }

    return StockfishAnalysis(bestMove, ponder, score)
}