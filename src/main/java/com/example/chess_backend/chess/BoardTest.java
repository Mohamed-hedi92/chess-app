package com.example.chess_backend.chess;

import com.example.chess_backend.domain.Board;
import com.example.chess_backend.domain.MoveResult;
import com.example.chess_backend.domain.MoveType;
import com.example.chess_backend.domain.Position;

public class BoardTest {

    public static void main(String[] args) {
        Board board = new Board();

        // --- Test 1: Pawn vorwärts ---
        Position pFrom = new Position(6, 0); // weiße Pawn a2
        Position pTo = new Position(4, 0);   // a4
        if (board.move(pFrom, pTo).success()) {
            System.out.println("Pawn Move OK: " + pFrom + " -> " + pTo);
        }

        // --- Test 2: Knight Sprung ---
        Position nFrom = new Position(7, 1); // weiße Knight b1
        Position nTo = new Position(5, 2);   // c3
        if (board.move(nFrom, nTo).success()) {
            System.out.println("Knight Move OK: " + nFrom + " -> " + nTo);
        }

        // --- Test 3: Queen diagonal ---
        Position qFrom = new Position(7, 3); // weiße Queen d1
        Position qTo = new Position(3, 7);   // h5
        if (!board.move(qFrom, qTo).success()) {
            System.out.println("Queen Move blocked (OK)");
        }

        // --- Test 4: Castling kurz ---
        // Lege freie Felder zwischen King und Rook
        board.move(new Position(6, 5), new Position(5, 5)); // Pawn f2 -> f3
        board.move(new Position(6, 6), new Position(4, 6)); // Pawn g2 -> g4
        Position kingFrom = new Position(7, 4); // e1
        Position kingTo = new Position(7, 6);   // g1 (kurz rochade)
        MoveResult castlingResult = board.move(kingFrom, kingTo);
        if (castlingResult.success()) {
            System.out.println("Castling kurz OK (MoveType: " + castlingResult.moveType() + ")");
        }

        // --- Test 5: Ungültiger Move ---
        Position invalidFrom = new Position(7, 0); // Rook a1
        Position invalidTo = new Position(5, 2);   // c3
        if (!board.move(invalidFrom, invalidTo).success()) {
            System.out.println("Ungültiger Move erkannt (OK)");
        }

        // --- Test 6: Pawn schlagen diagonal ---
        Position pawnFrom = new Position(4, 6); // g4
        Position pawnTo = new Position(3, 5);   // f5, dort müsste schwarzer Pawn sein
        if (!board.move(pawnFrom, pawnTo).success()) {
            System.out.println("Pawn Schlag diagonal (noch leer) -> Move abgelehnt (OK)");
        }
    }
}
