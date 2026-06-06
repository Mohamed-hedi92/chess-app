package com.example.chess_backend.domain;

/**
 * Gibt den Typ eines speziellen Zugs an, damit das Frontend
 * entsprechende Animationen oder UI-Elemente anzeigen kann.
 */
public enum MoveType {
    NORMAL,               // Normaler Zug
    CASTLING_KINGSIDE,    // Kurz-Rochade (Königsseite)
    CASTLING_QUEENSIDE,   // Lang-Rochade (Damenseite)
    EN_PASSANT,           // En-Passant-Schlag
    PROMOTION             // Bauern-Umwandlung
}
