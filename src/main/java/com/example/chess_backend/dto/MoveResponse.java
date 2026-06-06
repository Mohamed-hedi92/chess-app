package com.example.chess_backend.dto;

import com.example.chess_backend.domain.Piece;

public class MoveResponse {

    private boolean success;
    private String message;
    private String gameStatus;   // ACTIVE, CHECK, WHITE_WINS, BLACK_WINS, STALEMATE
    private String moveType;     // NORMAL, CASTLING_KINGSIDE, CASTLING_QUEENSIDE, EN_PASSANT, PROMOTION
    private Piece[][] board;

    public MoveResponse() {}

    public MoveResponse(boolean success, String message, String gameStatus, String moveType, Piece[][] board) {
        this.success = success;
        this.message = message;
        this.gameStatus = gameStatus;
        this.moveType = moveType;
        this.board = board;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getGameStatus() { return gameStatus; }
    public void setGameStatus(String gameStatus) { this.gameStatus = gameStatus; }

    public String getMoveType() { return moveType; }
    public void setMoveType(String moveType) { this.moveType = moveType; }

    public Piece[][] getBoard() { return board; }
    public void setBoard(Piece[][] board) { this.board = board; }
}
