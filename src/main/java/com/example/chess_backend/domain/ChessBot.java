package com.example.chess_backend.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Schach-Bot mit Minimax-Algorithmus und Alpha-Beta-Pruning.
 *
 * Schwierigkeitsgrade:
 *   1 = Leicht  (Tiefe 2)
 *   2 = Mittel  (Tiefe 3)
 *   3 = Schwer  (Tiefe 4)
 *
 * Verbesserungen:
 *   - Knotenlimit verhindert Endlos-Suche
 *   - Quiescence Search sucht alle Züge wenn im Schach
 *   - Iterative Deepening mit Zeitlimit
 */
public class ChessBot {

    private static final int INF = 1000000;

    // Maximale Knoten pro Suche (verhindert Hängen)
    private static final int MAX_NODES = 500000;

    // Zeitlimit in Millisekunden
    private static final long MAX_TIME_MS = 8000;

    // Figurenwerte in Centipawns
    private static final int[] PIECE_VALUES = new int[PieceType.values().length];
    static {
        PIECE_VALUES[PieceType.PAWN.ordinal()]   = 100;
        PIECE_VALUES[PieceType.KNIGHT.ordinal()] = 320;
        PIECE_VALUES[PieceType.BISHOP.ordinal()] = 330;
        PIECE_VALUES[PieceType.ROOK.ordinal()]   = 500;
        PIECE_VALUES[PieceType.QUEEN.ordinal()]  = 900;
        PIECE_VALUES[PieceType.KING.ordinal()]   = 20000;
    }

    // Positionstabellen (aus weißer Sicht, Zeile 0 = Rank 8)
    private static final int[][] PAWN_TABLE = {
            {  0,  0,  0,  0,  0,  0,  0,  0},
            { 50, 50, 50, 50, 50, 50, 50, 50},
            { 10, 10, 20, 30, 30, 20, 10, 10},
            {  5,  5, 10, 25, 25, 10,  5,  5},
            {  0,  0,  0, 20, 20,  0,  0,  0},
            {  5, -5,-10,  0,  0,-10, -5,  5},
            {  5, 10, 10,-20,-20, 10, 10,  5},
            {  0,  0,  0,  0,  0,  0,  0,  0}
    };

    private static final int[][] KNIGHT_TABLE = {
            {-50,-40,-30,-30,-30,-30,-40,-50},
            {-40,-20,  0,  0,  0,  0,-20,-40},
            {-30,  0, 10, 15, 15, 10,  0,-30},
            {-30,  5, 15, 20, 20, 15,  5,-30},
            {-30,  0, 15, 20, 20, 15,  0,-30},
            {-30,  5, 10, 15, 15, 10,  5,-30},
            {-40,-20,  0,  5,  5,  0,-20,-40},
            {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    private static final int[][] BISHOP_TABLE = {
            {-20,-10,-10,-10,-10,-10,-10,-20},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-10,  0, 10, 10, 10, 10,  0,-10},
            {-10,  5,  5, 10, 10,  5,  5,-10},
            {-10,  0,  5, 10, 10,  5,  0,-10},
            {-10, 10, 10, 10, 10, 10, 10,-10},
            {-10,  5,  0,  0,  0,  0,  5,-10},
            {-20,-10,-10,-10,-10,-10,-10,-20}
    };

    private static final int[][] ROOK_TABLE = {
            {  0,  0,  0,  0,  0,  0,  0,  0},
            {  5, 10, 10, 10, 10, 10, 10,  5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            {  0,  0,  0,  5,  5,  0,  0,  0}
    };

    private static final int[][] QUEEN_TABLE = {
            {-20,-10,-10, -5, -5,-10,-10,-20},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-10,  0,  5,  5,  5,  5,  0,-10},
            { -5,  0,  5,  5,  5,  5,  0, -5},
            {  0,  0,  5,  5,  5,  5,  0, -5},
            {-10,  5,  5,  5,  5,  5,  0,-10},
            {-10,  0,  5,  0,  0,  0,  0,-10},
            {-20,-10,-10, -5, -5,-10,-10,-20}
    };

    private static final int[][] KING_MIDDLEGAME_TABLE = {
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-20,-30,-30,-40,-40,-30,-30,-20},
            {-10,-20,-20,-20,-20,-20,-20,-10},
            { 20, 20,  0,  0,  0,  0, 20, 20},
            { 20, 30, 10,  0,  0, 10, 30, 20}
    };

    // Endspiel-König: aktiver König
    private static final int[][] KING_ENDGAME_TABLE = {
            {-50,-40,-30,-20,-20,-30,-40,-50},
            {-30,-20,-10,  0,  0,-10,-20,-30},
            {-30,-10, 20, 30, 30, 20,-10,-30},
            {-30,-10, 30, 40, 40, 30,-10,-30},
            {-30,-10, 30, 40, 40, 30,-10,-30},
            {-30,-10, 20, 30, 30, 20,-10,-30},
            {-30,-30,  0,  0,  0,  0,-30,-30},
            {-50,-30,-30,-30,-30,-30,-30,-50}
    };

    private final int maxDepth;

    // Such-Statistik (pro Suche)
    private int nodeCount;
    private long searchStartTime;
    private boolean searchAborted;

    public ChessBot(int difficulty) {
        this.maxDepth = switch (difficulty) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            default -> 3;
        };
    }

    /**
     * Prüft ob die Suche abgebrochen werden soll (Zeit- oder Knotenlimit).
     */
    private boolean shouldAbort() {
        if (searchAborted) return true;
        if (nodeCount > MAX_NODES) {
            searchAborted = true;
            return true;
        }
        if (System.currentTimeMillis() - searchStartTime > MAX_TIME_MS) {
            searchAborted = true;
            return true;
        }
        return false;
    }

    /**
     * Berechnet den besten Zug für die gegebene Farbe.
     * Verwendet Iterative Deepening für zeitbegrenzte Suche.
     */
    public BotMove findBestMove(Board board, Color color) {
        List<MoveCandidate> candidates = getAllLegalMoves(board, color);
        if (candidates.isEmpty()) return null;

        // Züge vorsortieren: Schlagzüge zuerst
        candidates.sort((a, b) -> {
            int scoreA = getMoveOrderScore(board, a);
            int scoreB = getMoveOrderScore(board, b);
            return Integer.compare(scoreB, scoreA);
        });

        // Such-Statistik zurücksetzen
        nodeCount = 0;
        searchStartTime = System.currentTimeMillis();
        searchAborted = false;

        int bestScore = -INF;
        BotMove bestMove = null;

        // Iterative Deepening: Suche von Tiefe 1 bis maxDepth
        for (int currentDepth = 1; currentDepth <= maxDepth; currentDepth++) {
            int depthBestScore = -INF;
            BotMove depthBestMove = null;

            for (MoveCandidate candidate : candidates) {
                if (shouldAbort()) break;

                Board copy = simulateMove(board, candidate);
                nodeCount++;

                Color opponent = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
                int score = -negamax(copy, opponent, currentDepth - 1, -INF, INF);

                if (score > depthBestScore) {
                    depthBestScore = score;
                    depthBestMove = new BotMove(candidate.from, candidate.to, score);
                }
            }

            // Nur aktualisieren wenn die Suche nicht abgebrochen wurde
            // (unvollständige Ergebnisse sind schlechter als die vorherige Tiefe)
            if (!searchAborted || depthBestMove == null) {
                if (depthBestMove != null) {
                    bestScore = depthBestScore;
                    bestMove = depthBestMove;
                }
            }

            if (searchAborted) break;
        }

        // Fallback: erster Zug wenn nichts gefunden
        if (bestMove == null && !candidates.isEmpty()) {
            MoveCandidate fallback = candidates.get(0);
            bestMove = new BotMove(fallback.from, fallback.to, 0);
        }

        return bestMove;
    }

    /**
     * Negamax mit Alpha-Beta-Pruning.
     * Gibt die Bewertung aus Sicht des currentColor zurück.
     */
    private int negamax(Board board, Color currentColor, int depth, int alpha, int beta) {
        if (shouldAbort()) return 0;

        // Spielende prüfen
        if (board.isCheckmate(currentColor)) {
            return -(INF + depth); // Matt: je früher desto schlechter
        }
        if (board.isStalemate(currentColor)) {
            return 0; // Patt
        }

        // Blattknoten
        if (depth == 0) {
            return quiescence(board, currentColor, alpha, beta, 6);
        }

        List<MoveCandidate> moves = getAllLegalMoves(board, currentColor);

        // Zugsortierung
        moves.sort((a, b) -> {
            int scoreA = getMoveOrderScore(board, a);
            int scoreB = getMoveOrderScore(board, b);
            return Integer.compare(scoreB, scoreA);
        });

        for (MoveCandidate move : moves) {
            if (shouldAbort()) return alpha;

            Board copy = simulateMove(board, move);
            nodeCount++;

            Color opponent = (currentColor == Color.WHITE) ? Color.BLACK : Color.WHITE;
            int score = -negamax(copy, opponent, depth - 1, -beta, -alpha);

            if (score >= beta) {
                return beta; // Beta-Cutoff
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        return alpha;
    }

    /**
     * Quiescence Search: Verhindert "Horizont-Effekt"
     *
     * WICHTIG: Wenn die Farbe im Schach steht, werden ALLE Züge gesucht,
     * nicht nur Schlagzüge (man MUSS dem Schach ausweichen!).
     */
    private int quiescence(Board board, Color currentColor, int alpha, int beta, int maxDepth) {
        if (shouldAbort()) return 0;

        // Prüfe Spielende
        if (board.isCheckmate(currentColor)) {
            return -(INF + maxDepth);
        }
        if (board.isStalemate(currentColor)) {
            return 0;
        }

        boolean inCheck = board.isInCheck(currentColor);

        int standPat = evaluate(board, currentColor);

        if (maxDepth == 0) return standPat;

        // Wenn NICHT im Schach: standPat als untere Grenze
        // (wir können den Zug ablehnen und standPat behalten)
        if (!inCheck) {
            if (standPat >= beta) return beta;
            if (standPat > alpha) alpha = standPat;
        }

        // Im Schach: ALLE Züge suchen (Schachabwehr ist Pflicht!)
        // Nicht im Schach: Nur Schlagzüge suchen
        List<MoveCandidate> moves;
        if (inCheck) {
            moves = getAllLegalMoves(board, currentColor);
        } else {
            moves = getCaptureMoves(board, currentColor);
        }

        moves.sort((a, b) -> {
            int scoreA = getMoveOrderScore(board, a);
            int scoreB = getMoveOrderScore(board, b);
            return Integer.compare(scoreB, scoreA);
        });

        for (MoveCandidate move : moves) {
            Board copy = simulateMove(board, move);
            nodeCount++;

            // Prüfe ob der Zug legal ist (König nicht im Schach)
            // Nur nötig bei Schlagzügen; bei getAllLegalMoves schon geprüft
            if (!inCheck && copy.isInCheck(currentColor)) continue;

            Color opponent = (currentColor == Color.WHITE) ? Color.BLACK : Color.WHITE;
            int score = -quiescence(copy, opponent, -beta, -alpha, maxDepth - 1);

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }

        // Im Schach ohne legale Züge = Schachmatt (wird oben geprüft)
        // Aber falls isCheckmate teuer ist und wir über moves iteriert haben:
        // alpha enthält den besten Score
        return alpha;
    }

    /**
     * Bewertungsfunktion: Positive Werte = gut für currentColor.
     */
    private int evaluate(Board board, Color currentColor) {
        int whiteScore = 0;
        int blackScore = 0;
        int whiteMaterial = 0;
        int blackMaterial = 0;

        Piece[][] pieces = board.getBoardArray();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = pieces[r][c];
                if (p == null) continue;

                int value = getPieceValue(p, r, c, pieces);

                if (p.getColor() == Color.WHITE) {
                    whiteScore += value;
                    whiteMaterial += PIECE_VALUES[p.getType().ordinal()];
                } else {
                    blackScore += value;
                    blackMaterial += PIECE_VALUES[p.getType().ordinal()];
                }
            }
        }

        // Doppelbauern-Strafe
        whiteScore -= countDoubledPawns(pieces, Color.WHITE) * 20;
        blackScore -= countDoubledPawns(pieces, Color.BLACK) * 20;

        // Isolierte Bauern-Strafe
        whiteScore -= countIsolatedPawns(pieces, Color.WHITE) * 15;
        blackScore -= countIsolatedPawns(pieces, Color.BLACK) * 15;

        // Königssicherheit: Bestraft König in der Mitte im Mittelspiel
        boolean isEndgame = (whiteMaterial + blackMaterial) < 3000;
        if (!isEndgame) {
            whiteScore += evaluateKingSafety(pieces, Color.WHITE);
            blackScore += evaluateKingSafety(pieces, Color.BLACK);
        }

        int score = whiteScore - blackScore;
        return (currentColor == Color.WHITE) ? score : -score;
    }

    private int getPieceValue(Piece piece, int row, int col, Piece[][] pieces) {
        int baseValue = PIECE_VALUES[piece.getType().ordinal()];

        // Positionswert (aus weißer Sicht)
        int posRow = piece.getColor() == Color.WHITE ? row : 7 - row;

        int posValue = switch (piece.getType()) {
            case PAWN -> PAWN_TABLE[posRow][col];
            case KNIGHT -> KNIGHT_TABLE[posRow][col];
            case BISHOP -> BISHOP_TABLE[posRow][col];
            case ROOK -> ROOK_TABLE[posRow][col];
            case QUEEN -> QUEEN_TABLE[posRow][col];
            case KING -> {
                // Endspiel-Tabelle wenn wenig Material
                int totalMaterial = 0;
                for (Piece[] r : pieces) {
                    for (Piece p : r) {
                        if (p != null && p.getType() != PieceType.KING) {
                            totalMaterial += PIECE_VALUES[p.getType().ordinal()];
                        }
                    }
                }
                yield totalMaterial < 2600 ? KING_ENDGAME_TABLE[posRow][col] : KING_MIDDLEGAME_TABLE[posRow][col];
            }
        };

        // Läufer-Paar Bonus
        if (piece.getType() == PieceType.BISHOP) {
            boolean hasPair = hasBishopPair(pieces, piece.getColor());
            if (hasPair) baseValue += 30;
        }

        // Freibauer Bonus
        if (piece.getType() == PieceType.PAWN) {
            if (isPassedPawn(pieces, row, col, piece.getColor())) {
                baseValue += 40 + (piece.getColor() == Color.WHITE ? (6 - row) * 10 : (row - 1) * 10);
            }
        }

        return baseValue + posValue;
    }

    private boolean hasBishopPair(Piece[][] pieces, Color color) {
        int count = 0;
        for (Piece[] row : pieces) {
            for (Piece p : row) {
                if (p != null && p.getType() == PieceType.BISHOP && p.getColor() == color) {
                    count++;
                }
            }
        }
        return count >= 2;
    }

    private boolean isPassedPawn(Piece[][] pieces, int row, int col, Color color) {
        int dir = color == Color.WHITE ? -1 : 1;
        int startRow = row + dir;

        for (int r = startRow; r >= 0 && r < 8; r += dir) {
            if (pieces[r][col] != null && pieces[r][col].getType() == PieceType.PAWN && pieces[r][col].getColor() != color) {
                return false;
            }
            if (col > 0 && pieces[r][col - 1] != null && pieces[r][col - 1].getType() == PieceType.PAWN && pieces[r][col - 1].getColor() != color) {
                return false;
            }
            if (col < 7 && pieces[r][col + 1] != null && pieces[r][col + 1].getType() == PieceType.PAWN && pieces[r][col + 1].getColor() != color) {
                return false;
            }
        }
        return true;
    }

    private int countDoubledPawns(Piece[][] pieces, Color color) {
        int doubled = 0;
        for (int c = 0; c < 8; c++) {
            int pawnsInFile = 0;
            for (int r = 0; r < 8; r++) {
                if (pieces[r][c] != null && pieces[r][c].getType() == PieceType.PAWN && pieces[r][c].getColor() == color) {
                    pawnsInFile++;
                }
            }
            if (pawnsInFile > 1) doubled += pawnsInFile - 1;
        }
        return doubled;
    }

    private int countIsolatedPawns(Piece[][] pieces, Color color) {
        int isolated = 0;
        for (int c = 0; c < 8; c++) {
            boolean hasFriendlyNeighbor = false;
            if (c > 0) {
                for (int r = 0; r < 8; r++) {
                    if (pieces[r][c - 1] != null && pieces[r][c - 1].getType() == PieceType.PAWN && pieces[r][c - 1].getColor() == color) {
                        hasFriendlyNeighbor = true;
                        break;
                    }
                }
            }
            if (c < 7) {
                for (int r = 0; r < 8; r++) {
                    if (pieces[r][c + 1] != null && pieces[r][c + 1].getType() == PieceType.PAWN && pieces[r][c + 1].getColor() == color) {
                        hasFriendlyNeighbor = true;
                        break;
                    }
                }
            }

            if (!hasFriendlyNeighbor) {
                for (int r = 0; r < 8; r++) {
                    if (pieces[r][c] != null && pieces[r][c].getType() == PieceType.PAWN && pieces[r][c].getColor() == color) {
                        isolated++;
                    }
                }
            }
        }
        return isolated;
    }

    private int evaluateKingSafety(Piece[][] pieces, Color color) {
        int kingRow = -1, kingCol = -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (pieces[r][c] != null && pieces[r][c].getType() == PieceType.KING && pieces[r][c].getColor() == color) {
                    kingRow = r;
                    kingCol = c;
                }
            }
        }
        if (kingRow < 0) return 0;

        int safety = 0;
        int expectedRow = color == Color.WHITE ? 7 : 0;
        if (kingRow == expectedRow && (kingCol <= 2 || kingCol >= 6)) {
            safety += 40;
        }

        int pawnDir = color == Color.WHITE ? -1 : 1;
        for (int dc = -1; dc <= 1; dc++) {
            int pr = kingRow + pawnDir;
            int pc = kingCol + dc;
            if (pr >= 0 && pr < 8 && pc >= 0 && pc < 8) {
                if (pieces[pr][pc] != null && pieces[pr][pc].getType() == PieceType.PAWN && pieces[pr][pc].getColor() == color) {
                    safety += 20;
                }
            }
        }

        return safety;
    }

    /**
     * Zugsortierung: MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
     */
    private int getMoveOrderScore(Board board, MoveCandidate move) {
        int score = 0;
        Piece target = board.getPiece(move.to);
        Piece attacker = board.getPiece(move.from);

        if (target != null) {
            score += PIECE_VALUES[target.getType().ordinal()] * 10
                    - PIECE_VALUES[attacker.getType().ordinal()];
        }

        // Bauernförderung
        if (attacker.getType() == PieceType.PAWN) {
            int promoRow = attacker.getColor() == Color.WHITE ? 0 : 7;
            if (move.to.row() == promoRow) {
                score += 800;
            }
        }

        // Zentrumskontrolle
        if ((move.to.row() == 3 || move.to.row() == 4) && (move.to.col() == 3 || move.to.col() == 4)) {
            score += 30;
        }

        return score;
    }

    /**
     * Simuliert einen Zug auf einer Kopie des Boards.
     */
    private Board simulateMove(Board board, MoveCandidate move) {
        Board copy = board.copy();

        Piece movingPiece = copy.getPiece(move.from);

        boolean isCastling = copy.isCastlingMove(move.from, move.to);
        boolean isEnPassant = copy.isEnPassantCapture(move.from, move.to);
        copy.applyMove(move.from, move.to, isCastling, isEnPassant, PieceType.QUEEN);

        copy.switchTurn();

        Position newEnPassantTarget = null;
        if (movingPiece != null && movingPiece.getType() == PieceType.PAWN
                && Math.abs(move.to.row() - move.from.row()) == 2) {
            int targetRow = (move.from.row() + move.to.row()) / 2;
            newEnPassantTarget = new Position(targetRow, move.from.col());
        }
        copy.setEnPassantTarget(newEnPassantTarget);

        return copy;
    }

    /**
     * Sammelt alle legalen Züge für eine Farbe.
     */
    private List<MoveCandidate> getAllLegalMoves(Board board, Color color) {
        List<MoveCandidate> moves = new ArrayList<>();
        Piece[][] pieces = board.getBoardArray();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = pieces[r][c];
                if (p != null && p.getColor() == color) {
                    Position from = new Position(r, c);
                    List<Position> possibleMoves = board.getPossibleMoves(from);
                    for (Position to : possibleMoves) {
                        moves.add(new MoveCandidate(from, to));
                    }
                }
            }
        }
        return moves;
    }

    /**
     * Sammelt nur Schlagzüge (für Quiescence Search).
     */
    private List<MoveCandidate> getCaptureMoves(Board board, Color color) {
        List<MoveCandidate> captures = new ArrayList<>();
        Piece[][] pieces = board.getBoardArray();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = pieces[r][c];
                if (p != null && p.getColor() == color) {
                    Position from = new Position(r, c);
                    List<Position> possibleMoves = board.getPossibleMoves(from);
                    for (Position to : possibleMoves) {
                        Piece target = board.getPiece(to);
                        if (target != null) {
                            captures.add(new MoveCandidate(from, to));
                        }
                    }
                }
            }
        }
        return captures;
    }

    private record MoveCandidate(Position from, Position to) {}

    public record BotMove(Position from, Position to, int score) {}
}