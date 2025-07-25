package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.bhlangonijr.chesslib.Square

@Composable
fun ChessBoard(
    boardState: Array<Array<Piece?>>,
    onSquareClick: (Square) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f) // Make sure it's square
    ) {
        for (row in 0..7) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                for (col in 0..7) {
                    val square = Square.valueOf("${('A' + col)}${8 - row}")
                    val piece = boardState[row][col]
                    val isLight = (row + col) % 2 == 0
                    val bgColor = if (isLight) Color(0xFFEEEED2) else Color(0xFF769656)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(bgColor)
                            .clickable { onSquareClick(square) },
                        contentAlignment = Alignment.Center
                    ) {
                        piece?.let {
                            Text(
                                text = pieceSymbol(it),
                                fontSize = 30.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

fun pieceSymbol(piece: Piece): String {
    return when (piece.type) {
        PieceType.PAWN -> if (piece.color == PieceColor.WHITE) "♙" else "♟"
        PieceType.ROOK -> if (piece.color == PieceColor.WHITE) "♖" else "♜"
        PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) "♘" else "♞"
        PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) "♗" else "♝"
        PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) "♕" else "♛"
        PieceType.KING -> if (piece.color == PieceColor.WHITE) "♔" else "♚"
    }
}