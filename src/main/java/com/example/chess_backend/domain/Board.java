package com.example.chess_backend.domain;

import java.util.ArrayList;
import java.util.List;

public class Board {

    private final Piece[][] board = new Piece[8][8];

    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private final boolean[] whiteRooksMoved = new boolean[2];
    private final boolean[] blackRooksMoved = new boolean[2];

    // En Passant: speichert das Feld, auf dem ein En-Passant-Schlag möglich ist
    // null = kein En Passant möglich
    private Position enPassantTarget = null;

    // Spielstatus
    private Color currentTurn = Color.WHITE;
    private GameStatus gameStatus = GameStatus.ACTIVE;

    public Board() {
        init();
    }

    private Board(boolean skipInit) {
        if (!skipInit) {
            init();
        }
    }

    private void init() {
        board[0] = new Piece[]{
                new Piece(PieceType.ROOK, Color.BLACK),
                new Piece(PieceType.KNIGHT, Color.BLACK),
                new Piece(PieceType.BISHOP, Color.BLACK),
                new Piece(PieceType.QUEEN, Color.BLACK),
                new Piece(PieceType.KING, Color.BLACK),
                new Piece(PieceType.BISHOP, Color.BLACK),
                new Piece(PieceType.KNIGHT, Color.BLACK),
                new Piece(PieceType.ROOK, Color.BLACK)
        };
        for (int i = 0; i < 8; i++) board[1][i] = new Piece(PieceType.PAWN, Color.BLACK);

        board[7] = new Piece[]{
                new Piece(PieceType.ROOK, Color.WHITE),
                new Piece(PieceType.KNIGHT, Color.WHITE),
                new Piece(PieceType.BISHOP, Color.WHITE),
                new Piece(PieceType.QUEEN, Color.WHITE),
                new Piece(PieceType.KING, Color.WHITE),
                new Piece(PieceType.BISHOP, Color.WHITE),
                new Piece(PieceType.KNIGHT, Color.WHITE),
                new Piece(PieceType.ROOK, Color.WHITE)
        };
        for (int i = 0; i < 8; i++) board[6][i] = new Piece(PieceType.PAWN, Color.WHITE);
    }

    // ========== Öffentliche API ==========

    public Piece getPiece(Position p) {
        return isInside(p) ? board[p.row()][p.col()] : null;
    }

    public Piece[][] getBoardArray() {
        return board;
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    /**
     * Führt einen normalen Zug aus (ohne Promotion-Angabe).
     * Bei Bauern-Umwandlung wird automatisch zur Dame umgewandelt.
     */
    public MoveResult move(Position from, Position to) {
        return move(from, to, PieceType.QUEEN);
    }

    /**
     * Führt einen Zug aus mit Promotion-Angabe.
     * Gibt ein MoveResult zurück mit Info über den Zug und den Spielstatus.
     */
    public MoveResult move(Position from, Position to, PieceType promotionType) {
        Piece piece = getPiece(from);
        if (piece == null) return new MoveResult(false, gameStatus, null);

        // Prüfe ob der richtige Spieler am Zug ist
        if (piece.getColor() != currentTurn) return new MoveResult(false, gameStatus, null);

        if (!isLegalMove(from, to)) return new MoveResult(false, gameStatus, null);

        // Prüfe ob es sich um eine Rochade handelt
        boolean isCastling = isCastlingMove(from, to);

        // Prüfe ob es sich um En Passant handelt
        boolean isEnPassant = isEnPassantCapture(from, to);

        // Führe den Zug aus
        applyMove(from, to, isCastling, isEnPassant, promotionType);

        // Wechsle den Spieler
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;

        // En Passant Target zurücksetzen (nur gültig für den nächsten Zug)
        enPassantTarget = null;

        // Setze neues En Passant Target wenn Bauer Doppelschritt gemacht hat
        if (piece.getType() == PieceType.PAWN && Math.abs(to.row() - from.row()) == 2) {
            int targetRow = (from.row() + to.row()) / 2;
            enPassantTarget = new Position(targetRow, from.col());
        }

        // Prüfe Spielstatus nach dem Zug
        updateGameStatus();

        // Bestimme den speziellen Zug-Typ für das Frontend
        MoveType moveType = MoveType.NORMAL;
        if (isCastling) {
            moveType = (to.col() > from.col()) ? MoveType.CASTLING_KINGSIDE : MoveType.CASTLING_QUEENSIDE;
        } else if (isEnPassant) {
            moveType = MoveType.EN_PASSANT;
        } else if (piece.getType() == PieceType.PAWN &&
                (to.row() == 0 || to.row() == 7)) {
            moveType = MoveType.PROMOTION;
        }

        return new MoveResult(true, gameStatus, moveType);
    }

    public List<Position> getPossibleMoves(Position from) {
        List<Position> moves = new ArrayList<>();
        Piece piece = getPiece(from);
        if (piece == null) return moves;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position to = new Position(r, c);
                if (isLegalMove(from, to)) {
                    moves.add(to);
                }
            }
        }
        return moves;
    }

    public boolean isInCheck(Color color) {
        Position kingPos = findKing(color);
        if (kingPos == null) return false;

        Color enemy = color == Color.WHITE ? Color.BLACK : Color.WHITE;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.getColor() == enemy) {
                    if (canAttack(new Position(r, c), kingPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Prüft ob die gegebene Farbe im Schachmatt steht.
     */
    public boolean isCheckmate(Color color) {
        if (!isInCheck(color)) return false;
        return !hasAnyLegalMove(color);
    }

    /**
     * Prüft ob die gegebene Farbe im Patt steht (kein legaler Zug, aber nicht im Schach).
     */
    public boolean isStalemate(Color color) {
        if (isInCheck(color)) return false;
        return !hasAnyLegalMove(color);
    }

    // ========== Zug-Validierung ==========

    private boolean isLegalMove(Position from, Position to) {
        Piece piece = getPiece(from);
        if (piece == null) return false;

        if (from.row() == to.row() && from.col() == to.col()) return false;

        Piece target = getPiece(to);
        if (target != null && target.getColor() == piece.getColor()) return false;

        int dr = to.row() - from.row();
        int dc = to.col() - from.col();

        boolean pseudoLegal = switch (piece.getType()) {
            case PAWN -> isValidPawnMove(piece, from, to);
            case ROOK -> (dr == 0 || dc == 0) && clearPath(from, to, dr, dc);
            case BISHOP -> Math.abs(dr) == Math.abs(dc) && clearPath(from, to, dr, dc);
            case QUEEN -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) && clearPath(from, to, dr, dc);
            case KNIGHT -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case KING -> isValidKingMove(piece, from, to);
        };

        if (!pseudoLegal) return false;

        return !wouldLeaveKingInCheck(from, to);
    }

    private boolean isValidPawnMove(Piece pawn, Position from, Position to) {
        int dir = pawn.getColor() == Color.WHITE ? -1 : 1;
        int dr = to.row() - from.row();
        int dc = Math.abs(to.col() - from.col());

        // Einfacher Schritt nach vorne
        if (from.col() == to.col() && dr == dir && getPiece(to) == null) {
            return true;
        }

        // Doppelschritt von der Startposition
        if ((pawn.getColor() == Color.WHITE && from.row() == 6 || pawn.getColor() == Color.BLACK && from.row() == 1)
                && from.col() == to.col()
                && dr == 2 * dir
                && getPiece(to) == null
                && getPiece(new Position(from.row() + dir, from.col())) == null) {
            return true;
        }

        // Normaler diagonaler Schlag
        if (dc == 1 && dr == dir && getPiece(to) != null) {
            return true;
        }

        // En Passant Schlag
        if (dc == 1 && dr == dir && getPiece(to) == null && enPassantTarget != null
                && to.row() == enPassantTarget.row() && to.col() == enPassantTarget.col()) {
            return true;
        }

        return false;
    }

    /**
     * König-Zug inklusive Rochade-Prüfung.
     */
    private boolean isValidKingMove(Piece king, Position from, Position to) {
        int dr = Math.abs(to.row() - from.row());
        int dc = Math.abs(to.col() - from.col());

        // Normaler König-Zug (1 Feld in jede Richtung)
        if (dr <= 1 && dc <= 1) {
            return true;
        }

        // Rochade: König bewegt sich 2 Felder zur Seite
        if (dr == 0 && Math.abs(to.col() - from.col()) == 2) {
            return isValidCastling(king.getColor(), from, to);
        }

        return false;
    }

    // ========== Rochade (Castling) ==========

    /**
     * Prüft ob die Rochade gültig ist.
     * Bedingungen:
     * 1. König wurde noch nicht bewegt
     * 2. Beteiligter Turm wurde noch nicht bewegt
     * 3. Keine Figuren zwischen König und Turm
     * 4. König steht nicht im Schach
     * 5. König zieht nicht durch ein bedrohtes Feld
     * 6. König steht nach dem Zug nicht im Schach
     */
    private boolean isValidCastling(Color color, Position from, Position to) {
        // König muss auf seiner Startposition stehen
        int kingRow = (color == Color.WHITE) ? 7 : 0;
        if (from.row() != kingRow || from.col() != 4) return false;

        // König darf nicht im Schach stehen
        if (isInCheck(color)) return false;

        boolean kingside = to.col() > from.col(); // Königsseite (kurz) oder Damenseite (lang)
        int rookCol = kingside ? 7 : 0;

        // Prüfe ob König bewegt wurde
        if (color == Color.WHITE && whiteKingMoved) return false;
        if (color == Color.BLACK && blackKingMoved) return false;

        // Prüfe ob der Turm bewegt wurde
        if (color == Color.WHITE) {
            int rookIndex = kingside ? 1 : 0;
            if (whiteRooksMoved[rookIndex]) return false;
        } else {
            int rookIndex = kingside ? 1 : 0;
            if (blackRooksMoved[rookIndex]) return false;
        }

        // Prüfe ob der Turm tatsächlich dort steht
        Position rookPos = new Position(kingRow, rookCol);
        Piece rook = getPiece(rookPos);
        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColor() != color) return false;

        // Prüfe ob der Weg zwischen König und Turm frei ist
        int step = kingside ? 1 : -1;
        for (int c = from.col() + step; c != rookCol; c += step) {
            if (board[kingRow][c] != null) return false;
        }

        // Prüfe ob der König durch ein bedrohtes Feld zieht
        // Der König zieht über from.col() + step und to.col()
        for (int c = from.col() + step; ; c += step) {
            Position passThrough = new Position(kingRow, c);
            if (isSquareAttacked(passThrough, color)) return false;
            if (c == to.col()) break;
        }

        return true;
    }

    /**
     * Prüft ob ein Feld von der gegnerischen Farbe angegriffen wird.
     */
    private boolean isSquareAttacked(Position pos, Color defenderColor) {
        Color attackerColor = (defenderColor == Color.WHITE) ? Color.BLACK : Color.WHITE;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.getColor() == attackerColor) {
                    if (canAttack(new Position(r, c), pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Prüft ob es sich um einen Rochade-Zug handelt.
     */
    public boolean isCastlingMove(Position from, Position to) {
        Piece piece = getPiece(from);
        if (piece == null || piece.getType() != PieceType.KING) return false;
        return Math.abs(to.col() - from.col()) == 2 && from.row() == to.row();
    }

    // ========== En Passant ==========

    /**
     * Prüft ob es sich um einen En-Passant-Schlag handelt.
     */
    public boolean isEnPassantCapture(Position from, Position to) {
        Piece piece = getPiece(from);
        if (piece == null || piece.getType() != PieceType.PAWN) return false;

        int dc = Math.abs(to.col() - from.col());
        int dr = to.row() - from.row();
        int dir = piece.getColor() == Color.WHITE ? -1 : 1;

        // Diagonaler Bauer-Zug auf ein leeres Feld = En Passant
        return dc == 1 && dr == dir && getPiece(to) == null && enPassantTarget != null
                && to.row() == enPassantTarget.row() && to.col() == enPassantTarget.col();
    }

    // ========== Zug-Ausführung ==========

    public void applyMove(Position from, Position to, boolean isCastling, boolean isEnPassant, PieceType promotionType) {
        Piece piece = board[from.row()][from.col()];

        // En Passant: Geschlagenen Bauer entfernen
        if (isEnPassant) {
            int capturedPawnRow = from.row(); // Der geschlagene Bauer steht auf der gleichen Reihe wie der ziehende Bauer
            board[capturedPawnRow][to.col()] = null;
        }

        // Rochade: Turm ebenfalls bewegen
        if (isCastling) {
            int kingRow = from.row();
            boolean kingside = to.col() > from.col();

            int rookFromCol = kingside ? 7 : 0;
            int rookToCol = kingside ? 5 : 3; // Turm-Endposition bei Rochade

            board[kingRow][rookToCol] = board[kingRow][rookFromCol];
            board[kingRow][rookFromCol] = null;

            // Turm-Bewegungsmarkierung setzen
            if (piece.getColor() == Color.WHITE) {
                whiteRooksMoved[kingside ? 1 : 0] = true;
            } else {
                blackRooksMoved[kingside ? 1 : 0] = true;
            }
        }

        // Figur bewegen
        board[to.row()][to.col()] = piece;
        board[from.row()][from.col()] = null;

        if (piece == null) return;

        // Bauern-Umwandlung (Promotion)
        if (piece.getType() == PieceType.PAWN) {
            if ((piece.getColor() == Color.WHITE && to.row() == 0) ||
                    (piece.getColor() == Color.BLACK && to.row() == 7)) {
                board[to.row()][to.col()] = new Piece(promotionType, piece.getColor());
            }
        }

        // König-Bewegung tracken
        if (piece.getType() == PieceType.KING) {
            if (piece.getColor() == Color.WHITE) whiteKingMoved = true;
            else blackKingMoved = true;
        }

        // Turm-Bewegung tracken (bei normalem Turm-Zug, nicht bei Rochade - das wird oben gemacht)
        if (piece.getType() == PieceType.ROOK && !isCastling) {
            if (piece.getColor() == Color.WHITE) {
                if (from.row() == 7 && from.col() == 0) whiteRooksMoved[0] = true;
                if (from.row() == 7 && from.col() == 7) whiteRooksMoved[1] = true;
            } else {
                if (from.row() == 0 && from.col() == 0) blackRooksMoved[0] = true;
                if (from.row() == 0 && from.col() == 7) blackRooksMoved[1] = true;
            }
        }
    }

    // ========== Spielstatus ==========

    /**
     * Aktualisiert den Spielstatus nach einem Zug.
     */
    private void updateGameStatus() {
        if (isCheckmate(currentTurn)) {
            gameStatus = (currentTurn == Color.WHITE) ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS;
        } else if (isStalemate(currentTurn)) {
            gameStatus = GameStatus.STALEMATE;
        } else if (isInCheck(currentTurn)) {
            gameStatus = GameStatus.CHECK;
        } else {
            gameStatus = GameStatus.ACTIVE;
        }
    }

    /**
     * Prüft ob die gegebene Farbe mindestens einen legalen Zug hat.
     */
    private boolean hasAnyLegalMove(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.getColor() == color) {
                    if (!getPossibleMoves(new Position(r, c)).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ========== Hilfsmethoden ==========

    private boolean canAttack(Position from, Position to) {
        Piece piece = getPiece(from);
        if (piece == null) return false;

        int dr = to.row() - from.row();
        int dc = to.col() - from.col();

        return switch (piece.getType()) {
            case PAWN -> {
                int dir = piece.getColor() == Color.WHITE ? -1 : 1;
                yield dr == dir && Math.abs(dc) == 1;
            }
            case ROOK -> (dr == 0 || dc == 0) && clearPath(from, to, dr, dc);
            case BISHOP -> Math.abs(dr) == Math.abs(dc) && clearPath(from, to, dr, dc);
            case QUEEN -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) && clearPath(from, to, dr, dc);
            case KNIGHT -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case KING -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1;
        };
    }

    private Position findKing(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
                    return new Position(r, c);
                }
            }
        }
        return null;
    }

    private boolean wouldLeaveKingInCheck(Position from, Position to) {
        Piece movingPiece = getPiece(from);
        if (movingPiece == null) return false;

        Board copy = copy();
        // Im Copy müssen wir den Zug simulieren, inklusive En Passant und Rochade
        boolean isCastling = copy.isCastlingMove(from, to);
        boolean isEnPassant = copy.isEnPassantCapture(from, to);
        copy.applyMove(from, to, isCastling, isEnPassant, PieceType.QUEEN);
        return copy.isInCheck(movingPiece.getColor());
    }

    private boolean clearPath(Position from, Position to, int dr, int dc) {
        int rStep = Integer.compare(dr, 0);
        int cStep = Integer.compare(dc, 0);

        int r = from.row() + rStep;
        int c = from.col() + cStep;

        while (r != to.row() || c != to.col()) {
            if (board[r][c] != null) return false;
            r += rStep;
            c += cStep;
        }
        return true;
    }

    public Board copy() {
        Board copy = new Board(true);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null) {
                    copy.board[r][c] = new Piece(p.getType(), p.getColor());
                }
            }
        }

        copy.whiteKingMoved = whiteKingMoved;
        copy.blackKingMoved = blackKingMoved;
        copy.whiteRooksMoved[0] = whiteRooksMoved[0];
        copy.whiteRooksMoved[1] = whiteRooksMoved[1];
        copy.blackRooksMoved[0] = blackRooksMoved[0];
        copy.blackRooksMoved[1] = blackRooksMoved[1];
        copy.enPassantTarget = enPassantTarget;
        copy.currentTurn = currentTurn;
        copy.gameStatus = gameStatus;

        return copy;
    }

    /**
     * Wechselt den aktuellen Spieler (für Bot-Simulation).
     */
    public void switchTurn() {
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }

    /**
     * Setzt das En-Passant-Ziel (für Bot-Simulation).
     */
    public void setEnPassantTarget(Position target) {
        this.enPassantTarget = target;
    }

    private boolean isInside(Position p) {
        return p.row() >= 0 && p.row() < 8 && p.col() >= 0 && p.col() < 8;
    }
}
