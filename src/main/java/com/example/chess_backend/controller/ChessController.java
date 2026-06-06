package com.example.chess_backend.controller;

import com.example.chess_backend.dto.BotSettingsRequest;
import com.example.chess_backend.domain.Board;
import com.example.chess_backend.domain.GameStatus;
import com.example.chess_backend.domain.MoveResult;
import com.example.chess_backend.domain.MoveType;
import com.example.chess_backend.domain.PieceType;
import com.example.chess_backend.domain.Position;
import com.example.chess_backend.dto.MoveRequest;
import com.example.chess_backend.dto.MoveResponse;
import com.example.chess_backend.dto.BotMoveResponse;
import com.example.chess_backend.service.ChessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chess")
public class ChessController {

    private final ChessService service;

    public ChessController(ChessService service) {
        this.service = service;
    }

    // Aktuelles Board abfragen
    @GetMapping("/board")
    public Board getBoard() {
        return service.getBoard();
    }

    // Zug ausführen
    @PostMapping("/move")
    public MoveResponse move(@RequestBody MoveRequest request) {
        Position from = new Position(request.getFromRow(), request.getFromCol());
        Position to = new Position(request.getToRow(), request.getToCol());

        MoveResult result;
        if (request.getPromotionType() != null) {
            PieceType promotionType = PieceType.valueOf(request.getPromotionType());
            result = service.move(from, to, promotionType);
        } else {
            result = service.move(from, to);
        }

        String message = buildMessage(result);

        return new MoveResponse(
                result.success(),
                message,
                result.gameStatus().name(),
                result.moveType() != null ? result.moveType().name() : null,
                service.getBoard().getBoardArray()
        );
    }

    // Bot-Zug ausführen
    @PostMapping("/bot-move")
    public BotMoveResponse botMove() {
        ChessService.BotMoveResult result = service.doBotMove();

        String message = "";
        if (result.success()) {
            message = "Bot-Zug ausgeführt";
            if (result.gameStatus() == GameStatus.CHECK) message += " - Schach!";
            if (result.gameStatus() == GameStatus.WHITE_WINS) message = "Schachmatt! Weiß gewinnt!";
            if (result.gameStatus() == GameStatus.BLACK_WINS) message = "Schachmatt! Schwarz gewinnt!";
            if (result.gameStatus() == GameStatus.STALEMATE) message = "Patt! Unentschieden!";
        } else {
            message = "Kein Bot-Zug möglich";
        }

        return new BotMoveResponse(
                result.success(),
                result.from() != null ? result.from().row() : -1,
                result.from() != null ? result.from().col() : -1,
                result.to() != null ? result.to().row() : -1,
                result.to() != null ? result.to().col() : -1,
                result.gameStatus().name(),
                message,
                service.getBoard().getBoardArray()
        );
    }

    // Mögliche Züge abfragen
    @GetMapping("/moves")
    public List<Position> getPossibleMoves(
            @RequestParam int row,
            @RequestParam int col
    ) {
        return service.getPossibleMoves(new Position(row, col));
    }

    // Bot-Einstellungen
    @PostMapping("/bot/settings")
    public String updateBotSettings(@RequestBody BotSettingsRequest request) {
        if (request.getDifficulty() != null) {
            service.setBotDifficulty(request.getDifficulty());
        }
        if (request.getEnabled() != null) {
            service.setBotEnabled(request.getEnabled());
        }
        return "OK";
    }

    // Neues Spiel
    @PostMapping("/reset")
    public String resetGame() {
        service.resetGame();
        return "OK";
    }

    private String buildMessage(MoveResult result) {
        if (!result.success()) return "Ungültiger Move";

        String msg = switch (result.gameStatus()) {
            case WHITE_WINS -> "Schachmatt! Weiß gewinnt!";
            case BLACK_WINS -> "Schachmatt! Schwarz gewinnt!";
            case STALEMATE -> "Patt! Unentschieden!";
            case CHECK -> "Schach!";
            case ACTIVE -> "Move OK";
        };

        if (result.moveType() == MoveType.CASTLING_KINGSIDE) {
            msg += " (Kurz-Rochade)";
        } else if (result.moveType() == MoveType.CASTLING_QUEENSIDE) {
            msg += " (Lang-Rochade)";
        } else if (result.moveType() == MoveType.EN_PASSANT) {
            msg += " (En Passant)";
        } else if (result.moveType() == MoveType.PROMOTION) {
            msg += " (Umwandlung)";
        }

        return msg;
    }
}
