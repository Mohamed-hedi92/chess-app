package com.example.chess_backend.service;
import com.example.chess_backend.domain.GameStatus;
import com.example.chess_backend.domain.Board;
import com.example.chess_backend.domain.ChessBot;
import com.example.chess_backend.domain.Color;
import com.example.chess_backend.domain.MoveResult;
import com.example.chess_backend.domain.PieceType;
import com.example.chess_backend.domain.Position;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChessService {

    private Board board = new Board();
    private ChessBot bot = new ChessBot(2); // Standard: Mittel
    private boolean botEnabled = true;
    private Color botColor = Color.BLACK;

    public Board getBoard() {
        return board;
    }

    public MoveResult move(Position from, Position to) {
        return board.move(from, to);
    }

    public MoveResult move(Position from, Position to, PieceType promotionType) {
        return board.move(from, to, promotionType);
    }

    public List<Position> getPossibleMoves(Position from) {
        return board.getPossibleMoves(from);
    }

    /**
     * Berechnet den Bot-Zug und führt ihn aus.
     */
    public BotMoveResult doBotMove() {
        if (!botEnabled) return new BotMoveResult(false, null, null, board.getGameStatus());

        Color currentTurn = board.getCurrentTurn();
        if (currentTurn != botColor) {
            return new BotMoveResult(false, null, null, board.getGameStatus());
        }

        // Spiel vorbei?
        if (board.getGameStatus() == GameStatus.WHITE_WINS ||
                board.getGameStatus() == GameStatus.BLACK_WINS ||
                board.getGameStatus() == GameStatus.STALEMATE) {
            return new BotMoveResult(false, null, null, board.getGameStatus());
        }

        ChessBot.BotMove bestMove = bot.findBestMove(board, botColor);
        if (bestMove == null) {
            return new BotMoveResult(false, null, null, board.getGameStatus());
        }

        MoveResult result = board.move(bestMove.from(), bestMove.to());
        return new BotMoveResult(result.success(), bestMove.from(), bestMove.to(), result.gameStatus());
    }

    public void setBotDifficulty(int difficulty) {
        this.bot = new ChessBot(difficulty);
    }

    public void setBotEnabled(boolean enabled) {
        this.botEnabled = enabled;
    }

    public boolean isBotEnabled() {
        return botEnabled;
    }

    public Color getBotColor() {
        return botColor;
    }

    public void resetGame() {
        this.board = new Board();
    }

    public record BotMoveResult(boolean success, Position from, Position to, GameStatus gameStatus) {}
}