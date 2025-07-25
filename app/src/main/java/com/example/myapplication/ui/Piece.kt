package com.example.myapplication.ui

enum class PieceType {
    PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
}

enum class PieceColor {
    WHITE, BLACK
}

data class Piece(val type: PieceType, val color: PieceColor)