package com.example.chess_backend.domain;


public enum GameStatus {
    ACTIVE,        // Spiel läuft normal
    CHECK,         // Aktueller Spieler steht im Schach
    WHITE_WINS,    // Weiß gewinnt (Schachmatt)
    BLACK_WINS,    // Schwarz gewinnt (Schachmatt)
    STALEMATE      // Patt - Unentschieden
}
