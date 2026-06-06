package com.example.chess_backend.domain;

/**
 * Ergebnis eines Zugs, das dem Frontend alle nötigen Informationen liefert.
 */
public record MoveResult(
        boolean success,        // War der Zug erfolgreich?
        GameStatus gameStatus,  // Aktueller Spielstatus nach dem Zug
        MoveType moveType       // Art des Zugs (normal, Rochade, En Passant, Promotion)
) {}
