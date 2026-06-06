package com.example.chess_backend.dto;

import com.example.chess_backend.domain.Piece;

public class BotMoveResponse {

    private boolean success;
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private String gameStatus;
    private String message;
    private Piece[][] board;

    public BotMoveResponse() {}

    public BotMoveResponse(boolean success, int fromRow, int fromCol, int toRow, int toCol,
                           String gameStatus, String message, Piece[][] board) {
        this.success = success;
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.gameStatus = gameStatus;
        this.message = message;
        this.board = board;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public int getFromRow() { return fromRow; }
    public void setFromRow(int fromRow) { this.fromRow = fromRow; }

    public int getFromCol() { return fromCol; }
    public void setFromCol(int fromCol) { this.fromCol = fromCol; }

    public int getToRow() { return toRow; }
    public void setToRow(int toRow) { this.toRow = toRow; }

    public int getToCol() { return toCol; }
    public void setToCol(int toCol) { this.toCol = toCol; }

    public String getGameStatus() { return gameStatus; }
    public void setGameStatus(String gameStatus) { this.gameStatus = gameStatus; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Piece[][] getBoard() { return board; }
    public void setBoard(Piece[][] board) { this.board = board; }
}
